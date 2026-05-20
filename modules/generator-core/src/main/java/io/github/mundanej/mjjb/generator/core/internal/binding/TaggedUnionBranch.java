package io.github.mundanej.mjjb.generator.core.internal.binding;

import java.util.Objects;

/** One branch of the supported root-level tagged oneOf shape. */
public record TaggedUnionBranch(String tagValue, ObjectBinding object) {
  public TaggedUnionBranch {
    Objects.requireNonNull(tagValue, "tagValue");
    Objects.requireNonNull(object, "object");
  }
}
