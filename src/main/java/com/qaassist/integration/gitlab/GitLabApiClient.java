// src/main/java/com/qaassist/integration/gitlab/GitLabApiClient.java
package com.qaassist.integration.gitlab;

import com.qaassist.agent.mr.model.CommitFile;
import com.qaassist.config.properties.GitlabProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitLabApiClient {

  private final WebClient gitlabWebClient; // Бин из Части 1
  private final GitlabProperties props;
  private final ObjectMapper objectMapper;

  /**
   * Проверяет существование ветки.
   */
  public boolean branchExists(String projectId, String branchName) {
    try {
      gitlabWebClient.get()
          .uri("/api/v4/projects/{id}/repository/branches/{branch}",
              urlEncode(projectId), urlEncode(branchName))
          .retrieve()
          .toBodilessEntity()
          .block();
      return true;
    } catch (WebClientResponseException.NotFound e) {
      return false;
    } catch (Exception e) {
      log.error("❌ Failed to check branch existence: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Создаёт ветку от sourceBranch.
   */
  @Retryable(retryFor = WebClientResponseException.class, maxAttempts = 3,
      backoff = @Backoff(delay = 1000, multiplier = 2))
  public void createBranch(String projectId, String branchName, String sourceBranch) {
    log.info("🌿 Creating branch: {} from {}", branchName, sourceBranch);
    gitlabWebClient.post()
        .uri("/api/v4/projects/{id}/repository/branches", urlEncode(projectId))
        .bodyValue(Map.of("branch", branchName, "ref", sourceBranch))
        .retrieve()
        .toBodilessEntity()
        .block();
  }

  /**
   * Коммитит файлы в ветку.
   */
  @Retryable(retryFor = WebClientResponseException.class, maxAttempts = 3,
      backoff = @Backoff(delay = 2000, multiplier = 2))
  public void commitFiles(String projectId, String branchName, List<CommitFile> files, String message) {
    log.info("📦 Committing {} files to {}", files.size(), branchName);

    var actions = files.stream().map(f -> Map.of(
        "action", f.action(),
        "file_path", f.filePath(),
        "content", f.content(),
        "encoding", f.encoding()
    )).toList();

    gitlabWebClient.post()
        .uri("/api/v4/projects/{id}/repository/commits", urlEncode(projectId))
        .bodyValue(Map.of(
            "branch", branchName,
            "commit_message", message,
            "actions", actions
        ))
        .retrieve()
        .toBodilessEntity()
        .block();
  }

  /**
   * Создаёт Merge Request.
   */
  @Retryable(retryFor = WebClientResponseException.class, maxAttempts = 3,
      backoff = @Backoff(delay = 1500, multiplier = 2))
  public GitlabMrResponse createMergeRequest(String projectId, String sourceBranch,
      String targetBranch, String title, String description) {
    log.info("🔀 Creating MR: {} → {}", sourceBranch, targetBranch);

    JsonNode response = gitlabWebClient.post()
        .uri("/api/v4/projects/{id}/merge_requests", urlEncode(projectId))
        .bodyValue(Map.of(
            "source_branch", sourceBranch,
            "target_branch", targetBranch,
            "title", title,
            "description", description,
            "remove_source_branch", true
        ))
        .retrieve()
        .bodyToMono(JsonNode.class)
        .block();

    return new GitlabMrResponse(
        response.path("web_url").asText(),
        response.path("iid").asText(),
        response.path("state").asText()
    );
  }

  /**
   * Ищет существующий открытый MR по source_branch.
   */
  public Optional<GitlabMrResponse> findExistingMr(String projectId, String sourceBranch) {
    try {
      JsonNode arr = gitlabWebClient.get()
          .uri(uriBuilder -> uriBuilder
              .path("/api/v4/projects/{id}/merge_requests")
              .queryParam("state", "opened")
              .queryParam("source_branch", sourceBranch)
              .build(urlEncode(projectId)))
          .retrieve()
          .bodyToMono(JsonNode.class)
          .block();

      if (arr.isArray() && arr.size() > 0) {
        return Optional.of(new GitlabMrResponse(
            arr.get(0).path("web_url").asText(),
            arr.get(0).path("iid").asText(),
            "opened"
        ));
      }
    } catch (Exception e) {
      log.debug("No existing MR found for {}: {}", sourceBranch, e.getMessage());
    }
    return Optional.empty();
  }

  private String urlEncode(String s) {
    return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
  }

  public record GitlabMrResponse(String webUrl, String iid, String state) {}
}