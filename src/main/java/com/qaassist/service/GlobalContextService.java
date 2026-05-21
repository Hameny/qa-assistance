package com.qaassist.service;

import com.qaassist.domain.context.ProjectContextEntity;
import com.qaassist.domain.context.ProjectContextRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalContextService {

  private final ProjectContextRepository repository;
  private static final int MAX_CONTEXT_CHARS = 8000; // Лимит под контекстное окно LLM

  /**
   * Загружает полный контекст проекта. Кэшируется на уровне приложения.
   */
  @Cacheable(value = "global-context", key = "#projectId + '_full'")
  public Map<String, String> loadFullContext(String projectId) {
    log.debug("📥 Loading full context for {} (cache miss)", projectId);

    List<ProjectContextEntity> entities = repository.findByProjectIdAndIsActiveTrue(projectId);
    return entities.stream()
        .collect(Collectors.toMap(
            e -> e.getType().name().toLowerCase(),
            ProjectContextEntity::getContent,
            (existing, replacement) -> existing // При конфликте берём первый
        ));
  }

  /**
   * Генерирует оптимизированный срез контекста для отправки в LLM.
   * Фильтрует по типам, объединяет, применяет мягкий лимит символов.
   */
  public String getContextSlice(String projectId, List<ProjectContextEntity.ContextType> requestedTypes) {
    Map<String, String> fullContext = loadFullContext(projectId);

    StringBuilder slice = new StringBuilder();
    int totalChars = 0;

    for (ProjectContextEntity.ContextType type : requestedTypes) {
      String content = fullContext.get(type.name().toLowerCase());
      if (content == null || content.isBlank()) continue;

      int remaining = MAX_CONTEXT_CHARS - totalChars;
      if (remaining <= 100) break; // Оставляем место для системного промпта

      String section = content.length() > remaining
          ? content.substring(0, remaining) + "\n... [CONTEXT TRUNCATED]"
          : content;

      slice.append("### ").append(type.name()).append("\n")
          .append(section)
          .append("\n\n");

      totalChars += section.length() + 20; // + overhead for markdown
    }

    log.debug("📦 Context slice: {} chars, types: {} for project {}",
        slice.length(), requestedTypes.size(), projectId);
    return slice.toString();
  }

  /**
   * Сохраняет или обновляет контекст. Автоматически инвалидирует кэш.
   */
  @CacheEvict(value = "global-context", key = "#projectId + '_full'")
  @Transactional
  public void saveContext(String projectId, ProjectContextEntity.ContextType type,
      String content, String sourceRef) {
    // Деактивируем старую версию (soft delete для истории)
    repository.deactivateByProjectAndType(projectId, type);

    var entity = ProjectContextEntity.builder()
        .projectId(projectId)
        .type(type)
        .content(content)
        .sourceRef(sourceRef)
        .title(type.name() + " for " + projectId)
        .version("v1.0")
        .isActive(true)
        .createdAt(java.time.Instant.now())
        .updatedAt(java.time.Instant.now())
        .build();

    repository.save(entity);
    log.info("💾 Context updated: {} | {} | source: {}", projectId, type, sourceRef);
  }
}