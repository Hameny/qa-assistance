// src/main/java/com/qaassist/domain/factory/ArtifactFactory.java
package com.qaassist.domain.factory;

import com.qaassist.domain.artifact.*;
import com.qaassist.domain.common.*;
import com.qaassist.domain.requirement.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Централизованная фабрика для создания артефактов.
 * Гарантирует консистентность и применение бизнес-правил.
 */
public class ArtifactFactory {

  private ArtifactFactory() {} // utility class

  public static TestCase createTestCase(String name, String description, TestType type) {
    return TestCase.builder()
        .name(name)
        .description(description)
        .type(type)
        .priority(Priority.MEDIUM)
        .traceability(new Traceability(
            UUID.randomUUID(), List.of(), List.of(), null, null
        ))
        .build();
  }

  public static TestStep createApiStep(String action, String method, String endpoint) {
    return new TestStep(
        UUID.randomUUID(),
        action,
        "Response status 200",
        5, // seconds
        List.of(Assertion.equals("Status check", "200", "$.status")),
        java.util.Optional.empty(),
        java.util.Optional.empty(),
        java.util.Optional.of(new TestStep.ApiCall(method, endpoint, java.util.Optional.empty(), java.util.Optional.empty()))
    );
  }

  public static TestStep createUiStep(String action, String selector, SelectorType type) {
    return new TestStep(
        UUID.randomUUID(),
        action,
        "Element is visible",
        3,
        List.of(Assertion.isPresent("Element check", "$.visible")),
        java.util.Optional.empty(),
        java.util.Optional.of(new TestStep.UiLocator(selector, type, java.util.Optional.empty(), java.util.Optional.empty())),
        java.util.Optional.empty()
    );
  }

  public static AcceptanceCriterion fromGherkin(String title, String gherkin, String source) {
    // Простой парсинг Given-When-Then
    var parts = gherkin.split("(?i)^(given|when|then)\\s*", -1);
    if (parts.length < 4) {
      throw new IllegalArgumentException("Invalid Gherkin format");
    }
    return new AcceptanceCriterion(
        UUID.randomUUID(), title, gherkin, Priority.MEDIUM, source,
        parts[1].trim(), parts[2].trim(), parts[3].trim(), true
    );
  }
}