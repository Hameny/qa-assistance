// src/main/java/com/qaassist/agent/validator/DecompositionValidator.java
package com.qaassist.agent.validator;

import com.qaassist.domain.requirement.UserStory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class DecompositionValidator {

  /**
   * Бизнес-валидация после LLM-парсинга.
   * Проверяет логику, которую JSON Schema не покрывает.
   */
  public List<String> validate(UserStory story) {
    List<String> warnings = new ArrayList<>();

    if (story.acceptanceCriteria().isEmpty()) {
      warnings.add("UserStory has no acceptance criteria. Test generation will be incomplete.");
    }

    if (story.title().length() < 10) {
      warnings.add("Title is too short. Consider expanding for clarity.");
    }

    if (story.priority() == com.qaassist.domain.common.Priority.CRITICAL
        && story.acceptanceCriteria().size() < 2) {
      warnings.add("CRITICAL story should have at least 2 acceptance criteria for safety.");
    }

    // Проверка на дубликаты критериев
    long unique = story.acceptanceCriteria().stream()
        .map(AcceptanceCriterion::description)
        .distinct()
        .count();
    if (unique < story.acceptanceCriteria().size()) {
      warnings.add("Duplicate acceptance criteria detected. Merging recommended.");
    }

    if (!warnings.isEmpty()) {
      log.warn("⚠️ Decomposition warnings for {}: {}", story.id(), String.join("; ", warnings));
    }
    return warnings;
  }
}