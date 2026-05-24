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
    Optional<MapBinding> patternProperties,
    Optional<MapBinding> additionalProperties,
    ObjectValidationConstraints validationConstraints,
    SchemaAnnotationsBinding annotations) {
  public ObjectBinding(String javaTypeName, JsonPointer schemaPointer, List<FieldBinding> fields) {
    this(
        javaTypeName,
        schemaPointer,
        fields,
        List.of(),
        Optional.empty(),
        Optional.empty(),
        ObjectValidationConstraints.EMPTY,
        SchemaAnnotationsBinding.EMPTY);
  }

  public ObjectBinding(
      String javaTypeName,
      JsonPointer schemaPointer,
      List<FieldBinding> fields,
      SchemaAnnotationsBinding annotations) {
    this(
        javaTypeName,
        schemaPointer,
        fields,
        List.of(),
        Optional.empty(),
        Optional.empty(),
        ObjectValidationConstraints.EMPTY,
        annotations);
  }

  public ObjectBinding(
      String javaTypeName,
      JsonPointer schemaPointer,
      List<FieldBinding> fields,
      Optional<MapBinding> additionalProperties,
      SchemaAnnotationsBinding annotations) {
    this(
        javaTypeName,
        schemaPointer,
        fields,
        List.of(),
        Optional.empty(),
        additionalProperties,
        ObjectValidationConstraints.EMPTY,
        annotations);
  }

  public ObjectBinding(
      String javaTypeName,
      JsonPointer schemaPointer,
      List<FieldBinding> fields,
      Optional<MapBinding> patternProperties,
      Optional<MapBinding> additionalProperties,
      ObjectValidationConstraints validationConstraints,
      SchemaAnnotationsBinding annotations) {
    this(
        javaTypeName,
        schemaPointer,
        fields,
        List.of(),
        patternProperties,
        additionalProperties,
        validationConstraints,
        annotations);
  }

  public ObjectBinding {
    Objects.requireNonNull(javaTypeName, "javaTypeName");
    Objects.requireNonNull(schemaPointer, "schemaPointer");
    fields = List.copyOf(Objects.requireNonNull(fields, "fields"));
    reservedJsonPropertyNames =
        List.copyOf(Objects.requireNonNull(reservedJsonPropertyNames, "reservedJsonPropertyNames"));
    Objects.requireNonNull(patternProperties, "patternProperties");
    Objects.requireNonNull(additionalProperties, "additionalProperties");
    Objects.requireNonNull(validationConstraints, "validationConstraints");
    Objects.requireNonNull(annotations, "annotations");
    if (javaTypeName.isBlank()) {
      throw new IllegalArgumentException("javaTypeName must not be blank");
    }
  }
}
