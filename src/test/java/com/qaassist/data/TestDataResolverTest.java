// src/test/java/com/qaassist/data/TestDataResolverTest.java
package com.qaassist.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TestDataResolverTest {

  private TestDataResolver resolver;
  private FixtureLoader mockLoader;

  @BeforeEach
  void setUp() {
    mockLoader = mock(FixtureLoader.class);
    when(mockLoader.loadStaticFixtures(any())).thenReturn(Map.of(
        "users", Map.of("admin", Map.of("login", "admin@qa.local", "role", "SUPER")),
        "endpoints", Map.of("base_url", "https://api.test.com")
    ));
    resolver = new TestDataResolver(mockLoader);
  }

  @Test
  @DisplayName("Динамическая генерация UUID и TIMESTAMP")
  void generatesDynamicTokens() {
    String input = "User {{UUID}} at {{TIMESTAMP}}";
    String result = resolver.resolve(input, "PROJ-1");

    assertThat(result).doesNotContain("{{UUID}}");
    assertThat(result).doesNotContain("{{TIMESTAMP}}");
    assertThat(result).matches("User [a-f0-9-]+ at \\d{4}-\\d{2}-\\d{2}T.*");
    assertThat(resolver.getGeneratedValues()).containsKeys("UUID", "TIMESTAMP");
  }

  @Test
  @DisplayName("Загрузка статических фикстур по пути")
  void resolvesStaticFixtures() {
    String input = "Login as {{STATIC:users.admin.login}}";
    String result = resolver.resolve(input, "PROJ-1");

    assertThat(result).isEqualTo("Login as admin@qa.local");
    verify(mockLoader).loadStaticFixtures("PROJ-1");
  }

  @Test
  @DisplayName("REF ссылается на сгенерированное значение")
  void resolvesReferenceToGeneratedValue() {
    String input1 = "Create user {{UUID}}";
    resolver.resolve(input1, "PROJ-1");

    String generatedId = resolver.getGeneratedValues().get("UUID");
    String input2 = "Check status of {{REF:UUID}}";
    String result = resolver.resolve(input2, "PROJ-1");

    assertThat(result).contains(generatedId);
  }

  @Test
  @DisplayName("Изоляция контекста между вызовами clearRunContext")
  void isolatesContextBetweenRuns() {
    resolver.resolve("{{UUID}}", "PROJ-1");
    String firstUuid = resolver.getGeneratedValues().get("UUID");

    resolver.clearRunContext();
    resolver.resolve("{{UUID}}", "PROJ-1");
    String secondUuid = resolver.getGeneratedValues().get("UUID");

    assertThat(firstUuid).isNotEqualTo(secondUuid);
  }
}