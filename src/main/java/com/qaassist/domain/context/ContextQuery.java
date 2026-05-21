package com.qaassist.domain.context;

import java.util.List;
import java.util.Optional;

/**
 * Запрос для среза контекста. Оптимизирован под ограничения LLM.
 */
public record ContextQuery(
    String projectId,
    List<String> categories,           // какие разделы брать
    Optional<List<String>> keywords,   // фильтрация по ключевым словам
    Optional<Integer> maxChars,        // лимит символов (приблизительно = токены * 4)
    boolean prioritizeRecent           // сортировать по updatedAt DESC
) {
  public static ContextQuery forCategories(String projectId, List<String> categories) {
    return new ContextQuery(projectId, categories, Optional.empty(), Optional.of(8000), true);
  }

  public static ContextQuery fullContext(String projectId, int maxChars) {
    return new ContextQuery(projectId, List.of(), Optional.empty(), Optional.of(maxChars), true);
  }
}