// src/main/java/com/qaassist/domain/artifact/TestCase.java
package com.qaassist.domain.artifact;

import com.qaassist.domain.common.Priority;
import com.qaassist.domain.common.TestType;
import com.qaassist.domain.requirement.Requirement;
import com.qaassist.domain.validation.Traceable;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Traceable
public record TestCase(
    @NotNull UUID id,

    @NotBlank @Size(min = 3, max = 200) String name,

    @NotBlank @Size(max = 2000) String description,

    @NotNull Priority priority,

    @NotNull TestType type,

    @Valid @Size(min = 1) List<@Valid TestStep> steps,

    @Valid Traceability traceability,

    @NotNull Instant createdAt,

    @NotNull Instant updatedAt,

    @Valid List<@Valid Requirement> linkedRequirements
) implements TestArtifact {

  // Compact constructor для валидации и нормализации
  public TestCase {
    if (createdAt.isAfter(updatedAt)) {
      throw new IllegalArgumentException("createdAt cannot be after updatedAt");
    }
    if (steps.isEmpty()) {
      throw new IllegalArgumentException("TestCase must have at least one step");
    }
    // Нормализация: убираем дубликаты требований по ID
    linkedRequirements = linkedRequirements.stream()
        .distinct()
        .toList();
  }

  // Factory method с авто-генерацией ID и временных меток
  public static TestCase.Builder builder() {
    return new Builder();
  }

  // Fluent Builder для удобного создания
  public static class Builder {
    private UUID id = UUID.randomUUID();
    private String name;
    private String description = "";
    private Priority priority = Priority.MEDIUM;
    private TestType type = TestType.FUNCTIONAL;
    private List<TestStep> steps = List.of();
    private Traceability traceability;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    private List<Requirement> linkedRequirements = List.of();

    public Builder name(String name) { this.name = name; return this; }
    public Builder description(String description) { this.description = description; return this; }
    public Builder priority(Priority priority) { this.priority = priority; return this; }
    public Builder type(TestType type) { this.type = type; return this; }
    public Builder steps(List<TestStep> steps) { this.steps = steps; return this; }
    public Builder step(TestStep step) {
      this.steps = List.of(new ArrayList<>(this.steps) {{ add(step); }}.toArray(new TestStep[0]));
      return this;
    }
    public Builder traceability(Traceability traceability) { this.traceability = traceability; return this; }
    public Builder linkedRequirements(List<Requirement> reqs) { this.linkedRequirements = reqs; return this; }

    public TestCase build() {
      return new TestCase(
          id, name, description, priority, type, steps,
          traceability, createdAt, updatedAt, linkedRequirements
      );
    }
  }

  // Utility methods для агентов
  public boolean coversRequirement(String requirementId) {
    return traceability().requirementIds().contains(requirementId);
  }

  public int estimatedExecutionTimeSeconds() {
    return steps.stream()
        .mapToInt(TestStep::estimatedDurationSeconds)
        .sum();
  }
}