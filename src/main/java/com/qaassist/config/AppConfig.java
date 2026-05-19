package com.qaassist.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.qaassist.properties.AppProperties;

@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {

}
