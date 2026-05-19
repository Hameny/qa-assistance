// src/main/java/com/qaassist/domain/requirement/AcceptanceCriterion.java
package com.qaassist.domain.requirement;

import com.qaassist.domain.common.Priority;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/**
 * Критерий приёмки (Given-When-Then формат).
 */
public record AcceptanceCriterion(
    UUID id,
    @NotBlank String title,
    @NotBlank String description,
    Priority priority,
    String source,
    @NotBlank String given,    // контекст/предусловия
    @NotBlank String when,     // действие
    @NotBlank String then,     // ожидаемый результат
    boolean automated          // можно ли автоматизировать
) implements Requirement {

  // Конвертация в формат Gherkin для BDD-фреймворков
  public String toGherkin() {
    return """
            Scenario: %s
              Given %s
               When %s
               Then %s
            """.formatted(title, given, when, then);
  }
}