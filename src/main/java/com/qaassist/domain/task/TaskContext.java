// src/main/java/com/qaassist/domain/task/TaskContext.java
package com.qaassist.domain.task;

import com.qaassist.domain.artifact.TestSuite;
import com.qaassist.domain.requirement.UserStory;
import com.qaassist.domain.common.ExecutionStatus;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Контекст выполнения пайплайна для одной задачи.
 * Передаётся между агентами, содержит все необходимые данные.
 */
public record TaskContext(
    UUID pipelineId,
    TaskMetadata taskMetadata,
    @Valid UserStory decomposedStory,
    Map<String, Object> globalContextSlice,  // подмножество глобального контекста
    ExecutionStatus status,
    List<PipelineStage> completedStages,
    TestSuite generatedTests,
    List<ProcessingError> errors,
    Instant startedAt,
    Instant updatedAt
) {
  // Compact constructor для инициализации по умолчанию
  public TaskContext {
    if (completedStages == null) completedStages = List.of();
    if (errors == null) errors = List.of();
    if (globalContextSlice == null) globalContextSlice = Map.of();
  }

  // Factory: начальный контекст для новой задачи
  public static TaskContext initialize(UUID pipelineId, TaskMetadata metadata) {
    return new TaskContext(
        pipelineId,
        metadata,
        null,  // UserStory будет добавлен после декомпозиции
        Map.of(),
        ExecutionStatus.IN_PROGRESS,
        List.of(),
        null,
        List.of(),
        Instant.now(),
        Instant.now()
    );
  }

  // Immutable update methods (copy-with)
  public TaskContext withStory(UserStory story) {
    return new TaskContext(pipelineId, taskMetadata, story, globalContextSlice,
        status, completedStages, generatedTests, errors, startedAt, Instant.now());
  }

  public TaskContext withStageCompleted(PipelineStage stage) {
    var newStages = new java.util.ArrayList<>(completedStages);
    newStages.add(stage);
    return new TaskContext(pipelineId, taskMetadata, decomposedStory, globalContextSlice,
        status, newStages, generatedTests, errors, startedAt, Instant.now());
  }

  public TaskContext withTests(TestSuite tests) {
    return new TaskContext(pipelineId, taskMetadata, decomposedStory, globalContextSlice,
        status, completedStages, tests, errors, startedAt, Instant.now());
  }

  public TaskContext withError(ProcessingError error) {
    var newErrors = new java.util.ArrayList<>(errors);
    newErrors.add(error);
    return new TaskContext(pipelineId, taskMetadata, decomposedStory, globalContextSlice,
        ExecutionStatus.FAILED, completedStages, generatedTests, newErrors, startedAt, Instant.now());
  }

  public boolean canProceedTo(PipelineStage nextStage) {
    // Простая логика: нельзя перейти к этапу, если есть ошибки
    return errors.isEmpty() && !completedStages.contains(nextStage);
  }

  // Вложенные records
  public enum PipelineStage {
    FETCH_REQUIREMENTS,
    DECOMPOSE_STORY,
    GENERATE_SCENARIOS,
    ENRICH_WITH_DATA,
    FIX_DISCREPANCIES,
    COLLECT_SELECTORS,
    GENERATE_API_TESTS,
    GENERATE_UI_TESTS,
    VALIDATE_AUTOMATION,
    UPLOAD_TO_TMS,
    CREATE_MERGE_REQUEST
  }

  public record ProcessingError(
      PipelineStage stage,
      String errorCode,
      String message,
      Throwable cause,
      Instant occurredAt
  ) {
    public static ProcessingInfo of(PipelineStage stage, String code, String msg) {
      return new ProcessingError(stage, code, msg, null, Instant.now());
    }

    public static ProcessingError of(PipelineStage stage, String code, String msg, Throwable cause) {
      return new ProcessingError(stage, code, msg, cause, Instant.now());
    }
  }
}