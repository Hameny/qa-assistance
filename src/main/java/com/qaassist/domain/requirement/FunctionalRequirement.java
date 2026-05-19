// src/main/java/com/qaassist/domain/requirement/FunctionalRequirement.java
package com.qaassist.domain.requirement;

import com.qaassist.domain.common.Priority;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public record FunctionalRequirement(
    UUID id,
    @NotBlank String title,
    @NotBlank String description,
    Priority priority,
    String source,
    List<String> preconditions,      // шаги для подготовки
    List<@NotBlank String> workflow, // основной сценарий
    List<@NotBlank String> postconditions,
    List<@NotBlank String> businessRules
) implements Requirement {

  public boolean hasPreconditions() { return preconditions != null && !preconditions.isEmpty(); }
  public boolean hasBusinessRules() { return businessRules != null && !businessRules.isEmpty(); }
}