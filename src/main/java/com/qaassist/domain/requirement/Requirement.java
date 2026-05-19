// src/main/java/com/qaassist/domain/requirement/Requirement.java
package com.qaassist.domain.requirement;

import com.qaassist.domain.common.Priority;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/**
 * Базовый контракт для всех типов требований.
 * Sealed — расширение только в этом пакете.
 */
public sealed interface Requirement
    permits FunctionalRequirement, NonFunctionalRequirement, AcceptanceCriterion {

  UUID id();
  @NotBlank String title();
  @NotBlank String description();
  Priority priority();
  String source();  // Jira key, Confluence URL, etc.
}