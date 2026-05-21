package com.qaassist.validation.schema;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class JsonSchemaValidator {

  private final SchemaRegistry registry;

  public JsonSchemaValidator(SchemaRegistry registry) {
    this.registry = registry;
  }

  public ValidationResult validate(String schemaId, String json) {
    JsonSchema schema = registry.getSchema(schemaId);
    if (schema == null) throw new IllegalArgumentException("Schema not found: " + schemaId);

    Set<ValidationMessage> errors = schema.validate(json);
    return new ValidationResult(errors.isEmpty(), errors);
  }

  public record ValidationResult(boolean valid, Set<ValidationMessage> errors) {
    public String formatErrors() {
      return errors.stream()
          .map(ValidationMessage::getMessage)
          .reduce((a, b) -> a + "\n" + b)
          .orElse("No errors");
    }
  }
}