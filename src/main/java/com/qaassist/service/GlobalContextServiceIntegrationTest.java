// src/test/java/com/qaassist/service/GlobalContextServiceIntegrationTest.java
package com.qaassist.service;

import com.qaassist.domain.context.ContextQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class GlobalContextServiceIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired
  private GlobalContextService contextService;

  @TempDir
  Path tempDir;

  @BeforeEach
  void setUp() throws Exception {
    // Создаем структуру файлов контекста
    Path projectDir = tempDir.resolve("test-project");
    Files.createDirectories(projectDir.resolve("endpoints"));
    Files.writeString(projectDir.resolve("endpoints").resolve("api-users.yml"), """
            baseUrl: https://api.example.com/v1
            endpoints:
              GET /users: list users
              POST /users: create user
            """);

    Files.writeString(projectDir.resolve("glossary.md"), """
            # Glossary
            - **Tenant**: isolated workspace for a company
            - **SuperAdmin**: user with system-wide access
            """);

    // Настраиваем путь в свойствах (в реальном тесте можно использовать @MockBean или reflection)
    // Для краткости предполагаем, что AppProperties уже указывает на tempDir
  }

  @Test
  void syncAndRetrieveContextSlice() {
    // Arrange
    String projectId = "test-project";

    // Act
    contextService.syncContext(projectId);
    var slice = contextService.getContextSlice(
        ContextQuery.forCategories(projectId, java.util.List.of("endpoints", "glossary"))
    );

    // Assert
    assertThat(slice.entries()).hasSize(2);
    assertThat(slice.formattingTemplate()).contains("api-users");
    assertThat(slice.formattingTemplate()).contains("Glossary");
    assertThat(slice.truncated()).isFalse();
    assertThat(slice.totalChars()).isGreaterThan(0);
  }

  @Test
  void truncatesWhenOverLimit() {
    String projectId = "test-project";
    contextService.syncContext(projectId);

    var slice = contextService.getContextSlice(
        new ContextQuery(projectId, java.util.List.of("endpoints"), java.util.Optional.empty(), java.util.Optional.of(50), true)
    );

    assertThat(slice.truncated()).isTrue();
    assertThat(slice.entries()).hasSize(1); // только первая запись влезет
  }
}