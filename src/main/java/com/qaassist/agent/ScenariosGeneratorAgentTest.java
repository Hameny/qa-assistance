package com.qaassist.agent;

import com.qaassist.agent.engine.IstqbTechniquesEngine;
import com.qaassist.domain.artifact.TestSuite;
import com.qaassist.domain.artifact.TraceabilityBuilder;
import com.qaassist.domain.common.Priority;
import com.qaassist.domain.requirement.AcceptanceCriterion;
import com.qaassist.domain.requirement.UserStory;
import com.qaassist.validation.schema.ScenarioQualityGate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScenariosGeneratorAgentTest {

  @Mock private IstqbTechniquesEngine engine;
  @Mock private TraceabilityBuilder traceabilityBuilder;
  @Mock private ScenarioQualityGate qualityGate;
  @InjectMocks private ScenariosGeneratorAgent agent;

  @Test
  @DisplayName("Успешная генерация сьюта с прохождением Quality Gate")
  void successfulGenerationWithQualityGatePass() {
    // Arrange
    UserStory story = new UserStory(
        UUID.randomUUID(), "Login Story", "Auth module", Priority.HIGH,
        "PROJ-100", List.of(
        new AcceptanceCriterion(UUID.randomUUID(), "Valid Login", "Must accept correct creds", Priority.HIGH, "PROJ-100", "Given on page", "When submit", "Then success", true)
    ), List.of(), Instant.now(), Instant.now()
    );

    var mockCase = mock(com.qaassist.domain.artifact.TestCase.class);
    when(engine.generateFrom(story)).thenReturn(List.of(mockCase, mockCase));
    when(traceabilityBuilder.build(any(), any(), any())).thenReturn(mock(com.qaassist.domain.artifact.Traceability.class));
    when(qualityGate.validate(any())).thenReturn(new ScenarioQualityGate.ValidationResult(true, Set.of()));

    // Act
    TestSuite result = agent.generate(story);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.testCases()).hasSize(2);
    assertThat(result.sourceStoryId()).isEqualTo(story.id().toString());
    verify(engine).generateFrom(story);
    verify(qualityGate).validate(any());
  }

  @Test
  @DisplayName("Выброс исключения при падении Quality Gate")
  void throwsOnQualityGateFailure() {
    UserStory story = mock(UserStory.class);
    when(story.title()).thenReturn("Fail Story");
    when(engine.generateFrom(story)).thenReturn(List.of());
    when(qualityGate.validate(any())).thenReturn(new ScenarioQualityGate.ValidationResult(false, Set.of("Empty suite")));

    assertThatThrownBy(() -> agent.generate(story))
        .isInstanceOf(ScenariosGeneratorAgent.ScenarioGenerationException.class)
        .hasMessageContaining("Quality Gate failed");
  }
}