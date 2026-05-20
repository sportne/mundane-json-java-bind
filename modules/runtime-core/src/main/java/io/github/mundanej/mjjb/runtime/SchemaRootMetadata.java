package io.github.mundanej.mjjb.runtime;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Generated metadata for one root schema binding. */
public record SchemaRootMetadata(
    String dialect,
    String rootTypeName,
    SchemaObjectMetadata rootObject,
    Optional<String> tagPropertyName,
    List<SchemaBranchMetadata> branches) {
  public SchemaRootMetadata {
    Objects.requireNonNull(dialect, "dialect");
    Objects.requireNonNull(rootTypeName, "rootTypeName");
    Objects.requireNonNull(rootObject, "rootObject");
    Objects.requireNonNull(tagPropertyName, "tagPropertyName");
    branches = List.copyOf(Objects.requireNonNull(branches, "branches"));
    if (dialect.isBlank()) {
      throw new IllegalArgumentException("dialect must not be blank");
    }
    if (rootTypeName.isBlank()) {
      throw new IllegalArgumentException("rootTypeName must not be blank");
    }
  }
}
