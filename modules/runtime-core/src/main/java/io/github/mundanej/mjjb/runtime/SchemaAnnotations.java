package io.github.mundanej.mjjb.runtime;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Generated schema annotation metadata. */
public record SchemaAnnotations(
    Optional<String> title,
    Optional<String> description,
    Optional<String> comment,
    List<String> examplesJson,
    Optional<Boolean> deprecated,
    Optional<Boolean> readOnly,
    Optional<Boolean> writeOnly,
    Optional<String> defaultJson) {
  public static final SchemaAnnotations EMPTY =
      new SchemaAnnotations(
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          List.of(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty());

  public SchemaAnnotations {
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
