package io.github.mundanej.mjjb.generator.core.internal.binding;

import io.github.mundanej.mjjb.schema.model.JsonPointer;
import java.util.Objects;

/** Binding IR for object-valued additionalProperties. */
public record MapBinding(
    String javaFieldName, FieldValueType valueType, JsonPointer schemaPointer) {
  public MapBinding {
    Objects.requireNonNull(javaFieldName, "javaFieldName");
    Objects.requireNonNull(valueType, "valueType");
    Objects.requireNonNull(schemaPointer, "schemaPointer");
    if (javaFieldName.isBlank()) {
      throw new IllegalArgumentException("javaFieldName must not be blank");
    }
  }

  public boolean object() {
    return valueType.objectBinding().isPresent();
  }

  public boolean array() {
    return valueType.array();
  }

  public JavaScalarType scalarType() {
    return valueType.scalarType();
  }
}
