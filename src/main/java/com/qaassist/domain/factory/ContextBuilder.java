// src/main/java/com/qaassist/domain/factory/ContextBuilder.java
package com.qaassist.domain.factory;

import com.qaassist.domain.task.*;
import com.qaassist.domain.common.ExecutionStatus;

import java.util.*;

/**
 * Fluent builder для TaskContext с поддержкой чейнинга.
 */
public class ContextBuilder {

  private final UUID pipelineId;
  private TaskMetadata metadata;
  private final Map<String, Object> contextData = new HashMap<>();
  private final List<TaskContext.PipelineStage> completed = new ArrayList<>();

  private ContextBuilder(UUID pipelineId) {
    this.pipelineId = pipelineId;
  }

  public static ContextBuilder forPipeline(UUID pipelineId) {
    return new ContextBuilder(pipelineId);
  }

  public ContextBuilder withMetadata(TaskMetadata metadata) {
    this.metadata = metadata;
    return this;
  }

  public ContextBuilder putContext(String key, Object value) {
    this.contextData.put(key, value);
    return this;
  }

  public ContextBuilder markStageCompleted(TaskContext.PipelineStage stage) {
    this.completed.add(stage);
    return this;
  }

  public TaskContext build() {
    if (metadata == null) {
      throw new IllegalStateException("TaskMetadata is required");
    }
    return TaskContext.initialize(pipelineId, metadata);
  }
}