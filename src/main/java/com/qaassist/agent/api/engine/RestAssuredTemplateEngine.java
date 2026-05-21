// src/main/java/com/qaassist/agent/api/engine/RestAssuredTemplateEngine.java
package com.qaassist.agent.api.engine;

import com.qaassist.agent.api.model.ApiTestStructure;
import com.qaassist.agent.api.model.ApiTestStructure.ApiTestMethod;
import com.qaassist.agent.api.model.ApiTestStructure.ApiAssertion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Slf4j
@Component
public class RestAssuredTemplateEngine {

  /**
   * Генерирует полный Java-класс из структуры API-теста.
   */
  public String generateClass(ApiTestStructure structure) {
    return """
            package %s;
            
            %s
            import io.restassured.RestAssured;
            import io.restassured.response.Response;
            import io.restassured.specification.RequestSpecification;
            import org.junit.jupiter.api.*;
            import static io.restassured.RestAssured.given;
            import static org.assertj.core.api.Assertions.assertThat;
            import static org.hamcrest.Matchers.*;
            
            @Tag("api")
            @DisplayName("%s")
            public class %s {
            
                private RequestSpecification spec;
            
                @BeforeEach
                void setUp() {
            %s
                    spec = given()
                        .baseUri("%s")
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json");
                }
            
                @AfterEach
                void tearDown() {
            %s
                }
            
            %s
            }
            """.formatted(
        structure.packageName(),
        structure.imports().values().stream().collect(Collectors.joining("\n")),
        structure.testClassName().replace("Test", ""),
        structure.testClassName(),
        indent(structure.fixture().setupSteps(), 8),
        structure.baseUrl(),
        indent(structure.fixture().teardownSteps(), 8),
        structure.methods().stream().map(this::generateMethod).collect(Collectors.joining("\n"))
    );
  }

  private String generateMethod(ApiTestMethod method) {
    String httpMethod = method.httpMethod().toUpperCase();
    String requestSpec = method.requestBody() != null
        ? "            .body(\"\"\"\n" + indentBody(method.requestBody(), 12) + "\n            \"\"\")\n"
        : "";

    return """
                @Test
                @Order(%d)
                @DisplayName("%s")
                %s
                void %s() {
            %s
                    Response response = given(spec)
            %s                        .%s("%s");
            
            %s
                }
            """.formatted(
        method.priority(),
        method.description(),
        method.tags().stream().map(t -> "@Tag(\"%s\")".formatted(t)).collect(Collectors.joining("\n                ")),
        method.methodName(),
        indent(method.description().startsWith("[BVA]") ? "@Disabled(\"Boundary test - requires dynamic data\")" : "", 8),
        requestSpec,
        httpMethod.toLowerCase(),
        method.endpoint(),
        indent(buildAssertions(method.assertions()), 8)
    );
  }

  private String buildAssertions(List<ApiAssertion> assertions) {
    return assertions.stream().map(a -> switch (a.type()) {
      case "STATUS_CODE" -> "response.then().statusCode(%s);".formatted(a.expected());
      case "JSON_PATH" -> "response.then().body(\"%s\", %s(\"%s\"));"
          .formatted(a.actualExpression(), a.expected().contains("null") ? "nullValue()" : "equalTo(\"%s\")".formatted(a.expected()), a.expected());
      case "RESPONSE_TIME" -> "response.then().time(lessThan(%sL));".formatted(a.expected());
      case "SCHEMA" -> "// Schema validation: %s\nresponse.then().assertThat().matchesJsonSchemaInClasspath(\"%s\");".formatted(a.customMessage(), a.expected());
      default -> "// Custom assertion: %s".formatted(a.type());
    }).collect(Collectors.joining("\n            "));
  }

  private String indent(List<String> lines, int spaces) {
    String pad = " ".repeat(spaces);
    return lines.stream().map(l -> pad + l + ";").collect(Collectors.joining("\n"));
  }

  private String indentBody(String body, int spaces) {
    String pad = " ".repeat(spaces);
    return body.lines().map(l -> pad + l).collect(Collectors.joining("\n"));
  }
}