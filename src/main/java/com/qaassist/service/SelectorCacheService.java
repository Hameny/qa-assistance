// src/main/java/com/qaassist/service/SelectorCacheService.java
package com.qaassist.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaassist.domain.selector.SelectorCatalog;
import com.qaassist.domain.selector.UiLocator;
import com.qaassist.domain.context.ProjectContextEntity;
import com.qaassist.service.GlobalContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SelectorCacheService {

  private final GlobalContextService contextService;
  private final ObjectMapper objectMapper;

  private static final String CATALOG_KEY = "ui-selectors-catalog";

  /**
   * Загружает каталог селекторов из кэша или контекста.
   */
  @Cacheable(value = "selector-catalog", key = "#projectId")
  public SelectorCatalog loadCatalog(String projectId) {
    log.debug("📥 Loading selector catalog for {} (cache miss)", projectId);

    String json = contextService.getContextSlice(
        projectId,
        List.of(ProjectContextEntity.ContextType.GLOBAL_CONTEXT)
    );

    if (json.isBlank()) {
      return createEmptyCatalog(projectId);
    }

    try {
      // Парсим JSON-представление каталога
      Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<>() {});
      return deserializeCatalog(parsed, projectId);
    } catch (Exception e) {
      log.warn("⚠️ Failed to parse selector catalog: {}", e.getMessage());
      return createEmptyCatalog(projectId);
    }
  }

  /**
   * Сохраняет обновлённый каталог с инвалидацией кэша.
   */
  @CacheEvict(value = "selector-catalog", key = "#projectId")
  public void saveCatalog(String projectId, SelectorCatalog catalog) {
    try {
      String json = objectMapper.writeValueAsString(serializeCatalog(catalog));
      contextService.saveContext(projectId, ProjectContextEntity.ContextType.GLOBAL_CONTEXT,
          json, CATALOG_KEY);
      log.info("💾 Selector catalog saved for {}: {} locators",
          projectId, catalog.metadata().totalLocators());
    } catch (Exception e) {
      log.error("❌ Failed to save selector catalog: {}", e.getMessage());
    }
  }

  /**
   * Добавляет новые локаторы в существующий каталог (merge-логика).
   */
  public SelectorCatalog mergeAndSave(String projectId, List<UiLocator> newLocators) {
    SelectorCatalog existing = loadCatalog(projectId);

    // Группируем новые локаторы по компоненту (из description)
    Map<String, List<UiLocator>> byComponent = newLocators.stream()
        .collect(Collectors.groupingBy(loc ->
            loc.description().replace("Locator for: ", "")));

    // Merge: новые перезаписывают старые с тем же ключом
    Map<String, List<UiLocator>> mergedComponents = new HashMap<>(existing.byComponent());
    for (var entry : byComponent.entrySet()) {
      mergedComponents.merge(entry.getKey(), entry.getValue(),
          (oldList, newList) -> {
            // Объединяем, оставляя лучшие по приоритету
            var combined = new ArrayList<>(oldList);
            combined.addAll(newList);
            return combined.stream()
                .distinct()
                .sorted(Comparator.comparingInt(UiLocator::priorityScore).reversed())
                .limit(5)
                .toList();
          });
    }

    // Пересчитываем метаданные
    int total = mergedComponents.values().stream().mapToInt(List::size).sum();
    int stable = mergedComponents.values().stream()
        .flatMap(List::stream)
        .mapToInt(loc -> loc.stability().isStable() ? 1 : 0)
        .sum();

    var updated = new SelectorCatalog(
        existing.id(),
        projectId,
        existing.byPage(),
        mergedComponents,
        Instant.now(),
        new SelectorCatalog.CatalogMetadata(
            total, stable, total - stable, "1.42.0", List.of()
        )
    );

    saveCatalog(projectId, updated);
    return updated;
  }

  // Заглушки сериализации (в продакшене — полноценный DTO)
  private Map<String, Object> serializeCatalog(SelectorCatalog catalog) {
    return Map.of("projectId", catalog.projectId(), "byComponent", catalog.byComponent());
  }

  private SelectorCatalog deserializeCatalog(Map<String, Object> data, String projectId) {
    return new SelectorCatalog(
        UUID.randomUUID(), projectId, Map.of(), Map.of(), Instant.now(),
        new SelectorCatalog.CatalogMetadata(0, 0, 0, "1.42.0", List.of())
    );
  }

  private SelectorCatalog createEmptyCatalog(String projectId) {
    return new SelectorCatalog(
        UUID.randomUUID(), projectId, Map.of(), Map.of(), Instant.now(),
        new SelectorCatalog.CatalogMetadata(0, 0, 0, "1.42.0", List.of())
    );
  }
}