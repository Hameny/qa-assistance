// src/test/java/com/qaassist/agent/api/ApiTestGeneratorTest.java
package com.qaassist.agent.api;

import com.qaassist.agent.api.model.ApiTestStructure;
import com.qaassist.agent.api.engine.RestAssuredTemplateEngine;
import com.qaassist.data.TestDataResolver;
import com.qaassist.domain.artifact.*;
import com.qaassist.domain.common.Priority;
import com.qaassist.domain.common.TestType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiTestGeneratorTest {

  @Mock private TestDataResolver dataResolver;
  @InjectMocks private ApiTestGenerator generator;

  @Test
  @DisplayName("Генерация валидного REST Assured класса")
  void generatesValidRestAssuredClass() {
    // Arrange
    when(dataResolver.resolve(anyString(), anyString())).thenAnswer(i -> i.getArgument(0));

    TestSuite suite = createMockSuite();
    String baseUrl = "https://api.test.com/v1";

    // Act
    var result = generator.generate(suite, "PROJ-API", baseUrl);

    // Assert
    assertThat(result.className()).endsWith("Test");
    assertThat(result.sourceCode()).contains("package com.qaassist.tests.api.proj.api;");
    assertThat(result.sourceCode()).contains("import io.restassured.RestAssured;");
    assertThat(result.sourceCode()).contains("given(spec)");
    assertThat(result.sourceCode()).contains(".get(\"/users\")");
    assertThat(result.sourceCode()).contains("response.then().statusCode(200);");
    assertThat(result.methodCount()).isEqualTo(1);
  }

  private TestSuite createMockSuite() {
    var apiCall = new TestStep.ApiCall("GET", "/users", java.util.Optional.empty(), java.util.Optional.empty());
    var step = new TestStep(
        UUID.randomUUID(), "Get all users", "Returns 200 OK", 5,
        List.of(new Assertion(UUID.randomUUID(), "Status check", Assertion.AssertionType.EQUALS, "200", "$.status", true)),
        java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.of(apiCall)
    );
    var tc = new TestCase(
        UUID.randomUUID(), "GET /users", "List users endpoint", Priority.HIGH, TestType.FUNCTIONAL,
        List.of(step), null, Instant.now(), Instant.now(), List.of()
    );
    return new TestSuite(UUID.randomUUID(), "UsersApi", "story-1", List.of(tc), Instant.now(), new TestSuite.GenerationMetadata(List.of(), List.of()));
  }
}