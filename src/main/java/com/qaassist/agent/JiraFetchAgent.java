package com.qaassist.agent;

import com.qaassist.domain.external.JiraAttachment;
import com.qaassist.domain.external.JiraIssue;
import com.qaassist.domain.task.TaskContext;
import com.qaassist.domain.task.TaskMetadata;
import com.qaassist.integration.jira.AttachmentParser;
import com.qaassist.integration.jira.JiraApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JiraFetchAgent {

  private final JiraApiClient jiraClient;
  private final AttachmentParser attachmentParser;

  /**
   * Загружает задачу из Jira и обогащает контекст сырыми требованиями.
   */
  public Mono<TaskContext> fetchAndEnrich(TaskContext context) {
    String issueKey = context.taskMetadata().issueKey();

    return jiraClient.fetchIssue(issueKey)
        .flatMap(issue -> processAttachments(issue)
            .map(parsed -> enrichContext(context, issue, parsed)))
        .onErrorResume(e -> {
          log.error("❌ Failed to fetch {}: {}", issueKey, e.getMessage());
          return Mono.just(context.withError(
              TaskContext.ProcessingError.of(
                  TaskContext.PipelineStage.FETCH_REQUIREMENTS,
                  "JIRA_FETCH_ERROR",
                  e.getMessage()
              )
          ));
        });
  }

  private Mono<List<AttachmentParser.ParsedAttachment>> processAttachments(JiraIssue issue) {
    if (!issue.hasAttachments()) {
      return Mono.just(List.of());
    }

    var futures = issue.attachments().stream()
        .filter(JiraAttachment::isDocument)
        .map(att -> jiraClient.downloadAttachment(att.contentUrl())
            .map(content -> attachmentParser.parse(content, att.filename(), att.mimeType()))
            .onErrorReturn(new AttachmentParser.ParsedAttachment(att.filename(), att.mimeType(), "", true)))
        .toList();

    return Mono.zip(futures, objects -> {
      List<AttachmentParser.ParsedAttachment> result = new ArrayList<>();
      for (Object obj : objects) {
        if (obj instanceof AttachmentParser.ParsedAttachment parsed) {
          result.add(parsed);
        }
      }
      return result;
    });
  }

  private TaskContext enrichContext(
      TaskContext context,
      JiraIssue issue,
      List<AttachmentParser.ParsedAttachment> parsedAttachments
  ) {
    // Формируем объединённый текст требований
    StringBuilder requirements = new StringBuilder();
    requirements.append("SUMMARY: ").append(issue.summary()).append("\n\n");
    if (issue.description() != null) {
      requirements.append("DESCRIPTION:\n").append(issue.description()).append("\n\n");
    }

    // Добавляем контент из вложений
    for (var att : parsedAttachments) {
      if (!att.error() && !att.isEmpty()) {
        requirements.append("ATTACHMENT [")
            .append(att.filename())
            .append("]:\n")
            .append(att.content())
            .append("\n\n");
      }
    }

    // Обновляем метаданные и контекст
    TaskMetadata enrichedMeta = context.taskMetadata().withIssueDetails(
        issue.summary(), issue.priority(), issue.labels(), issue.epicLink()
    );

    return context
        .withMetadata(enrichedMeta)
        .withStageCompleted(TaskContext.PipelineStage.FETCH_REQUIREMENTS)
        .putContext("raw_requirements", requirements.toString())
        .putContext("jira_issue", issue);
  }
}