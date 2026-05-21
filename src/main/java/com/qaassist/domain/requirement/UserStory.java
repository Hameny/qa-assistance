// src/main/java/com/qaassist/domain/requirement/UserStory.java
package com.qaassist.domain.requirement;

import com.qaassist.domain.common.Priority;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Связанная пользовательская история с критериями приёмки и бизнес-правилами.
 * Используется как вход для генерации сценариев.
 */
public record UserStory(
    UUID id,
    String title,
    String description,
    Priority priority,
    String source,               // Jira key: PROJ-123
    List<AcceptanceCriterion> acceptanceCriteria,
    List<String> businessRules,
    Instant createdAt,
    Instant updatedAt
) {
  public UserStory {
    if (acceptanceCriteria == null) acceptanceCriteria = List.of();
    if (businessRules == null) businessRules = List.of();
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
  }

  public boolean hasCriticalAcceptanceCriteria() {
    return acceptanceCriteria.stream().anyMatch(ac -> ac.priority() == Priority.CRITICAL);
  }
}