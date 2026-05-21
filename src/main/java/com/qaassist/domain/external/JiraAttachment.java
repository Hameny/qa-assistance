// src/main/java/com/qaassist/domain/external/JiraAttachment.java
package com.qaassist.domain.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraAttachment(
    String id,
    String filename,
    String mimeType,
    long size,
    String contentUrl,      // Ссылка для скачивания
    String author,
    Instant created
) {
  public boolean isDocument() {
    return mimeType != null && (
        mimeType.equals("application/pdf") ||
            mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
            mimeType.equals("application/msword") ||
            mimeType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    );
  }
}