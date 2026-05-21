// src/main/java/com/qaassist/domain/external/JiraLink.java
package com.qaassist.domain.external;

public record JiraLink(
    String issueKey,
    String relationship,   // "blocks", "is blocked by", "relates to", etc.
    String summary
) {}