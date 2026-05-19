// src/main/java/com/qaassist/QaAssistanceApplication.java
package com.qaassist;

import com.qaassist.properties.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
@EnableCaching
@EnableAsync
public class QaAssistanceApplication {

  public static void main(String[] args) {
    SpringApplication.run(QaAssistanceApplication.class, args);
  }
}