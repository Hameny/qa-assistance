// src/main/java/com/qaassist/agent/RequirementsDecomposerAgent.java
package com.qaassist.agent;

import com.qaassist.domain.requirement.UserStory;
import com.qaassist.llm.LlmClientService;
import com.qaassist.prompt.PromptService;
import com.qaassist.prompt.model.RenderedPrompt;
import com.qaassist.validation.schema.LlmJsonParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequirementsDecomposerAgent {

  private final PromptService promptService;
  private final LlmClientService llmClient;
  private final LlmJsonParserService jsonParser;

  public UserStory decompose(String projectId, String rawRequirements, String contextSlice) {
    log.info("🚀 Starting requirements decomposition for project: {}", projectId);

    RenderedPrompt rendered = promptService.preparePrompt(
        "requirements_decomposition",
        Map.of(
            "PROJECT", projectId,
            "REQUIREMENTS_TEXT", rawRequirements,
            "GLOBAL_CONTEXT_SLICE", contextSlice != null ? contextSlice : "No additional context"
        )
    );

    String rawResponse = llmClient.chat(
        rendered.systemPrompt(),
        rendered.userPrompt(),
        Map.of()
    );

    // ✅ Валидация + автоматический recovery при ошибке
    return jsonParser.parseWithRecovery(
        rawResponse,
        "user-story",          // Имя схемы без .json
        UserStory.class,       // Целевой тип
        rendered.systemPrompt(),
        rendered.userPrompt()
    );
  }
}