// src/main/java/com/qaassist/agent/comparator/AutomationComparator.java
package com.qaassist.agent.comparator;

import com.qaassist.agent.comparator.engine.CodeAnalyzer;
import com.qaassist.agent.comparator.engine.CodeAnalyzer.AnalyzedStep;
import com.qaassist.agent.comparator.engine.CodeAnalyzer.AnalyzedTest;
import com.qaassist.agent.comparator.model.*;
import com.qaassist.domain.artifact.Assertion;
import com.qaassist.domain.artifact.TestCase;
import com.qaassist.domain.artifact.TestStep;
import com.qaassist.domain.artifact.TestSuite;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutomationComparator {

  private final CodeAnalyzer analyzer;
  private static final double SIMILARITY_THRESHOLD = 0.7;

  /**
   * Сравнивает JSON-сценарии с сгенерированным Java-кодом.
   */
  public ComparisonReport compare(TestSuite suite, String generatedCode) {
    log.info("⚖️ Comparing suite '{}' with generated code", suite.suiteName());

    List<AnalyzedTest> codeTests = analyzer.analyze(generatedCode);
    List<StepMatch> matches = new ArrayList<>();
    List<ComparisonDiscrepancy> discrepancies = new ArrayList<>();
    int totalScenarioSteps = 0;
    int matchedSteps = 0;

    for (TestCase tc : suite.testCases()) {
      totalScenarioSteps += tc.steps().size();

      // Находим соответствующий метод в коде по имени/описанию
      Optional<AnalyzedTest> matchingMethod = codeTests.stream()
          .filter(m -> calculateSimilarity(m.displayName(), tc.name()) >= SIMILARITY_THRESHOLD)
          .findFirst();

      if (matchingMethod.isEmpty()) {
        discrepancies.add(new ComparisonDiscrepancy(tc.id().toString(),
            "Test method missing in generated code", ComparisonDiscrepancy.DiscrepancyType.MISSING_ACTION));
        continue;
      }

      // Сопоставляем шаги
      for (TestStep step : tc.steps()) {
        Optional<AnalyzedStep> codeStep = findMatchingStep(step, matchingMethod.get().steps());
        if (codeStep.isPresent()) {
          matchedSteps++;
          matches.add(new StepMatch(step.id().toString(), codeStep.get().lineInfo(),
              StepMatch.MatchType.FULL_MATCH, 1.0));
        } else {
          discrepancies.add(new ComparisonDiscrepancy(step.id().toString(),
              "Step not implemented in code", ComparisonDiscrepancy.DiscrepancyType.MISSING_ACTION));
        }
      }
    }

    double coverage = totalScenarioSteps > 0 ? (matchedSteps * 100.0 / totalScenarioSteps) : 0;
    ComparisonReport.ComparisonStatus status = coverage >= 90 ? ComparisonReport.ComparisonStatus.FULL_MATCH :
        coverage >= 60 ? ComparisonReport.ComparisonStatus.PARTIAL_COVERAGE :
            ComparisonReport.ComparisonStatus.CRITICAL_MISMATCH;

    return new ComparisonReport(suite.id(), coverage, matches, discrepancies, status);
  }

  private Optional<AnalyzedStep> findMatchingStep(TestStep scenario, List<AnalyzedStep> codeSteps) {
    String scenarioTarget = scenario.uiLocator().map(l -> l.value()).orElse(
        scenario.apiCall().map(a -> a.endpoint()).orElse("")
    );
    String scenarioAction = scenario.action().toLowerCase();

    return codeSteps.stream()
        .filter(cs -> calculateSimilarity(cs.target(), scenarioTarget) >= SIMILARITY_THRESHOLD ||
            calculateSimilarity(cs.type(), scenarioAction) >= SIMILARITY_THRESHOLD)
        .findFirst();
  }

  /** Простая метрика схожести строк (Jaccard-like) */
  private double calculateSimilarity(String a, String b) {
    if (a == null || b == null) return 0;
    String sa = a.toLowerCase().replaceAll("[^a-z0-9]", "");
    String sb = b.toLowerCase().replaceAll("[^a-z0-9]", "");
    if (sa.equals(sb)) return 1.0;
    if (sa.contains(sb) || sb.contains(sa)) return 0.8;
    return 0.0;
  }
}