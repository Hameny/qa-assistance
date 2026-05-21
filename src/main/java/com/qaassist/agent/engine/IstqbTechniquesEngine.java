package com.qaassist.agent.engine;

import com.qaassist.domain.artifact.TestCase;
import com.qaassist.domain.artifact.TestStep;
import com.qaassist.domain.artifact.Assertion;
import com.qaassist.domain.common.Priority;
import com.qaassist.domain.common.TestType;
import com.qaassist.domain.requirement.AcceptanceCriterion;
import com.qaassist.domain.requirement.UserStory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class IstqbTechniquesEngine {

  // Паттерны для поиска числовых границ в тексте AC
  private static final Pattern RANGE_PATTERN = Pattern.compile("(>=|<=|>|<|=|\\bfrom\\b|\\bto\\b)\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
  private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b(\\d+)\\b");

  /**
   * Генерирует тест-кейсы на основе Acceptance Criteria с применением техник ISTQB.
   */
  public List<TestCase> generateFrom(UserStory story) {
    List<TestCase> generated = new ArrayList<>();
    List<String> appliedTechniques = new ArrayList<>();

    for (AcceptanceCriterion ac : story.acceptanceCriteria()) {
      generated.addAll(generateBoundaryTests(story, ac));
      appliedTechniques.add("BOUNDARY_VALUE");

      generated.addAll(generateEquivalenceTests(story, ac));
      appliedTechniques.add("EQUIVALENCE_PARTITIONING");
    }

    log.info("🧩 Generated {} test cases using techniques: {}", generated.size(), appliedTechniques);
    return generated;
  }

  private List<TestCase> generateBoundaryTests(UserStory story, AcceptanceCriterion ac) {
    List<TestCase> cases = new ArrayList<>();
    Matcher matcher = NUMBER_PATTERN.matcher(ac.description());

    if (!matcher.find()) return cases; // Нет числовых ограничений → пропускаем

    List<Integer> boundaries = new ArrayList<>();
    do { boundaries.add(Integer.parseInt(matcher.group(1))); } while (matcher.find());

    for (Integer val : boundaries) {
      // Граничные значения: val-1, val, val+1
      cases.add(createBoundaryCase(story, ac, val - 1, "MINUS_ONE"));
      cases.add(createBoundaryCase(story, ac, val, "EXACT"));
      cases.add(createBoundaryCase(story, ac, val + 1, "PLUS_ONE"));
    }
    return cases;
  }

  private List<TestCase> generateEquivalenceTests(UserStory story, AcceptanceCriterion ac) {
    // Базовый позитивный и негативный кейсы
    return List.of(
        createEquivalenceCase(story, ac, "VALID_INPUT", true),
        createEquivalenceCase(story, ac, "INVALID_INPUT", false)
    );
  }

  private TestCase createBoundaryCase(UserStory story, AcceptanceCriterion ac, int value, String variant) {
    return TestCase.builder()
        .name("[BVA] " + ac.title() + " | Value: " + value)
        .description("Boundary Value Analysis: " + variant)
        .priority(Priority.HIGH)
        .type(TestType.FUNCTIONAL)
        .steps(List.of(
            new TestStep(
                UUID.randomUUID(),
                "Enter boundary value " + value,
                value < 0 || value > 100 ? "Error message displayed" : "Action accepted",
                5,
                List.of(Assertion.equals("Result validation", variant.contains("EXACT") ? "SUCCESS" : "ERROR", "$.outcome")),
                java.util.Optional.empty(),
                java.util.Optional.empty(),
                java.util.Optional.of(new TestStep.ApiCall("POST", "/api/test/validate", java.util.Optional.of("{\"value\":" + value + "}"), java.util.Optional.empty()))
            )
        ))
        .traceability(new Traceability(UUID.randomUUID(), List.of(story.source()), List.of(ac.title()), story.title(), "jira:" + story.source()))
        .linkedRequirements(List.of())
        .build();
  }

  private TestCase createEquivalenceCase(UserStory story, AcceptanceCriterion ac, String partition, boolean isValid) {
    return TestCase.builder()
        .name("[EP] " + ac.title() + " | Partition: " + partition)
        .description("Equivalence Partitioning: " + partition)
        .priority(Priority.MEDIUM)
        .type(TestType.FUNCTIONAL)
        .steps(List.of(
            new TestStep(
                UUID.randomUUID(),
                "Submit " + (isValid ? "valid" : "invalid") + " data sample",
                isValid ? "Success flow continues" : "Validation error shown",
                4,
                List.of(Assertion.isPresent("Flow outcome", "$.status")),
                java.util.Optional.empty(),
                java.util.Optional.empty(),
                java.util.Optional.of(new TestStep.ApiCall("POST", "/api/test/validate", java.util.Optional.of("{\"sample\":\"" + partition + "\"}"), java.util.Optional.empty()))
            )
        ))
        .traceability(new Traceability(UUID.randomUUID(), List.of(story.source()), List.of(ac.title()), story.title(), "jira:" + story.source()))
        .linkedRequirements(List.of())
        .build();
  }
}