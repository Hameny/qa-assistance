package com.qaassist.prompt.model;

/**
 * Готовый к отправке промпт с метаданными.
 */
public record RenderedPrompt(
    String templateId,
    String templateVersion,
    String systemPrompt,
    String userPrompt,
    int maxTokens
) {}