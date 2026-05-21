package com.qaassist.config;

import com.qaassist.config.properties.AppProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "qa.assist.llm.provider=mock",
    "qa.assist.pipeline.quality-gates.min-coverage-percent=75"
})
@EnableConfigurationProperties(AppProperties.class)
class AppPropertiesTest {

  @Autowired
  private AppProperties properties;

  @Test
  @DisplayName("Свойства корректно биндятся из YAML и переопределяются")
  void bindsPropertiesCorrectly() {
    assertThat(properties.getLlm().getProvider()).isEqualTo("mock");
    assertThat(properties.getPipeline().getQualityGates().getMinCoveragePercent()).isEqualTo(75);
    assertThat(properties.getGlobalContext().getAllowedSources()).hasSizeGreaterThan(0);
  }

  @Test
  @DisplayName("Debug-флаги активны в test-профиле")
  void debugFlagsActiveInTestProfile() {
    assertThat(properties.getDebug().isMockExternalServices()).isTrue();
  }

  @Test
  @DisplayName("Feature Flags загружаются в мапу")
  void featureFlagsLoaded() {
    assertThat(properties.getLlm().getFeatureFlags())
        .containsEntry("use-structured-output", true);
  }
}