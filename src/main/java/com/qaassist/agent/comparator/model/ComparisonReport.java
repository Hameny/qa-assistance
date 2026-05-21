// src/main/java/com/qaassist/agent/comparator/model/ComparisonReport.java
package com.qaassist.agent.comparator.model;

import java.util.List;
import java.util.UUID;

public record ComparisonReport(
    UUID suiteId,
    double coveragePercent,
    List<StepMatch> matches,
    List<ComparisonDiscrepancy> discrepancies,
    ComparisonStatus overallStatus
) {
  public boolean passesQualityGate(double minCoverage) {
    return coveragePercent >= minCoverage && overallStatus != ComparisonStatus.CRITICAL_MISMATCH;
  }

  public enum ComparisonStatus {
    FULL_MATCH,
    PARTIAL_COVERAGE,
    CRITICAL_MISMATCH
  }
}