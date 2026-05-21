// src/main/java/com/qaassist/agent/api/model/ApiTestStructure.java
package com.qaassist.agent.api.model;

import java.util.List;
import java.util.Map;

/**
 * Промежуточная структура для генерации REST Assured кода.
 * Отделяет бизнес-логику сценариев от синтаксиса Java.
 */
public record ApiTestStructure(
    String packageName,
    String testClassName,
    String baseUrl,
    List<ApiTestMethod> methods,
    ApiTestFixture fixture,
    Map<String, String> imports
) {
  public record ApiTestMethod(
      String methodName,
      String description,
      String httpMethod,
      String endpoint,
      String requestBody,
      List<ApiAssertion> assertions,
      List<String> tags,
      int priority
  ) {}

  public record ApiTestFixture(
      List<String> setupSteps,
      List<String> teardownSteps,
      Map<String, String> sharedVariables
  ) {}

  public record ApiAssertion(
      String type,           // "STATUS_CODE", "JSON_PATH", "RESPONSE_TIME", "SCHEMA"
      String expected,
      String actualExpression,
      String customMessage
  ) {}
}