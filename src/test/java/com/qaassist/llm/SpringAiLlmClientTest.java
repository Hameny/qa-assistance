package com.qaassist.llm;

import com.qaassist.config.properties.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import java.util.Map;

import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SpringAiLlmClientTest {

  private ChatModel mockChatModel;
  private AppProperties properties;
  private LlmClientService client;

  @BeforeEach
  void setUp() {
    mockChatModel = mock(ChatModel.class);
    properties = new AppProperties();
    properties.getLlm().setRetry(new AppProperties.RetryProperties());

    // Mock ChatClient behavior
    ChatClient mockClient = mock(ChatClient.class);
    ChatClient.PromptSpec promptSpec = mock(ChatClient.PromptSpec.class);
    ChatClient.PromptSpecResponseSpec responseSpec = mock(ChatClient.PromptSpecResponseSpec.class);

    when(mockClient.prompt()).thenReturn(promptSpec);
    when(promptSpec.system(any())).thenReturn(promptSpec);
    when(promptSpec.user(any())).thenReturn(responseSpec);
    when(responseSpec.call()).thenReturn(responseSpec);
    when(responseSpec.content()).thenReturn("{\"status\":\"ok\"}");

    client = new SpringAiLlmClient(mockChatModel, properties, new ObjectMapper());
  }

  @Test
  @DisplayName("chatAsJson корректно извлекает и парсит JSON")
  void chatAsJsonParsesResponse() {
    record StatusResponse(String status) {}

    var result = client.chatAsJson("You are a JSON bot", "Return status", StatusResponse.class);

    assertThat(result).isNotNull();
    assertThat(result.status()).isEqualTo("ok");
    assertThat(client.getLastTrace().isSuccess()).isTrue();
  }

  @Test
  @DisplayName("extractJson обрабатывает markdown-блоки")
  void extractJsonHandlesMarkdown() {
    String response = """
            Вот ваш JSON:
            ```json
            {
              "users": ["Alice", "Bob"]
            }
            ```
            Надеюсь, поможет!
            """;

    String json = com.qaassist.util.JsonResponseExtractor.extractJson(response);
    assertThat(json).contains("\"users\"");
    assertThat(com.qaassist.util.JsonResponseExtractor.isValidJson(json)).isTrue();
  }

  @Test
  @DisplayName("resolveVariables подставляет контекстные переменные")
  void resolvesContextVariables() {
    // Проверяем через getLastTrace после вызова
    client.chat("System", "User: %name%, Project: %project%", Map.of("name", "QA Bot", "project", "Alpha"));

    assertThat(client.getLastTrace().userPrompt()).contains("QA Bot");
    assertThat(client.getLastTrace().userPrompt()).contains("Alpha");
  }
}