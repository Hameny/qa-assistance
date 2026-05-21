// src/main/java/com/qaassist/prompt/PromptTemplate.java
package com.qaassist.prompt;

import java.util.Objects;
import java.util.Set;

/**
 * Неизменяемое представление промпта с метаданными.
 * Использует Java 17 record для потокобезопасности и лаконичности.
 */
public record PromptTemplate(
    String id,
    String version,           // e.g. "v1.0", "v1.1-experimental"
    String systemPrompt,      // Роль и инструкции для LLM
    String userTemplate,      // Шаблон с переменными: {{PROJECT}}, {{REQUIREMENTS}}
    Set<String> requiredVariables,
    int maxTokens,            // Жёсткий лимит для контекстного окна
    String description
) {
  public PromptTemplate {
    Objects.requireNonNull(id, "id cannot be null");
    Objects.requireNonNull(systemPrompt, "systemPrompt cannot be null");
    Objects.requireNonNull(userTemplate, "userTemplate cannot be null");
    if (requiredVariables == null) requiredVariables = Set.of();
    if (maxTokens <= 0) throw new IllegalArgumentException("maxTokens must be positive");
  }

  /** Возвращает true, если шаблон использует экспериментальную версию */
  public boolean isExperimental() {
    return version.toLowerCase().contains("experimental") ||
        version.toLowerCase().contains("beta");
  }
}