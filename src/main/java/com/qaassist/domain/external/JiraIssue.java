package com.qaassist.domain.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Упрощённая модель задачи Jira для внутреннего использования.
 * Не зависит от полной модели Atlassian — только нужные поля.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraIssue(
    String key,                    // PROJ-123
    String summary,
    String description,
    String status,
    String priority,
    String assignee,
    String reporter,
    List<String> labels,
    List<JiraAttachment> attachments,
    Map<String, Object> customFields,  // Для гибкости: customfield_10001 и т.д.
    Instant created,
    Instant updated,
    String epicLink,               // Если задача в эпике
    List<JiraLink> issueLinks      // Связанные задачи (blocks, relates, etc.)
) {
  public boolean hasAttachments() { return attachments != null && !attachments.isEmpty(); }
  public String getEpicKey() { return epicLink; }
}