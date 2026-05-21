// src/main/java/com/qaassist/domain/discrepancy/FixStrategy.java
package com.qaassist.domain.discrepancy;

import java.util.Map;

/**
 * Стратегия авто-исправления расхождения.
 */
public record FixStrategy(
    String name,
    String description,
    FixAction action,
    Map<String, String> parameters,
    double confidenceScore  // 0.0..1.0 — уверенность в корректности фикса
) {
  public boolean isSafeToAutoApply() {
    return confidenceScore >= 0.9;
  }

  public enum FixAction {
    UPDATE_TEST_STEP,           // Изменить шаг теста
    UPDATE_SELECTOR,            // Заменить локатор
    ADD_MISSING_ASSERTION,      // Добавить проверку
    SYNC_REQUIREMENT_TEXT,      // Обновить текст требования
    GENERATE_MISSING_TEST,      // Создать недостающий тест-кейс
    FLAG_FOR_REVIEW            // Только пометить, не менять
  }
}