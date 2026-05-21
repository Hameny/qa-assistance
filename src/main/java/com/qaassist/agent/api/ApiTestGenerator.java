// src/main/java/com/qaassist/agent/api/ApiTestGenerator.java
package com.qaassist.agent.api;

import com.qaassist.agent.api.engine.RestAssuredTemplateEngine;
import com.qaassist.agent.api.model.ApiTestStructure;
import com.qaassist.agent.api.model.ApiTestStructure.*;
import com.qaassist.data.TestDataResolver;
import com.qaassist.domain.artifact.TestCase;
import com.qaassist.domain.artifact.TestStep;
import com.qaassist.domain.artifact.TestSuite;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiTestGenerator {

  private final RestAssuredTemplateEngine templateEngine;
  private final TestDataResolver dataResolver;

  /**
   * Преобразует TestSuite в готовый REST Assured Java-класс.
   */
  public GeneratedApiTest generate(TestSuite suite, String projectId, String baseUrl) {
    log.info("⚙️ Generating API tests for suite: {}", suite.suiteName());

    // 1. Преобразование доменной модели в структуру генерации
    ApiTestStructure structure = mapToStructure(suite, projectId, baseUrl);

    // 2. Генерация кода
    String javaCode = templateEngine.generateClass(structure);

    // 3. Валидация синтаксиса (базовая проверка)
    validateGeneratedCode(javaCode);

    log.info("✅ Generated {} API test methods for {}", structure.methods().size(), structure.testClassName());
    return new GeneratedApiTest(structure.testClassName(), javaCode, structure.methods().size());
  }

  private ApiTestStructure mapToStructure(TestSuite suite, String projectId, String baseUrl) {
    String packageName = "com.qaassist.tests.api." + projectId.toLowerCase().replace("-", ".");
    String className = suite.suiteName().replaceAll("[^a-zA-Z0-9]", "_") + "Test";

    List<ApiTestMethod> methods = suite.testCases().stream()
        .filter(tc -> tc.steps().stream().anyMatch(TestStep::isApiStep))
        .map(tc -> mapToMethod(tc, projectId))
        .toList();

    return new ApiTestStructure(
        packageName,
        className,
        baseUrl,
        methods,
        new ApiTestFixture(
            List.of("RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();"),
            List.of("// Cleanup test data if needed"),
            Map.of("baseUrl", baseUrl)
        ),
        Map.of("io.restassured.RestAssured", "io.restassured.RestAssured")
    );
  }

  private ApiTestMethod mapToMethod(TestCase tc, String projectId) {
    TestStep apiStep = tc.steps().stream()
        .filter(TestStep::isApiStep)
        .findFirst()
        .orElseThrow();

    TestStep.ApiCall api = apiStep.apiCall().orElseThrow();

    List<ApiAssertion> assertions = apiStep.assertions().stream()
        .map(a -> new ApiAssertion(
            a.type().name(),
            a.expectedValue(),
            a.actualExpression(),
            a.description()
        ))
        .toList();

    // Резолвим данные в запросе/ответе
    String resolvedBody = api.requestBody().map(b -> dataResolver.resolve(b, projectId)).orElse(null);

    return new ApiTestMethod(
        tc.name().replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase(),
        tc.description(),
        api.method(),
        api.endpoint(),
        resolvedBody,
        assertions,
        List.of(tc.type().name().toLowerCase()),
        tc.priority().weight()
    );
  }

  private void validateGeneratedCode(String code) {
    if (code.contains("// TODO") || code.contains("// FIXME")) {
      log.warn("⚠️ Generated code contains unresolved placeholders");
    }
    // В продакшене: компиляция через javax.tools.JavaCompiler или проверка AST
  }

  public record GeneratedApiTest(String className, String sourceCode, int methodCount) {}
}