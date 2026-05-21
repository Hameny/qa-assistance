// src/main/java/com/qaassist/agent/zephyr/model/ZephyrSyncReport.java
package com.qaassist.agent.zephyr.model;

import java.util.List;
import java.util.UUID;

public record ZephyrSyncReport(
    String projectId,
    String projectKey,
    int totalProcessed,
    int created,
    int updated,
    int skipped,
    int failed,
    List<SyncError> errors,
    java.time.Instant completedAt
) {
  public boolean isSuccessful() { return failed == 0; }
  public double successRate() { return totalProcessed > 0 ? (1.0 - (double)failed/totalProcessed) * 100 : 0; }

  public record SyncError(String testCaseId, String testCaseName, String errorMessage, String zephyrResponse) {}
}