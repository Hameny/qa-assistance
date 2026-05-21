// src/main/java/com/qaassist/agent/zephyr/ZephyrMapper.java
package com.qaassist.agent.zephyr;

import com.qaassist.domain.artifact.TestCase;
import com.qaassist.domain.artifact.TestStep;
import com.qaassist.domain.artifact.Assertion;
import com.qaassist.domain.common.Priority;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ZephyrMapper {

  /**
   * Преобразует TestCase в JSON-структуру, совместимую с Zephyr Scale v2 API.
   */
  public Map<String, Object> toZephyrPayload(TestCase testCase, String projectKey) {
    Map<String, Object> payload = new LinkedHashMap<>();

    payload.put("projectId", projectKey);
    payload.put("name", truncate(testCase.name(), 250));
    payload.put("objective", truncate(testCase.description(), 4000));
    payload.put("priority", mapPriority(testCase.priority()));
    payload.put("externalId", testCase.id().toString());
    payload.put("labels", extractTags(testCase));
    payload.put("testScriptType", "TABLE"); // Zephyr поддерживает TABLE, BDD, GHERKIN
    payload.put("testScript", mapSteps(testCase.steps()));

    // Preconditions из traceability
    if (testCase.traceability() != null && !testCase.traceability().requirementIds().isEmpty()) {
      payload.put("preconditions", "Requirements: " + String.join(", ", testCase.traceability().requirementIds()));
    }

    return payload;
  }

  private List<Map<String, String>> mapSteps(List<TestStep> steps) {
    return steps.stream().map(step -> {
      Map<String, String> row = new LinkedHashMap<>();
      row.put("index", String.valueOf(step.hashCode() % 10000)); // Простой индекс
      row.put("action", step.action());
      row.put("data", step.testDataRef().orElse(""));
      row.put("expectedResult", step.expected());
      // Добавляем assertions как часть expected result
      if (!step.assertions().isEmpty()) {
        String asserts = step.assertions().stream()
            .map(a -> "✅ " + a.description() + ": " + a.expectedValue())
            .collect(Collectors.joining("\n"));
        row.put("expectedResult", row.get("expectedResult") + "\n" + asserts);
      }
      return row;
    }).toList();
  }

  private String mapPriority(Priority p) {
    return switch (p) {
      case CRITICAL -> "Highest";
      case HIGH -> "High";
      case MEDIUM -> "Medium";
      case LOW -> "Low";
    };
  }

  private List<String> extractTags(TestCase tc) {
    List<String> tags = new ArrayList<>();
    tags.add(tc.type().name().toLowerCase());
    tags.add("auto-generated");
    tags.add("qa-assist");
    if (tc.traceability() != null) {
      tags.addAll(tc.traceability().requirementIds());
    }
    return tags;
  }

  private String truncate(String s, int max) {
    return s == null ? "" : (s.length() > max ? s.substring(0, max) + "..." : s);
  }
}