// src/test/java/com/qaassist/config/TestPlaywrightConfig.java
package com.qaassist.config;

import com.microsoft.playwright.Playwright;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestPlaywrightConfig {

  @Bean
  public Playwright playwright() {
    // Для CI: использовать headless + указать путь к браузеру
    return Playwright.create();
  }
}