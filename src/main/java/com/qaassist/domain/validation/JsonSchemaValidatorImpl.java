// src/main/java/com/qaassist/domain/validation/JsonSchemaValidatorImpl.java
package com.qaassist.domain.validation;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JsonSchemaValidatorImpl implements ConstraintValidator<ValidJsonSchema, String> {

  private static final Map<String, JsonSchema> SCHEMA_CACHE = new ConcurrentHashMap<>();
  private final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
  private String schemaPath;

  @Override
  public void initialize(ValidJsonSchema constraintAnnotation) {
    this.schemaPath = constraintAnnotation.schemaPath();
  }

  @Override
  public boolean isValid(String json, ConstraintValidatorContext context) {
    if (!StringUtils.hasText(json) || !StringUtils.hasText(schemaPath)) return true;

    try {
      JsonSchema schema = SCHEMA_CACHE.computeIfAbsent(schemaPath, path -> {
        try (var stream = new ClassPathResource(path).getInputStream()) {
          return factory.getSchema(stream);
        } catch (IOException e) {
          throw new RuntimeException("Failed to load schema: " + path, e);
        }
      });

      var errors = schema.validate(json);
      if (!errors.isEmpty()) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
            "Schema validation failed: " + errors.iterator().next().getMessage()
        ).addConstraintViolation();
        return false;
      }
      return true;
    } catch (Exception e) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate("Invalid JSON or schema error: " + e.getMessage())
          .addConstraintViolation();
      return false;
    }
  }
}