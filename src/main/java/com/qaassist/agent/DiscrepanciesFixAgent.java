// src/main/java/com/qaassist/agent/DiscrepanciesFixAgent.java
package com.qaassist.agent;

import com.qaassist.agent.analysis.DiscrepancyAnalyzer;
import com.qaassist.agent.fix.DiscrepancyFixEngine;
import com.qaassist.agent.fix.DiscrepancyFixEngine.FixResult;
import com.qaassist.agent.report.DiscrepancyReportGenerator;
import com.qaassist.domain.artifact.TestSuite;
import com.qaassist.domain.discrepancy.Discrepancy;
import com.qaassist.domain.requirement.UserStory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscrepanciesFixAgent {

  private final DiscrepancyAnalyzer analyzer;
  private final DiscrepancyFixEngine fixEngine;
  private final DiscrepancyReportGenerator reportGenerator;

  /**
   * Полный цикл: анализ → авто-фикс → генерация отчёта.
   */
  public FixAgentResult process(UserStory story, TestSuite suite, String projectId) {
    log.info("🔍 Starting discrepancy analysis for story: {}", story.title());

    // 1. Анализ
    List<Discrepancy> allDiscrepancies = analyzer.analyze(story, suite);

    // 2. Авто-фикс
    FixResult fixResult = fixEngine.applyAutoFixes(suite, allDiscrepancies);

    // 3. Отчёт
    String report = reportGenerator.generateReport(
        projectId,
        allDiscrepancies,
        fixResult.autoFixed(),
        fixResult.requiresManualReview()
    );

    log.info("✅ Discrepancy processing complete: {} fixed, {} pending",
        fixResult.autoFixed().size(), fixResult.requiresManualReview().size());

    return new FixAgentResult(
        fixResult.updatedSuite(),
        allDiscrepancies,
        fixResult.autoFixed(),
        fixResult.requiresManualReview(),
        report
    );
  }

  public record FixAgentResult(
      TestSuite updatedSuite,
      List<Discrepancy> allDiscrepancies,
      List<Discrepancy> autoFixed,
      List<Discrepancy> requiresManualReview,
      String markdownReport
  ) {
    public boolean hasCriticalIssues() {
      return requiresManualReview.stream()
          .anyMatch(d -> d.severity() == Discrepancy.Severity.CRITICAL);
    }
  }
}