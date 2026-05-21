package com.qaassist.agent;

import com.qaassist.agent.engine.IstqbTechniquesEngine;
import com.qaassist.domain.artifact.TestCase;
import com.qaassist.domain.artifact.TestSuite;
import com.qaassist.domain.artifact.TraceabilityBuilder;
import com.qaassist.domain.requirement.UserStory;
import com.qaassist.validation.schema.ScenarioQualityGate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScenariosGeneratorAgent {

  private final IstqbTechniquesEngine engine;
  private final TraceabilityBuilder traceabilityBuilder;
  private final ScenarioQualityGate qualityGate;

  /**
   * Полный цикл генерации: алгоритмическое ядро → дедупликация → трассируемость → Quality Gate.
   */
  public TestSuite generate(UserStory story) {
    log.info("🚀 Generating test scenarios for story: {}", story.title());

    // 1. Алгоритмическая генерация
    List<TestCase> rawCases = engine.generateFrom(story);

    // 2. Дедупликация по хешу шагов (убираем идентичные кейсы)
    List<TestCase> deduplicated = removeDuplicates(rawCases);

    // 3. Обогащение трассируемостью
    List<TestCase> traceable = deduplicated.stream()
        .map(tc -> enrichTraceability(tc, story))
        .toList();

    // 4. Сборка сьюта
    TestSuite suite = new TestSuite(
        UUID.randomUUID(),
        story.title() + " - Test Suite",
        story.id().toString(),
        traceable,
        Instant.now(),
        new TestSuite.GenerationMetadata(List.of("BOUNDARY_VALUE", "EQUIVALENCE"), List.of())
    );

    // 5. Quality Gate валидация
    var validationResult = qualityGate.validate(suite);
    if (!validationResult.isValid()) {
      log.error("❌ Scenario generation failed Quality Gate: {}", validationResult.errors());
      throw new ScenarioGenerationException("Quality Gate failed: " + validationResult.errors());
    }

    log.info("✅ Generated suite: {} cases, {} total steps", suite.testCases().size(), suite.totalSteps());
    return suite;
  }

  private List<TestCase> removeDuplicates(List<TestCase> cases) {
    return cases.stream()
        .distinct()
        .toList();
  }

  private TestCase enrichTraceability(TestCase tc, UserStory story) {
    // Находим связанный AC по подстроке в названии
    var relatedAc = story.acceptanceCriteria().stream()
        .filter(ac -> tc.name().contains(ac.title().substring(0, Math.min(ac.title().length(), 20))))
        .findFirst()
        .orElse(story.acceptanceCriteria().get(0));

    return new TestCase(
        tc.id(), tc.name(), tc.description(), tc.priority(), tc.type(),
        tc.steps(), traceabilityBuilder.build(story, relatedAc, tc),
        tc.createdAt(), tc.updatedAt(), tc.linkedRequirements()
    );
  }

  public static class ScenarioGenerationException extends RuntimeException {
    public ScenarioGenerationException(String message) { super(message); }
  }
}