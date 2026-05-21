// src/main/java/com/qaassist/agent/api/engine/AllureAnnotationInjector.java
package com.qaassist.agent.api.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Добавляет Allure-аннотации в сгенерированный код для отчётности.
 */
@Slf4j
@Component
public class AllureAnnotationInjector {

  public String injectAllureAnnotations(String javaCode) {
    return javaCode
        .replaceFirst("(?m)^public class", "@Epic(\"API Regression\")\n@Feature(\"%s\")\npublic class")
        .replace("@Test", "@Test\n    @Severity(SeverityLevel.NORMAL)")
        .replace("import org.junit.jupiter.api.*;", """
                import org.junit.jupiter.api.*;
                import io.qameta.allure.*;
                import io.qameta.allure.SeverityLevel;
                """);
  }
}