// src/main/java/com/qaassist/config/properties/ZephyrProperties.java
package com.qaassist.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties(prefix = "qa.assist.zephyr")
@Validated
public class ZephyrProperties {
  @NotBlank String baseUrl = "https://api.zephyrscale.smartbear.com/v2";
  @NotBlank String token;
  @NotBlank String projectKey;
  String defaultFolder = "Auto-Generated";
  boolean skipExisting = true;
  @Positive int batchSize = 10;
  @Positive int retryMaxAttempts = 3;
  @Positive long retryBackoffMs = 1500;
}