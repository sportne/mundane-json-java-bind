package io.github.mundanej.mjjb.generator.core.internal.binding;

import java.util.Objects;
import java.util.Optional;

/** Root binding IR for one generated schema binding. */
public record BindingModel(
    String packageName,
    String rootTypeName,
    ObjectBinding rootObject,
    Optional<TaggedUnionBinding> taggedUnion) {
  public BindingModel(String packageName, String rootTypeName, ObjectBinding rootObject) {
    this(packageName, rootTypeName, rootObject, Optional.empty());
  }

  public BindingModel {
    Objects.requireNonNull(packageName, "packageName");
    Objects.requireNonNull(rootTypeName, "rootTypeName");
    Objects.requireNonNull(rootObject, "rootObject");
    Objects.requireNonNull(taggedUnion, "taggedUnion");
    if (packageName.isBlank()) {
      throw new IllegalArgumentException("packageName must not be blank");
    }
    if (rootTypeName.isBlank()) {
      throw new IllegalArgumentException("rootTypeName must not be blank");
    }
  }
}
