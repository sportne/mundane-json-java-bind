package io.github.mundanej.mjjb.generator.core.internal.binding;

import io.github.mundanej.mjjb.schema.model.JsonPointer;
import java.util.Objects;

/** Binding IR for generated map fields. */
public record MapBinding(
    String javaFieldName,
    FieldValueType valueType,
    JsonPointer schemaPointer,
    MapBindingKind kind,
    String pattern) {
  public MapBinding {
    Objects.requireNonNull(javaFieldName, "javaFieldName");
    Objects.requireNonNull(valueType, "valueType");
    Objects.requireNonNull(schemaPointer, "schemaPointer");
    Objects.requireNonNull(kind, "kind");
    if (javaFieldName.isBlank()) {
      throw new IllegalArgumentException("javaFieldName must not be blank");
    }
    if (kind == MapBindingKind.PATTERN_PROPERTIES && (pattern == null || pattern.isBlank())) {
      throw new IllegalArgumentException("pattern must not be blank for patternProperties");
    }
    if (kind == MapBindingKind.ADDITIONAL_PROPERTIES && pattern != null) {
      throw new IllegalArgumentException("additionalProperties maps must not declare a pattern");
    }
  }

  public static MapBinding additionalProperties(
      String javaFieldName, FieldValueType valueType, JsonPointer schemaPointer) {
    return new MapBinding(
        javaFieldName, valueType, schemaPointer, MapBindingKind.ADDITIONAL_PROPERTIES, null);
  }

  public static MapBinding patternProperties(
      String javaFieldName, FieldValueType valueType, JsonPointer schemaPointer, String pattern) {
    return new MapBinding(
        javaFieldName, valueType, schemaPointer, MapBindingKind.PATTERN_PROPERTIES, pattern);
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

  public boolean patternProperties() {
    return kind == MapBindingKind.PATTERN_PROPERTIES;
  }

  public String sourceKeyword() {
    return patternProperties() ? "patternProperties" : "additionalProperties";
  }
}
