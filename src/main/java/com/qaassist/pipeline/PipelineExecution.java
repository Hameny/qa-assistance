// src/main/java/com/qaassist/pipeline/PipelineExecution.java
package com.qaassist.pipeline;

import com.qaassist.agent.api.ApiTestGenerator;
import com.qaassist.agent.mr.model.MrCreationResult;
import com.qaassist.agent.ui.UiAutomationSkill;
import com.qaassist.agent.zephyr.model.ZephyrSyncReport;
import com.qaassist.domain.artifact.TestSuite;
import com.qaassist.domain.requirement.UserStory;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class PipelineExecution {
  UUID pipelineId;
  String projectId;
  String issueKey;
  PipelineStatus status;
  UserStory userStory;
  TestSuite testSuite;
  List<ApiTestGenerator.GeneratedApiTest> apiTests;
  List<UiAutomationSkill.GeneratedUiTest> uiTests;
  ZephyrSyncReport zephyrReport;
  MrCreationResult mrResult;
  List<String> errors;
  Instant startedAt;
  Instant finishedAt;

  public enum PipelineStatus {
    PENDING, IN_PROGRESS, COMPLETED, FAILED, PARTIALLY_COMPLETED
  }

  public PipelineExecution withStatus(PipelineStatus s) {
    return toBuilder().status(s).build();
  }

  public PipelineExecution addError(String err) {
    return toBuilder().errors(List.of(err)).status(PipelineStatus.FAILED).build();
  }

  public static PipelineExecution start(String projectId, String issueKey) {
    return PipelineExecution.builder()
        .pipelineId(UUID.randomUUID())
        .projectId(projectId)
        .issueKey(issueKey)
        .status(PipelineStatus.PENDING)
        .errors(List.of())
        .startedAt(Instant.now())
        .build();
  }
}