package io.github.mundanej.mjjb.generator.core.internal.binding;

import io.github.mundanej.mjjb.schema.model.JsonPointer;
import java.util.Objects;

/** Binding IR for one object property. */
public record FieldBinding(
    String jsonPropertyName,
    String javaFieldName,
    JavaScalarType scalarType,
    boolean required,
    JsonPointer schemaPointer) {
  public FieldBinding {
    Objects.requireNonNull(jsonPropertyName, "jsonPropertyName");
    Objects.requireNonNull(javaFieldName, "javaFieldName");
    Objects.requireNonNull(scalarType, "scalarType");
    Objects.requireNonNull(schemaPointer, "schemaPointer");
    if (javaFieldName.isBlank()) {
      throw new IllegalArgumentException("javaFieldName must not be blank");
    }
  }
}
