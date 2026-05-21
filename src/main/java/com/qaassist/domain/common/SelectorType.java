// src/main/java/com/qaassist/domain/common/SelectorType.java
package com.qaassist.domain.common;

/**
 * Типы UI-локаторов с приоритетом устойчивости.
 */
public enum SelectorType {
  TEST_ID("data-testid", 100),
  ROLE("ARIA role", 90),
  CSS_ATTRIBUTE("[attr]", 80),
  CSS_CLASS(".class", 70),
  CSS_TAG("tag", 60),
  XPATH("//...", 40),
  TEXT("text=", 30);

  private final String strategy;
  private final int stabilityScore; // Чем выше — тем устойчивее

  SelectorType(String strategy, int stabilityScore) {
    this.strategy = strategy;
    this.stabilityScore = stabilityScore;
  }

  public String strategy() { return strategy; }
  public int stabilityScore() { return stabilityScore; }

  public static SelectorType fromString(String raw) {
    if (raw == null) return CSS_CLASS;
    return switch (raw.toUpperCase()) {
      case "TEST_ID", "DATA-TESTID" -> TEST_ID;
      case "ROLE", "ARIA" -> ROLE;
      case "XPATH" -> XPATH;
      case "TEXT" -> TEXT;
      default -> CSS_CLASS;
    };
  }
}