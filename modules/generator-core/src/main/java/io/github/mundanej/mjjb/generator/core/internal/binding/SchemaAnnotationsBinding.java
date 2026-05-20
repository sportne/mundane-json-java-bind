package io.github.mundanej.mjjb.generator.core.internal.binding;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Schema annotation metadata carried into optional generated helpers. */
public record SchemaAnnotationsBinding(
    Optional<String> title,
    Optional<String> description,
    Optional<String> comment,
    List<String> examplesJson,
    Optional<Boolean> deprecated,
    Optional<Boolean> readOnly,
    Optional<Boolean> writeOnly,
    Optional<String> defaultJson) {
  public static final SchemaAnnotationsBinding EMPTY =
      new SchemaAnnotationsBinding(
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          List.of(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty());

  public SchemaAnnotationsBinding {
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(description, "description");
    Objects.requireNonNull(comment, "comment");
    examplesJson = List.copyOf(Objects.requireNonNull(examplesJson, "examplesJson"));
    Objects.requireNonNull(deprecated, "deprecated");
    Objects.requireNonNull(readOnly, "readOnly");
    Objects.requireNonNull(writeOnly, "writeOnly");
    Objects.requireNonNull(defaultJson, "defaultJson");
  }
}
