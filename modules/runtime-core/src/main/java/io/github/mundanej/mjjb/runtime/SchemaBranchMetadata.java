package io.github.mundanej.mjjb.runtime;

import java.util.Objects;

/** Generated metadata for one tagged union branch. */
public record SchemaBranchMetadata(
    String tagValue, String javaTypeName, SchemaObjectMetadata object) {
  public SchemaBranchMetadata {
    Objects.requireNonNull(tagValue, "tagValue");
    Objects.requireNonNull(javaTypeName, "javaTypeName");
    Objects.requireNonNull(object, "object");
    if (javaTypeName.isBlank()) {
      throw new IllegalArgumentException("javaTypeName must not be blank");
    }
  }
}
