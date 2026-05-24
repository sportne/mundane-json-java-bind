package io.github.mundanej.mjjb.generator.core.internal.binding;

import io.github.mundanej.mjjb.schema.model.JsonPointer;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Binding IR for an object schema. */
public record ObjectBinding(
    String javaTypeName,
    JsonPointer schemaPointer,
    List<FieldBinding> fields,
    List<String> reservedJsonPropertyNames,
    Optional<MapBinding> additionalProperties,
    SchemaAnnotationsBinding annotations) {
  public ObjectBinding(String javaTypeName, JsonPointer schemaPointer, List<FieldBinding> fields) {
    this(
        javaTypeName,
        schemaPointer,
        fields,
        List.of(),
        Optional.empty(),
        SchemaAnnotationsBinding.EMPTY);
  }

  public ObjectBinding(
      String javaTypeName,
      JsonPointer schemaPointer,
      List<FieldBinding> fields,
      SchemaAnnotationsBinding annotations) {
    this(javaTypeName, schemaPointer, fields, List.of(), Optional.empty(), annotations);
  }

  public ObjectBinding(
      String javaTypeName,
      JsonPointer schemaPointer,
      List<FieldBinding> fields,
      Optional<MapBinding> additionalProperties,
      SchemaAnnotationsBinding annotations) {
    this(javaTypeName, schemaPointer, fields, List.of(), additionalProperties, annotations);
  }

  public ObjectBinding {
    Objects.requireNonNull(javaTypeName, "javaTypeName");
    Objects.requireNonNull(schemaPointer, "schemaPointer");
    fields = List.copyOf(Objects.requireNonNull(fields, "fields"));
    reservedJsonPropertyNames =
        List.copyOf(Objects.requireNonNull(reservedJsonPropertyNames, "reservedJsonPropertyNames"));
    Objects.requireNonNull(additionalProperties, "additionalProperties");
    Objects.requireNonNull(annotations, "annotations");
    if (javaTypeName.isBlank()) {
      throw new IllegalArgumentException("javaTypeName must not be blank");
    }
  }
}
