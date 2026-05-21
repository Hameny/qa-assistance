package com.qaassist.config;

import com.qaassist.config.properties.AppProperties;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

  private final AppProperties properties;

  public WebClientConfig(AppProperties properties) {
    this.properties = properties;
  }

  @Bean
  public WebClient jiraWebClient() {
    var jira = properties.getJira();
    var timeout = properties.getLlm().getTimeoutSeconds();

    var httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
        .doOnConnected(conn -> conn
            .addHandlerLast(new ReadTimeoutHandler(timeout, TimeUnit.SECONDS))
            .addHandlerLast(new WriteTimeoutHandler(timeout, TimeUnit.SECONDS)));

    return WebClient.builder()
        .baseUrl(jira.getBaseUrl())
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.AUTHORIZATION,
            "Basic " + encodeBasicAuth(jira.getEmail(), jira.getApiToken()))
        .build();
  }

  @Bean
  public WebClient gitlabWebClient() {
    var gitlab = properties.getGitlab();
    var timeout = properties.getLlm().getTimeoutSeconds();

    var httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
        .doOnConnected(conn -> conn
            .addHandlerLast(new ReadTimeoutHandler(timeout, TimeUnit.SECONDS)));

    return WebClient.builder()
        .baseUrl(gitlab.getBaseUrl())
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader("PRIVATE-TOKEN", gitlab.getToken())
        .build();
  }

  private String encodeBasicAuth(String email, String token) {
    return java.util.Base64.getEncoder()
        .encodeToString((email + ":" + token).getBytes());
  }
}