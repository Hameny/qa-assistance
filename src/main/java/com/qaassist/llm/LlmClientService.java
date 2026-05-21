package com.qaassist.llm;

import com.qaassist.config.properties.AppProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Единый контракт для взаимодействия с LLM.
 * Скрывает детали провайдера, добавляет retry, валидацию JSON и трассировку.
 */
@Service
public interface LlmClientService {

  /**
   * Базовый чат с промптом и контекстными переменными.
   */
  String chat(String systemPrompt, String userPrompt, Map<String, Object> contextVars);

  /**
   * Чат с гарантированным JSON-ответом.
   * Автоматически извлекает JSON, валидирует и десериализует.
   * При ошибке парсинга — автоматически повторяет запрос (до N раз).
   */
  <T> T chatAsJson(String systemPrompt, String userPrompt, Class<T> responseType);

  /**
   * Возвращает трассировку последнего выполненного запроса.
   */
  PromptTrace getLastTrace();

  /**
   * Сброс состояния трассировки (для тестов/очистки).
   */
  void clearTrace();
}