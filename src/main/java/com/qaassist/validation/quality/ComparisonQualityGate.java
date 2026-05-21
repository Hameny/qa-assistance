// src/main/java/com/qaassist/validation/quality/ComparisonQualityGate.java
package com.qaassist.validation.quality;

import com.qaassist.agent.comparator.AutomationComparator;
import com.qaassist.agent.comparator.model.ComparisonReport;
import com.qaassist.config.properties.AppProperties;
import com.qaassist.domain.artifact.TestSuite;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ComparisonQualityGate {

  private final AutomationComparator comparator;
  private final AppProperties.PipelineProperties pipelineProps;

  public ComparisonReport validate(TestSuite suite, String generatedCode) {
    ComparisonReport report = comparator.compare(suite, generatedCode);

    if (!report.passesQualityGate(pipelineProps.getQualityGates().getMinCoveragePercent())) {
      log.error("🚫 Quality Gate FAILED: Coverage {}% < {}%",
          report.coveragePercent(), pipelineProps.getQualityGates().getMinCoveragePercent());
      throw new QualityGateFailedException(
          "Code-scenario mismatch: %.1f%% coverage. Discrepancies: %d".formatted(
              report.coveragePercent(), report.discrepancies().size())
      );
    }

    log.info("✅ Quality Gate PASSED: Coverage {}%, Status: {}", report.coveragePercent(), report.overallStatus());
    return report;
  }

  public static class QualityGateFailedException extends RuntimeException {
    public QualityGateFailedException(String message) { super(message); }
  }
}