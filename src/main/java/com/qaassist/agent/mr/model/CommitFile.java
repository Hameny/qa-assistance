// src/main/java/com/qaassist/agent/mr/model/CommitFile.java
package com.qaassist.agent.mr.model;

/**
 * Файл для коммита в GitLab.
 */
public record CommitFile(
    String filePath,
    String content,
    String encoding,    // "text" или "base64"
    String action       // "create" или "update"
) {
  public CommitFile(String path, String content) {
    this(path, content, "text", "create");
  }
}