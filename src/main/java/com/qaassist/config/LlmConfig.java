package com.qaassist.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaassist.properties.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class LlmConfig {

  private final AppProperties properties;

  public LlmConfig(AppProperties properties) {
    this.properties = properties;
  }

  @Bean
  public GeminiClient geminiClient() {
    // Получаем API ключ из переменных окружения для безопасности
    String apiKey = System.getenv("GEMINI_API_KEY");
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("Environment variable GEMINI_API_KEY is not set");
    }

    // Можно передать настройки температуры/токенов из properties, если нужно
    //但目前 GeminiClient использует дефолтные значения внутри себя.
    // При необходимости можно модифицировать конструктор GeminiClient.
    return new GeminiClient(apiKey);
  }

  /**
   * Клиент для работы с Google Gemini API.
   * Реализует логику авто-переключения моделей при ошибках квот или недоступности.
   */
  public static class GeminiClient {

    private final String apiKey;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // Кэш доступных моделей
    private List<String> availableModels = null;
    private String currentModel = null;

    public GeminiClient(String apiKey) {
      this.apiKey = apiKey;
    }

    /**
     * Возвращает список доступных моделей, поддерживающих generateContent с текстом
     */
    public List<String> getAvailableModels() {
      if (availableModels != null) {
        return availableModels;
      }

      String modelsUrl = "https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey;

      try {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(modelsUrl))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
          throw new RuntimeException("Failed to fetch models. Status: " + response.statusCode() + " Body: " + response.body());
        }

        JsonNode root = mapper.readTree(response.body());
        JsonNode models = root.path("models");
        availableModels = new ArrayList<>();

        if (!models.isArray()) {
          return availableModels;
        }

        for (JsonNode model : models) {
          String name = model.path("name").asText();
          if (name.isEmpty()) continue;

          // Исключаем неподходящие модели
          if (name.contains("-tts") || name.contains("embedding") || name.contains("-vision")) {
            continue;
          }

          // Проверяем поддержку generateContent
          JsonNode supportedMethods = model.path("supportedGenerationMethods");
          boolean supportsGenerate = false;

          if (supportedMethods.isArray()) {
            for (JsonNode method : supportedMethods) {
              if ("generateContent".equals(method.asText())) {
                supportsGenerate = true;
                break;
              }
            }
          }

          if (name.startsWith("models/gemini") && supportsGenerate) {
            String modelName = name.replace("models/", "");
            availableModels.add(modelName);
          }
        }

        System.out.println("📋 Available text-generation models: " + availableModels);
        return availableModels;

      } catch (Exception e) {
        throw new RuntimeException("Failed to get model list from Gemini API", e);
      }
    }

    /**
     * Вызов API с конкретной моделью
     */
    private String callWithModel(String prompt, String model) {
      // Исправлен URL: убраны лишние пробелы
      String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

      try {
        // Формируем JSON безопасно через ObjectMapper
        var root = mapper.createObjectNode();
        var contents = root.putArray("contents");
        var contentObj = contents.addObject();
        var parts = contentObj.putArray("parts");
        parts.addObject().put("text", prompt);

        var config = root.putObject("generationConfig");
        // Можно вынести температуру и лимиты в AppProperties, если требуется гибкость
        config.put("temperature", 0.2);
        config.put("topK", 40);
        config.put("topP", 0.95);
        config.put("maxOutputTokens", 8192);

        String body = mapper.writeValueAsString(root);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
          throw new RuntimeException("API error: " + response.statusCode() + " - " + response.body());
        }

        // Извлекаем только текст из ответа
        return extractText(response.body());

      } catch (Exception e) {
        throw new RuntimeException("API call failed for model " + model + ": " + e.getMessage(), e);
      }
    }

    /**
     * Парсит ответ Gemini и возвращает только текстовую часть
     */
    private String extractText(String jsonResponse) {
      try {
        JsonNode root = mapper.readTree(jsonResponse);
        JsonNode candidates = root.path("candidates");

        if (candidates.isArray() && !candidates.isEmpty()) {
          JsonNode content = candidates.get(0).path("content");
          JsonNode parts = content.path("parts");

          if (parts.isArray() && !parts.isEmpty()) {
            return parts.get(0).path("text").asText();
          }
        }
        // Если структура неожиданная, возвращаем сырой JSON для отладки
        return jsonResponse;
      } catch (Exception e) {
        return "Error parsing response: " + e.getMessage() + "\nRaw: " + jsonResponse;
      }
    }

    /**
     * Основной метод для вызова Gemini API с автоматическим переключением моделей
     */
    public String call(String prompt) {
      List<String> models = getAvailableModels();
      if (models.isEmpty()) {
        throw new RuntimeException("No available Gemini models for this API key");
      }

      // Приоритетный порядок (только текстовые модели)
      List<String> priorityOrder = List.of(
          "gemini-2.0-flash",
          "gemini-2.5-flash", // Проверьте актуальность версии
          "gemini-1.5-flash",
          "gemini-2.0-flash-lite",
          "gemini-1.5-flash-lite",
          "gemini-1.5-pro"
      );

      // Формируем список для попыток: сначала приоритетные, затем остальные
      List<String> modelsToTry = new ArrayList<>();
      for (String priority : priorityOrder) {
        if (models.contains(priority) && !modelsToTry.contains(priority)) {
          modelsToTry.add(priority);
        }
      }
      for (String model : models) {
        if (!modelsToTry.contains(model)) {
          modelsToTry.add(model);
        }
      }

      Exception lastException = null;

      for (String model : modelsToTry) {
        try {
          System.out.println("🔄 Trying model: " + model);
          String response = callWithModel(prompt, model);
          currentModel = model;
          System.out.println("✅ Successfully used model: " + currentModel);
          return response;
        } catch (RuntimeException e) {
          lastException = e;
          String errorMsg = e.getMessage();

          if (errorMsg.contains("429") || errorMsg.contains("quota") || errorMsg.contains("RESOURCE_EXHAUSTED")) {
            System.out.println("⚠️ Quota exceeded for " + model + ", switching to next model...");
            continue;
          } else if (errorMsg.contains("404") || errorMsg.contains("not found")) {
            System.out.println("⚠️ Model " + model + " not available, switching to next model...");
            continue;
          } else if (errorMsg.contains("400") && (errorMsg.contains("modalities") || errorMsg.contains("invalid"))) {
            System.out.println("⚠️ Model " + model + " incompatible, switching...");
            continue;
          }

          // Критические ошибки (например, неверный ключ) не пытаемся обойти другой моделью
          if (errorMsg.contains("403") || errorMsg.contains("API_KEY_INVALID")) {
            throw e;
          }
        }
      }

      throw new RuntimeException("All available models exhausted. Last error: " +
          (lastException != null ? lastException.getMessage() : "Unknown"), lastException);
    }
  }
}