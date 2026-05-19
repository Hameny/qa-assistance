package com.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validator for JSON schemas, particularly for test suite validation.
 */
public class JsonSchemaValidator {

  private final ObjectMapper objectMapper;
  private final JsonSchemaFactory schemaFactory;

  public JsonSchemaValidator() {
    this.objectMapper = new ObjectMapper();
    this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
  }

  /**
   * Validates JSON content against a schema from the classpath.
   *
   * @param jsonContent   The JSON content to validate
   * @param schemaPath    Path to the schema file in classpath (e.g., "/schema/test-suite.schema.json")
   * @return              ValidationResult containing success status and any error messages
   */
  public ValidationResult validate(String jsonContent, String schemaPath) {
    try {
      JsonNode jsonNode = objectMapper.readTree(jsonContent);

      try (InputStream schemaStream = getClass().getResourceAsStream(schemaPath)) {
        if (schemaStream == null) {
          return ValidationResult.error("Schema not found: " + schemaPath);
        }

        JsonSchema schema = schemaFactory.getSchema(schemaStream);
        Set<ValidationMessage> messages = schema.validate(jsonNode);

        if (messages.isEmpty()) {
          return ValidationResult.success();
        } else {
          String errors = messages.stream()
              .map(ValidationMessage::getMessage)
              .collect(Collectors.joining("; "));
          return ValidationResult.error(errors);
        }
      }
    } catch (Exception e) {
      return ValidationResult.error("Validation failed: " + e.getMessage());
    }
  }

  /**
   * Validates JSON content against a schema string.
   *
   * @param jsonContent   The JSON content to validate
   * @param schemaContent The schema content as a string
   * @return              ValidationResult containing success status and any error messages
   */
  public ValidationResult validateWithSchema(String jsonContent, String schemaContent) {
    try {
      JsonNode jsonNode = objectMapper.readTree(jsonContent);
      JsonSchema schema = schemaFactory.getSchema(schemaContent);
      Set<ValidationMessage> messages = schema.validate(jsonNode);

      if (messages.isEmpty()) {
        return ValidationResult.success();
      } else {
        String errors = messages.stream()
            .map(ValidationMessage::getMessage)
            .collect(Collectors.joining("; "));
        return ValidationResult.error(errors);
      }
    } catch (Exception e) {
      return ValidationResult.error("Validation failed: " + e.getMessage());
    }
  }

  /**
   * Result of a validation operation.
   */
  public static class ValidationResult {
    private final boolean valid;
    private final String errorMessage;

    private ValidationResult(boolean valid, String errorMessage) {
      this.valid = valid;
      this.errorMessage = errorMessage;
    }

    public static ValidationResult success() {
      return new ValidationResult(true, null);
    }

    public static ValidationResult error(String message) {
      return new ValidationResult(false, message);
    }

    public boolean isValid() {
      return valid;
    }

    public String getErrorMessage() {
      return errorMessage;
    }
  }
}