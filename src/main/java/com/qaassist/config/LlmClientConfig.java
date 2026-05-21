package com.qaassist.config;

import com.qaassist.config.properties.AppProperties;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LlmClientConfig {

  private final AppProperties properties;

  public LlmClientConfig(AppProperties properties) {
    this.properties = properties;
  }

  @Bean
  @ConditionalOnProperty(name = "qa.assist.llm.provider", havingValue = "openai", matchIfMissing = true)
  public ChatModel openAiChatModel() {
    var llm = properties.getLlm();
    var apiKey = System.getenv("OPENAI_API_KEY");

    var api = OpenAiApi.builder()
        .apiKey(apiKey)
        .build();

    var options = OpenAiChatOptions.builder()
        .model(llm.getModel())
        .temperature(llm.getTemperature())
        .maxTokens(llm.getMaxTokens())
        .build();

    return new OpenAiChatModel(api, options);
  }

  @Bean
  @ConditionalOnProperty(name = "qa.assist.llm.provider", havingValue = "mock")
  public ChatModel mockChatModel() {
    // Заглушка для тестов/разработки без ключей
    return new MockChatModel();
  }

  // Аналогично можно добавить @ConditionalOnProperty для Anthropic, Ollama и т.д.
}