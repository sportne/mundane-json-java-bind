package io.github.mundanej.mjjb.generator.core.internal.binding;

import io.github.mundanej.mjjb.schema.model.JsonPointer;
import java.util.List;
import java.util.Objects;

/** Binding IR for the supported root-level tagged oneOf shape. */
public record TaggedUnionBinding(
    String tagPropertyName, JsonPointer schemaPointer, List<TaggedUnionBranch> branches) {
  public TaggedUnionBinding {
    Objects.requireNonNull(tagPropertyName, "tagPropertyName");
    Objects.requireNonNull(schemaPointer, "schemaPointer");
    branches = List.copyOf(Objects.requireNonNull(branches, "branches"));
    if (tagPropertyName.isBlank()) {
      throw new IllegalArgumentException("tagPropertyName must not be blank");
    }
    if (branches.isEmpty()) {
      throw new IllegalArgumentException("branches must not be empty");
    }
  }
}
