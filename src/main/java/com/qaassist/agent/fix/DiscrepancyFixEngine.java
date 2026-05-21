// src/main/java/com/qaassist/agent/fix/DiscrepancyFixEngine.java
package com.qaassist.agent.fix;

import com.qaassist.domain.artifact.TestCase;
import com.qaassist.domain.artifact.TestStep;
import com.qaassist.domain.artifact.TestSuite;
import com.qaassist.domain.discrepancy.Discrepancy;
import com.qaassist.domain.discrepancy.FixStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DiscrepancyFixEngine {

  /**
   * Применяет авто-фиксы к расхождениям, если они безопасны.
   * Возвращает обновлённый TestSuite и список применённых фиксов.
   */
  public FixResult applyAutoFixes(TestSuite suite, List<Discrepancy> discrepancies) {
    List<Discrepancy> applied = new ArrayList<>();
    TestSuite current = suite;

    for (Discrepancy d : discrepancies) {
      if (!d.isAutoFixable()) continue;

      FixStrategy safeFix = d.possibleFixes().stream()
          .filter(FixStrategy::isSafeToAutoApply)
          .max(Comparator.comparingDouble(FixStrategy::confidenceScore))
          .orElse(null);

      if (safeFix == null) continue;

      try {
        current = applyFix(current, d, safeFix);
        applied.add(d.withStatus(Discrepancy.ResolutionStatus.AUTO_FIXED));
        log.info("🔧 Auto-fixed: {} via {}", d.title(), safeFix.name());
      } catch (Exception e) {
        log.warn("⚠️ Failed to auto-fix {}: {}", d.title(), e.getMessage());
        applied.add(d.withStatus(Discrepancy.ResolutionStatus.ESCALATED));
      }
    }

    return new FixResult(current, applied, discrepancies.stream()
        .filter(d -> !applied.contains(d))
        .toList());
  }

  private TestSuite applyFix(TestSuite suite, Discrepancy d, FixStrategy fix) {
    return switch (fix.action()) {
      case UPDATE_SELECTOR -> updateSelectors(suite, fix.parameters());
      case UPDATE_TEST_STEP -> updateTestSteps(suite, fix.parameters());
      case ADD_MISSING_ASSERTION -> addAssertions(suite, fix.parameters());
      case GENERATE_MISSING_TEST -> generateMissingTests(suite, d, fix.parameters());
      default -> suite; // Остальные стратегии требуют ручного вмешательства
    };
  }

  private TestSuite updateSelectors(TestSuite suite, Map<String, String> params) {
    String oldSelector = params.get("old_selector");
    String newSelector = params.get("suggested");
    if (oldSelector == null || newSelector == null) return suite;

    List<TestCase> updated = suite.testCases().stream()
        .map(tc -> {
          List<TestStep> newSteps = tc.steps().stream()
              .map(step -> step.uiLocator().map(loc ->
                  loc.selector().equals(oldSelector)
                      ? step.withUiLocator(new TestStep.UiLocator(
                      newSelector, loc.type(), loc.frame(), loc.description()))
                      : step
              ).orElse(step))
              .toList();
          return tc.withSteps(newSteps);
        })
        .toList();
    return suite.withTestCases(updated);
  }

  // Заглушки для остальных методов (реализация зависит от доменной модели)
  private TestSuite updateTestSteps(TestSuite suite, Map<String, String> params) { return suite; }
  private TestSuite addAssertions(TestSuite suite, Map<String, String> params) { return suite; }
  private TestSuite generateMissingTests(TestSuite suite, Discrepancy d, Map<String, String> params) { return suite; }

  public record FixResult(
      TestSuite updatedSuite,
      List<Discrepancy> autoFixed,
      List<Discrepancy> requiresManualReview
  ) {}
}