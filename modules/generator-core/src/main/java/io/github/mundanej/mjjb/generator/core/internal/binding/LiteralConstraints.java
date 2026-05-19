package io.github.mundanej.mjjb.generator.core.internal.binding;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Literal constraints and default annotation selected for one scalar value binding. */
public record LiteralConstraints(
    List<LiteralValue> enumValues,
    Optional<LiteralValue> constValue,
    Optional<LiteralValue> defaultValue) {
  public static final LiteralConstraints EMPTY =
      new LiteralConstraints(List.of(), Optional.empty(), Optional.empty());

  public LiteralConstraints {
    enumValues = List.copyOf(Objects.requireNonNull(enumValues, "enumValues"));
    Objects.requireNonNull(constValue, "constValue");
    Objects.requireNonNull(defaultValue, "defaultValue");
  }

  public boolean hasEnum() {
    return !enumValues.isEmpty();
  }

  public boolean hasConst() {
    return constValue.isPresent();
  }

  public boolean hasDefault() {
    return defaultValue.isPresent();
  }
}
