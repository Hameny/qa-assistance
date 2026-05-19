// src/main/java/com/qaassist/properties/AppProperties.java
package com.qaassist.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "qa-assist")
@Validated
public class AppProperties {

  @Valid
  private LlmProperties llm = new LlmProperties();

  @Valid
  private JiraProperties jira = new JiraProperties();

  @Valid
  private GitlabProperties gitlab = new GitlabProperties();

  @Valid
  private GlobalContextProperties globalContext = new GlobalContextProperties();

  @Valid
  private PipelineProperties pipeline = new PipelineProperties();

  @Data
  public static class LlmProperties {
    @NotBlank
    private String provider = "openai";
    @NotBlank
    private String model = "gpt-4-turbo";
    private Double temperature = 0.1;
    @Positive
    private Integer maxTokens = 4096;
    @Positive
    private Integer timeoutSeconds = 120;

    @Valid
    private RetryProperties retry = new RetryProperties();
  }

  @Data
  public static class RetryProperties {
    @Positive
    private Integer maxAttempts = 3;
    @Positive
    private Long backoffMs = 1000L;
  }

  @Data
  public static class JiraProperties {
    @NotBlank
    private String baseUrl;
    private String apiToken;
    private String email;
  }

  @Data
  public static class GitlabProperties {
    @NotBlank
    private String baseUrl;
    private String token;
  }

  @Data
  public static class GlobalContextProperties {
    private String basePath = "./global-context";
    @Positive
    private Integer cacheTtlMinutes = 30;
  }

  @Data
  public static class PipelineProperties {
    @Valid
    private QualityGatesProperties qualityGates = new QualityGatesProperties();
    private boolean autoMode = false;
  }

  @Data
  public static class QualityGatesProperties {
    private boolean requireJsonSchema = true;
    private boolean requireTraceability = true;
    @Positive
    private Integer minCoveragePercent = 80;
  }
}