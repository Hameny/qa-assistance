// src/main/java/com/qaassist/domain/task/TaskMetadata.java
package com.qaassist.domain.task;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Метаданные задачи Jira, передаваемые по пайплайну.
 */
public record TaskMetadata(
    String issueKey,
    String projectId,
    String summary,
    String priority,
    List<String> labels,
    String epicLink,
    Map<String, Object> customFields,
    Instant created,
    Instant updated
) {
  public TaskMetadata {
    if (labels == null) labels = List.of();
    if (customFields == null) customFields = Map.of();
  }

  public static TaskMetadata fromIssueKey(String issueKey) {
    String proj = issueKey.contains("-") ? issueKey.split("-")[0] : "UNKNOWN";
    return new TaskMetadata(issueKey, proj, "", "MEDIUM", List.of(), null, Map.of(), Instant.now(), Instant.now());
  }

  public TaskMetadata withIssueDetails(String summary, String priority, List<String> labels, String epicLink) {
    return new TaskMetadata(issueKey, projectId, summary, priority, labels, epicLink, customFields, created, Instant.now());
  }
}