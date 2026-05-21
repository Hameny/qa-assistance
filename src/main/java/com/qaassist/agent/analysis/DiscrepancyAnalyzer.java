// src/main/java/com/qaassist/agent/analysis/DiscrepancyAnalyzer.java
package com.qaassist.agent.analysis;

import com.qaassist.domain.artifact.TestCase;
import com.qaassist.domain.artifact.TestSuite;
import com.qaassist.domain.discrepancy.*;
import com.qaassist.domain.requirement.UserStory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscrepancyAnalyzer {

  private static final Pattern CSS_SELECTOR_PATTERN = Pattern.compile("[#.][\\w-]+");
  private static final Pattern API_PATH_PATTERN = Pattern.compile("(GET|POST|PUT|DELETE)\\s+(/[\\w/{}-]+)");

  /**
   * Анализирует UserStory + TestSuite на предмет расхождений.
   */
  public List<Discrepancy> analyze(UserStory story, TestSuite suite) {
    List<Discrepancy> discrepancies = new ArrayList<>();

    // 1. Проверка покрытия требований тестами
    discrepancies.addAll(findCoverageGaps(story, suite));

    // 2. Проверка соответствия селекторов (если есть UI-шаги)
    discrepancies.addAll(findSelectorMismatches(suite));

    // 3. Проверка API-контрактов (если есть API-шаги)
    discrepancies.addAll(findApiContractViolations(suite));

    // 4. Проверка противоречий в описаниях
    discrepancies.addAll(findRequirementContradictions(story));

    log.info("🔍 Analysis complete: {} discrepancies found", discrepancies.size());
    return discrepancies;
  }

  private List<Discrepancy> findCoverageGaps(UserStory story, TestSuite suite) {
    List<Discrepancy> gaps = new ArrayList<>();
    Set<String> coveredAc = suite.testCases().stream()
        .flatMap(tc -> Optional.ofNullable(tc.traceability())
            .map(t -> t.acceptanceCriteria().stream())
            .orElse(Stream.empty()))
        .collect(Collectors.toSet());

    for (var ac : story.acceptanceCriteria()) {
      if (!coveredAc.contains(ac.title())) {
        gaps.add(new Discrepancy(
            UUID.randomUUID(),
            "Missing test coverage",
            "Acceptance criterion '%s' has no associated test case".formatted(ac.title()),
            Discrepancy.DiscrepancyType.TEST_COVERAGE_GAP,
            Discrepancy.Severity.HIGH,
            List.of(new SourceArtifact(SourceArtifact.SourceType.JIRA, story.source(), ac.description(), story.updatedAt())),
            SourceOfTruth.JIRA,
            List.of(new FixStrategy(
                "Generate missing test",
                "Auto-generate test case for uncovered AC",
                FixStrategy.FixAction.GENERATE_MISSING_TEST,
                Map.of("ac_id", ac.id().toString(), "story_id", story.id().toString()),
                0.85
            )),
            Discrepancy.ResolutionStatus.DETECTED,
            Instant.now(),
            null,
            null
        ));
      }
    }
    return gaps;
  }

  private List<Discrepancy> findSelectorMismatches(TestSuite suite) {
    List<Discrepancy> mismatches = new ArrayList<>();

    for (TestCase tc : suite.testCases()) {
      for (var step : tc.steps()) {
        step.uiLocator().ifPresent(locator -> {
          String selector = locator.selector();
          // Простая эвристика: селекторы с "btn-submit" должны быть <button>
          if (selector.contains("btn-submit") && !selector.matches("button.*")) {
            mismatches.add(createSelectorDiscrepancy(tc, step, selector));
          }
        });
      }
    }
    return mismatches;
  }

  private Discrepancy createSelectorDiscrepancy(TestCase tc, com.qaassist.domain.artifact.TestStep step, String selector) {
    return new Discrepancy(
        UUID.randomUUID(),
        "Selector type mismatch",
        "Selector '%s' may not match expected element type".formatted(selector),
        Discrepancy.DiscrepancyType.SELECTOR_MISMATCH,
        Discrepancy.Severity.MEDIUM,
        List.of(new SourceArtifact(SourceArtifact.SourceType.TEST_SUITE, tc.name(), selector, tc.updatedAt())),
        SourceOfTruth.FIGMA,
        List.of(new FixStrategy(
            "Update selector type",
            "Convert to button[selector] or [data-testid]",
            FixStrategy.FixAction.UPDATE_SELECTOR,
            Map.of("old_selector", selector, "suggested", "button[data-testid='%s']".formatted(selector.replace("#", ""))),
            0.75
        )),
        Discrepancy.ResolutionStatus.DETECTED,
        Instant.now(),
        null,
        null
    );
  }

  private List<Discrepancy> findApiContractViolations(TestSuite suite) {
    // Заглушка: в реальной реализации — загрузка OpenAPI и сравнение с шагами
    // Здесь возвращаем пустой список для компиляции
    return List.of();
  }

  private List<Discrepancy> findRequirementContradictions(UserStory story) {
    // Заглушка: анализ текста на противоречия (NLP или LLM)
    return List.of();
  }
}