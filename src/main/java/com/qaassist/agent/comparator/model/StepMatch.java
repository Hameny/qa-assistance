// src/main/java/com/qaassist/agent/comparator/model/StepMatch.java
package com.qaassist.agent.comparator.model;

public record StepMatch(
    String scenarioStepId,
    String codeLocation,      // e.g. "LoginTest.java:24"
    MatchType type,
    double similarityScore
) {
  public enum MatchType { ACTION_MATCH, ASSERTION_MATCH, FULL_MATCH, NO_MATCH }
}