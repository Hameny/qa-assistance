package com.qaassist.config;

import com.qaassist.config.properties.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry
public class RetryConfig {

  @Bean
  public AppProperties appProperties() {
    return new AppProperties(); // Spring уже создаст этот бин, но @EnableRetry требует явной конфигурации в некоторых версиях
  }
}