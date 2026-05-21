// src/main/java/com/qaassist/domain/discrepancy/SourceOfTruth.java
package com.qaassist.domain.discrepancy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Иерархия доверия к источникам. Используется для разрешения конфликтов.
 */
@RequiredArgsConstructor
public enum SourceOfTruth {
  JIRA("Jira", 100),              // Наибольший приоритет
  OPENAPI("OpenAPI Spec", 90),
  FIGMA("Figma Design", 80),
  CONFLUENCE("Confluence", 70),
  TEST_SUITE("Existing Tests", 60),
  GLOBAL_CONTEXT("Global Context", 50),
  LLM_INFERENCE("LLM Inference", 30); // Наименьший приоритет

  @Getter
  private final String label;
  @Getter
  private final int priority;     // Чем выше — тем более "истинный"

  public static SourceOfTruth resolveConflict(SourceOfTruth a, SourceOfTruth b) {
    return a.priority >= b.priority ? a : b;
  }
}