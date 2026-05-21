// src/main/java/com/qaassist/prompt/PromptValidator.java
package com.qaassist.prompt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class PromptValidator {

  /**
   * Проверяет готовый промпт перед отправкой в LLM.
   * Возвращает null, если всё ок, иначе бросает PromptValidationException.
   */
  public void validate(PromptTemplate template, Map<String, Object> variables, String renderedPrompt) {
    Set<String> errors = new HashSet<>();

    // 1. Проверка обязательных переменных
    if (variables != null) {
      for (String req : template.requiredVariables()) {
        if (!variables.containsKey(req) || variables.get(req) == null) {
          errors.add("Missing required variable: " + req);
        }
      }
    }

    // 2. Оценка токенов (эвристика: ~4 символа = 1 токен для большинства моделей)
    int estimatedTokens = estimateTokens(renderedPrompt);
    if (estimatedTokens > template.maxTokens()) {
      errors.add("Estimated tokens (%d) exceed limit (%d)".formatted(estimatedTokens, template.maxTokens()));
    }

    // 3. Проверка на пустой промпт
    if (renderedPrompt.isBlank()) {
      errors.add("Rendered prompt is empty");
    }

    if (!errors.isEmpty()) {
      throw new PromptValidationException(template.id(), errors);
    }

    log.debug("Prompt {} passed validation (est. {} tokens)", template.id(), estimatedTokens);
  }

  /**
   * Быстрая оценка токенов.
   * В продакшене рекомендуется использовать jtokkit (https://github.com/knuddels/jtokkit)
   */
  private int estimateTokens(String text) {
    // Базовая эвристика + корректировка под код/JSON
    int charCount = text.length();
    double codeRatio = countOccurrences(text, "{") * 2.0 / charCount;
    return (int) (charCount / 4.0 * (1 + codeRatio));
  }

  private int countOccurrences(String text, char target) {
    int count = 0;
    for (char c : text.toCharArray()) if (c == target) count++;
    return count;
  }
}