// src/main/java/com/qaassist/agent/mr/model/MrCreationResult.java
package com.qaassist.agent.mr.model;

public record MrCreationResult(
    String branchName,
    String mrUrl,
    String mrIid,
    boolean success,
    String errorMessage,
    int filesCommitted
) {
  public static MrCreationResult failure(String branch, String error) {
    return new MrCreationResult(branch, null, null, false, error, 0);
  }
}