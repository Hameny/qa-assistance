// src/main/java/com/qaassist/config/PipelineExecutorConfig.java
package com.qaassist.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class PipelineExecutorConfig {

  @Bean(name = "pipelineExecutor")
  public ExecutorService pipelineExecutor() {
    return new ThreadPoolExecutor(
        4, 8, 60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(50),
        r -> new Thread(r, "qa-pipeline-worker"),
        new ThreadPoolExecutor.CallerRunsPolicy() // Backpressure: если очередь полна, выполняем в вызывающем потоке
    );
  }
}