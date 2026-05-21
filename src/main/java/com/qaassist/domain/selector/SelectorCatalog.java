// src/main/java/com/qaassist/domain/selector/SelectorCatalog.java
package com.qaassist.domain.selector;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Каталог всех селекторов проекта с группировкой по страницам/компонентам.
 */
public record SelectorCatalog(
    UUID id,
    String projectId,
    Map<String, List<UiLocator>> byPage,      // "LoginPage" -> [locators]
    Map<String, List<UiLocator>> byComponent, // "SubmitButton" -> [locators]
    Instant lastUpdated,
    CatalogMetadata metadata
) {
  public record CatalogMetadata(
      int totalLocators,
      int stableCount,
      int needsReview,
      String playwrightVersion,
      List<String> warnings
  ) {}

  /** Возвращает лучший локатор для элемента по приоритету */
  public UiLocator findBest(String componentKey) {
    return byComponent.getOrDefault(componentKey, List.of()).stream()
        .max((a, b) -> Integer.compare(a.priorityScore(), b.priorityScore()))
        .orElse(null);
  }

  /** Группирует нестабильные селекторы для ревью */
  public List<UiLocator> getUnstable() {
    return byComponent.values().stream()
        .flatMap(List::stream)
        .filter(loc -> !loc.stability().isStable())
        .collect(Collectors.toList());
  }
}