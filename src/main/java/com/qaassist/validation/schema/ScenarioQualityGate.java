package com.qaassist.validation.schema;

import com.qaassist.domain.artifact.TestCase;
import com.qaassist.domain.artifact.TestSuite;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ScenarioQualityGate {

  /**
   * Проверяет сгенерированные сценарии перед передачей в автоматизацию.
   */
  public ValidationResult validate(TestSuite suite) {
    Set<String> errors = new HashSet<>();

    // 1. Проверка на пустоту
    if (suite.testCases().isEmpty()) {
      errors.add("Test suite contains zero cases");
    }

    // 2. Проверка трассируемости
    long traceable = suite.testCases().stream()
        .filter(tc -> tc.traceability() != null && !tc.traceability().requirementIds().isEmpty())
        .count();
    if (traceable < suite.testCases().size()) {
      errors.add("Not all test cases have requirement traceability");
    }

    // 3. Проверка шагов
    long casesWithoutSteps = suite.testCases().stream()
        .filter(tc -> tc.steps() == null || tc.steps().isEmpty())
        .count();
    if (casesWithoutSteps > 0) {
      errors.add(casesWithoutSteps + " test cases have no steps defined");
    }

    // 4. Проверка дубликатов имен
    Set<String> names = new HashSet<>();
    long duplicates = suite.testCases().stream()
        .map(TestCase::name)
        .filter(n -> !names.add(n))
        .count();
    if (duplicates > 0) {
      errors.add("Found " + duplicates + " duplicate test case names");
    }

    return new ValidationResult(errors.isEmpty(), errors);
  }

  public record ValidationResult(boolean valid, Set<String> errors) {}
}