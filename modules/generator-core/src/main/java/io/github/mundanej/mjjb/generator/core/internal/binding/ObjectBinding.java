package io.github.mundanej.mjjb.generator.core.internal.binding;

import io.github.mundanej.mjjb.schema.model.JsonPointer;
import java.util.List;
import java.util.Objects;

/** Binding IR for an object schema. */
public record ObjectBinding(
    String javaTypeName,
    JsonPointer schemaPointer,
    List<FieldBinding> fields,
    SchemaAnnotationsBinding annotations) {
  public ObjectBinding(String javaTypeName, JsonPointer schemaPointer, List<FieldBinding> fields) {
    this(javaTypeName, schemaPointer, fields, SchemaAnnotationsBinding.EMPTY);
  }

  public ObjectBinding {
    Objects.requireNonNull(javaTypeName, "javaTypeName");
    Objects.requireNonNull(schemaPointer, "schemaPointer");
    fields = List.copyOf(Objects.requireNonNull(fields, "fields"));
    Objects.requireNonNull(annotations, "annotations");
    if (javaTypeName.isBlank()) {
      throw new IllegalArgumentException("javaTypeName must not be blank");
    }
  }
}
