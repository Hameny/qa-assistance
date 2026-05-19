// src/main/java/com/qaassist/domain/artifact/TestStep.java
package com.qaassist.domain.artifact;

import com.qaassist.domain.common.SelectorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record TestStep(
    UUID id,
    @NotBlank String action,
    @NotBlank String expected,
    @Positive int estimatedDurationSeconds,
    List<@Valid Assertion> assertions,
    Optional<String> testDataRef,
    Optional<UiLocator> uiLocator,
    Optional<ApiCall> apiCall
) {
  public TestStep {
    // Валидация: шаг должен иметь либо UI, либо API контекст (или оба для E2E)
    if (uiLocator.isEmpty() && apiCall.isEmpty()) {
      throw new IllegalArgumentException(
          "TestStep must define at least uiLocator or apiCall"
      );
    }
  }

  public boolean isUiStep() { return uiLocator.isPresent(); }
  public boolean isApiStep() { return apiCall.isPresent(); }

  // Вложенные records для UI/API специфик
  public record UiLocator(
      @NotBlank String selector,
      SelectorType type,
      Optional<String> frame,
      Optional<String> description
  ) {}

  public record ApiCall(
      @NotBlank String method,      // GET, POST, etc.
      @NotBlank String endpoint,    // /api/v1/users
      Optional<String> requestBody, // JSON schema reference
      Optional<List<Header>> headers
  ) {}

  public record Header(@NotBlank String name, @NotBlank String value) {}
}