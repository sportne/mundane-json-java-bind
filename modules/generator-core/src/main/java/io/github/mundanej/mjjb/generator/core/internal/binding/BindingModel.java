package io.github.mundanej.mjjb.generator.core.internal.binding;

import java.util.Objects;

/** Root binding IR for one generated schema binding. */
public record BindingModel(String packageName, String rootTypeName, ObjectBinding rootObject) {
  public BindingModel {
    Objects.requireNonNull(packageName, "packageName");
    Objects.requireNonNull(rootTypeName, "rootTypeName");
    Objects.requireNonNull(rootObject, "rootObject");
    if (packageName.isBlank()) {
      throw new IllegalArgumentException("packageName must not be blank");
    }
    if (rootTypeName.isBlank()) {
      throw new IllegalArgumentException("rootTypeName must not be blank");
    }
  }
}
