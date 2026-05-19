// src/test/java/com/qaassist/domain/artifact/TestCaseTest.java
package com.qaassist.domain.artifact;

import com.qaassist.domain.common.Priority;
import com.qaassist.domain.common.TestType;
import com.qaassist.domain.factory.ArtifactFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class TestCaseTest {

  @Test
  @DisplayName("Создание TestCase через builder")
  void createTestCaseViaBuilder() {
    var testCase = TestCase.builder()
        .name("Login with valid credentials")
        .description("User can login with correct username and password")
        .priority(Priority.HIGH)
        .type(TestType.FUNCTIONAL)
        .steps(List.of(
            ArtifactFactory.createUiStep("Enter username", "#username", SelectorType.CSS),
            ArtifactFactory.createUiStep("Enter password", "#password", SelectorType.CSS),
            ArtifactFactory.createUiStep("Click login", "#login-btn", SelectorType.CSS)
        ))
        .traceability(new Traceability(
            UUID.randomUUID(),
            List.of("REQ-AUTH-001"),
            List.of("AC-1: Success message displayed"),
            "US-123",
            "jira:PROJ-456"
        ))
        .build();

    assertThat(testCase.name()).isEqualTo("Login with valid credentials");
    assertThat(testCase.priority()).isEqualTo(Priority.HIGH);
    assertThat(testCase.steps()).hasSize(3);
    assertThat(testCase.traceability().requirementIds()).contains("REQ-AUTH-001");
  }

  @Test
  @DisplayName("Валидация: пустые шаги — ошибка")
  void emptyStepsShouldThrow() {
    assertThatThrownBy(() -> TestCase.builder()
        .name("Invalid test")
        .steps(List.of())
        .traceability(new Traceability(UUID.randomUUID(), List.of(), List.of(), null, null))
        .build()
    ).hasMessageContaining("at least one step");
  }

  @ParameterizedTest
  @EnumSource(TestType.class)
  @DisplayName("TestCase поддерживает все типы тестов")
  void supportsAllTestTypes(TestType type) {
    var testCase = ArtifactFactory.createTestCase("Test", "Desc", type);
    assertThat(testCase.type()).isEqualTo(type);
  }

  @Test
  @DisplayName("Utility method: coversRequirement")
  void coversRequirementCheck() {
    var traceability = new Traceability(
        UUID.randomUUID(),
        List.of("REQ-1", "REQ-2", "REQ-3"),
        List.of(), null, null
    );
    var testCase = TestCase.builder()
        .name("Test")
        .traceability(traceability)
        .steps(List.of(ArtifactFactory.createUiStep("Step", "#el", SelectorType.CSS)))
        .build();

    assertThat(testCase.coversRequirement("REQ-2")).isTrue();
    assertThat(testCase.coversRequirement("REQ-999")).isFalse();
  }
}