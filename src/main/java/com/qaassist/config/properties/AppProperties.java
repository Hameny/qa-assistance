// src/main/java/com/qaassist/config/properties/AppProperties.java
package com.qaassist.config.properties;

import com.qaassist.config.validation.ValidLlmProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "qa.assist")
@Validated
@ValidLlmProvider
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

  @Valid
  private DebugProperties debug = new DebugProperties();

  // ================= ВЛОЖЕННЫЕ КОНФИГИ =================

  @Data
  public static class LlmProperties {
    @NotBlank
    private String provider = "openai";
    @NotBlank
    private String model = "gpt-4-turbo";
    @DecimalMin("0.0") @DecimalMax("2.0")
    private Double temperature = 0.1;
    @Positive
    private Integer maxTokens = 4096;
    @Positive
    private Integer timeoutSeconds = 120;

    @Valid
    private RetryProperties retry = new RetryProperties();

    /** Дополнительные HTTP-заголовки для LLM-запросов */
    private Map<String, String> customHeaders = Map.of();

    /** Фича-флаги для экспериментальных промптов */
    private Map<String, Boolean> featureFlags = Map.of(
        "use-structured-output", true,
        "enable-cot-reasoning", false
    );
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
    private String email;
    private String apiToken;
    @NotBlank
    private String projectKey = "DEFAULT";

    /** Поля Jira, которые нужно парсить */
    private List<String> parsedFields = List.of("summary", "description", "attachment", "labels");
  }

  @Data
  public static class GitlabProperties {
    @NotBlank
    private String baseUrl;
    private String token;
    private String projectId;
    private String defaultBranch = "main";
    private String testDirPrefix = "e2e-tests/";
  }

  @Data
  public static class GlobalContextProperties {
    private String basePath = "./global-context";
    @Positive
    private Integer cacheTtlMinutes = 30;
    private List<@NotBlank String> allowedSources = List.of("jira", "confluence");
  }

  @Data
  public static class PipelineProperties {
    @Valid
    private QualityGatesProperties qualityGates = new QualityGatesProperties();
    private boolean autoMode = false;
    @Positive
    private Integer parallelStagesMax = 4;
  }

  @Data
  public static class QualityGatesProperties {
    private boolean requireJsonSchema = true;
    private boolean requireTraceability = true;
    @Min(0) @Max(100)
    private Integer minCoveragePercent = 80;
  }

  @Data
  public static class DebugProperties {
    private boolean enabled = false;
    private boolean dumpLlmResponses = false;
    private boolean mockExternalServices = false;
  }
}