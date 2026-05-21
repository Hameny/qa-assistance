// src/main/java/com/qaassist/config/AppConfig.java
package com.qaassist.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Корневая конфигурация приложения.
 * В нашем проекте логика разнесена по WebClientConfig, CacheConfig, RetryConfig и т.д.
 * Этот класс нужен для глобального сканирования компонентов и включения асинхронности.
 */
@Configuration
@ComponentScan(basePackages = "com.qaassist")
@EnableAsync
@EnableScheduling
public class AppConfig {
  // Дополнительные глобальные бины можно добавлять сюда,
  // но рекомендуется использовать специфичные @Configuration классы.
}