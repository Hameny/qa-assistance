// src/main/java/com/qaassist/config/LlmConfig.java
package com.qaassist.config;

import com.qaassist.properties.AppProperties;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class LlmConfig {

  private final AppProperties properties;

  public LlmConfig(AppProperties properties) {
    this.properties = properties;
  }

  @Bean
  public OpenAiChatModel openAiChatModel(@Value("${spring.ai.openai.api-key:#{environment['OPENAI_API_KEY']}}") String apiKey) {
    var llm = properties.getLlm();
    if (llm == null) {
      throw new IllegalStateException("LLM properties are not configured");
    }

    var openAiApi = OpenAiApi.builder()
        .baseUrl("https://api.openai.com/v1")
        .apiKey(apiKey)
        .build();

    var options = OpenAiChatOptions.builder()
        .model(llm.getModel())
        .temperature(llm.getTemperature())
        .maxTokens(llm.getMaxTokens())
        .build();

    return new OpenAiChatModel(openAiApi, options);
  }
}
