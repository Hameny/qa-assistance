package com.qaassist.config.validation;

import com.qaassist.config.properties.AppProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

public class ValidLlmProviderValidator implements ConstraintValidator<ValidLlmProvider, AppProperties.LlmProperties> {

  @Override
  public boolean isValid(AppProperties.LlmProperties llm, ConstraintValidatorContext context) {
    if (llm == null) return true;

    String provider = llm.getProvider().toLowerCase();
    boolean hasKey = StringUtils.hasText(System.getenv("OPENAI_API_KEY")) ||
        StringUtils.hasText(System.getenv("ANTHROPIC_API_KEY"));

    if (!hasKey && ("openai".equals(provider) || "anthropic".equals(provider))) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate(
          "API Key environment variable is missing for provider: " + provider
      ).addConstraintViolation();
      return false;
    }

    if ("anthropic".equals(provider) && !"claude-3-5-sonnet-20240620".equals(llm.getModel())) {
      context.buildConstraintViolationWithTemplate(
          "Anthropic provider strongly recommends claude-3-5-sonnet model"
      ).addConstraintViolation();
      return false;
    }

    return true;
  }
}