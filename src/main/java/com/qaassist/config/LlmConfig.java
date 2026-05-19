// src/main/java/com/qaassist/config/LlmConfig.java
package com.qaassist.config;

import com.qaassist.properties.AppProperties;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class LlmConfig {

  private final AppProperties properties;

  public LlmConfig(AppProperties properties) {
    this.properties = properties;
  }

  @Bean
  public OpenAiChatModel openAiChatModel() {
    var llm = properties.getLlm();

    var openAiApi = OpenAiApi.builder()
        .baseUrl("https://api.openai.com/v1/")
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .build();

    var options = OpenAiChatOptions.builder()
        .model(llm.getModel())
        .temperature(llm.getTemperature())
        .maxTokens(llm.getMaxTokens())
        .build();

    return new OpenAiChatModel(openAiApi, options);
  }

  // Альтернатива: LangChain4j конфигурация
    /*
    @Bean
    public ChatLanguageModel langChainModel() {
        return OpenAiChatModel.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .modelName(properties.getLlm().getModel())
            .temperature(properties.getLlm().getTemperature())
            .maxTokens(properties.getLlm().getMaxTokens())
            .logRequests(true)
            .logResponses(true)
            .build();
    }
    */
}