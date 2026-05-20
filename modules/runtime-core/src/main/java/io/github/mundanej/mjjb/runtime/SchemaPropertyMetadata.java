package io.github.mundanej.mjjb.runtime;

import java.util.Objects;

/** Generated metadata for one schema property binding. */
public record SchemaPropertyMetadata(
    String jsonName,
    String javaFieldName,
    String schemaPointer,
    boolean required,
    String javaType,
    boolean nullable,
    boolean array,
    SchemaAnnotations annotations) {
  public SchemaPropertyMetadata {
    Objects.requireNonNull(jsonName, "jsonName");
    Objects.requireNonNull(javaFieldName, "javaFieldName");
    Objects.requireNonNull(schemaPointer, "schemaPointer");
    Objects.requireNonNull(javaType, "javaType");
    Objects.requireNonNull(annotations, "annotations");
    if (jsonName.isBlank()) {
      throw new IllegalArgumentException("jsonName must not be blank");
    }
    if (javaFieldName.isBlank()) {
      throw new IllegalArgumentException("javaFieldName must not be blank");
    }
    if (!schemaPointer.isEmpty() && !schemaPointer.startsWith("/")) {
      throw new IllegalArgumentException("schemaPointer must be empty or a JSON Pointer");
    }
    if (javaType.isBlank()) {
      throw new IllegalArgumentException("javaType must not be blank");
    }
  }
}
