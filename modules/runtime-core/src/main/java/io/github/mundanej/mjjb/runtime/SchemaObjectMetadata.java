package io.github.mundanej.mjjb.runtime;

import java.util.List;
import java.util.Objects;

/** Generated metadata for one object binding. */
public record SchemaObjectMetadata(
    String schemaPointer,
    String javaTypeName,
    SchemaAnnotations annotations,
    List<SchemaPropertyMetadata> properties) {
  public SchemaObjectMetadata {
    Objects.requireNonNull(schemaPointer, "schemaPointer");
    Objects.requireNonNull(javaTypeName, "javaTypeName");
    Objects.requireNonNull(annotations, "annotations");
    properties = List.copyOf(Objects.requireNonNull(properties, "properties"));
    if (!schemaPointer.isEmpty() && !schemaPointer.startsWith("/")) {
      throw new IllegalArgumentException("schemaPointer must be empty or a JSON Pointer");
    }
    if (javaTypeName.isBlank()) {
      throw new IllegalArgumentException("javaTypeName must not be blank");
    }
  }
}
