package com.qaassist.domain.artifact;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Контейнер для набора тест-кейсов, сгенерированных под одну User Story.
 */
public record TestSuite(
    UUID id,
    String suiteName,
    String sourceStoryId,
    List<TestCase> testCases,
    Instant generatedAt,
    GenerationMetadata metadata
) {
  public TestSuite {
    if (testCases == null) testCases = List.of();
    if (metadata == null) metadata = new GenerationMetadata(List.of(), List.of());
  }

  public record GenerationMetadata(
      List<String> appliedTechniques,
      List<String> warnings
  ) {}

  public int totalSteps() {
    return testCases.stream()
        .mapToInt(tc -> tc.steps().size())
        .sum();
  }

  public double averageStepsPerCase() {
    return testCases.isEmpty() ? 0 : (double) totalSteps() / testCases.size();
  }
}