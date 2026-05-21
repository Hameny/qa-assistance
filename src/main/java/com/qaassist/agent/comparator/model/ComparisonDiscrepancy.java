// src/main/java/com/qaassist/agent/comparator/model/ComparisonDiscrepancy.java
package com.qaassist.agent.comparator.model;

public record ComparisonDiscrepancy(
    String scenarioStepId,
    String description,
    DiscrepancyType type
) {
  public enum DiscrepancyType {
    MISSING_ASSERTION,
    MISSING_ACTION,
    WRONG_HTTP_METHOD,
    WRONG_LOCATOR,
    PARAMETER_MISMATCH
  }
}