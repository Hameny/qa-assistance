// src/main/java/com/qaassist/domain/requirement/NonFunctionalRequirement.java
package com.qaassist.domain.requirement;

import com.qaassist.domain.common.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record NonFunctionalRequirement(
    UUID id,
    @NotBlank String title,
    @NotBlank String description,
    Priority priority,
    String source,
    NfrCategory category,
    @Positive double threshold,      // например, 2000 мс для response time
    String measurementUnit,          // ms, %, users, etc.
    String validationMethod          // как проверять: load test, audit, etc.
) implements Requirement {

  public enum NfrCategory {
    PERFORMANCE,
    SECURITY,
    USABILITY,
    RELIABILITY,
    SCALABILITY,
    COMPATIBILITY,
    MAINTAINABILITY
  }
}