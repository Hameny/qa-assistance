// src/main/java/com/qaassist/domain/common/Priority.java
package com.qaassist.domain.common;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Priority {
  LOW(1, "Низкий"),
  MEDIUM(2, "Средний"),
  HIGH(3, "Высокий"),
  CRITICAL(4, "Критический");

  private final int weight;
  private final String label;

  Priority(int weight, String label) {
    this.weight = weight;
    this.label = label;
  }

  public int weight() { return weight; }

  @JsonValue
  public String label() { return label; }

  public static Priority fromWeight(int weight) {
    for (Priority p : values()) {
      if (p.weight == weight) return p;
    }
    return MEDIUM;
  }
}