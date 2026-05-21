package com.qaassist.domain.context;

import java.util.List;

/**
 * Результат среза контекста, готовый к подаче в промпт LLM.
 */
public record ContextSlice(
    String projectId,
    List<ContextEntryDto> entries,
    int totalChars,
    boolean truncated,
    String formattingTemplate // markdown-обёртка для LLM
) {
  public static ContextSlice of(String projectId, List<ContextEntryDto> entries, int maxChars) {
    int total = entries.stream().mapToInt(e -> e.content().length()).sum();
    boolean truncated = total > maxChars;

    // Если превышен лимит — обрезаем последние/наименее важные записи
    var finalEntries = truncated ? truncateEntries(entries, maxChars) : entries;
    int finalTotal = finalEntries.stream().mapToInt(e -> e.content().length()).sum();

    return new ContextSlice(
        projectId,
        finalEntries,
        finalTotal,
        truncated,
        formatForLlm(finalEntries)
    );
  }

  private static List<ContextEntryDto> truncateEntries(List<ContextEntryDto> entries, int maxChars) {
    var result = new java.util.ArrayList<ContextEntryDto>();
    int current = 0;
    for (var e : entries) {
      if (current + e.content().length() > maxChars) break;
      result.add(e);
      current += e.content().length();
    }
    return result;
  }

  private static String formatForLlm(List<ContextEntryDto> entries) {
    var sb = new StringBuilder("## 📚 Project Context\n\n");
    for (var e : entries) {
      sb.append("### 📁 [%s] %s\n").formatted(e.category().toUpperCase(), e.contextKey());
      sb.append(e.content()).append("\n\n---\n\n");
    }
    return sb.toString();
  }
}