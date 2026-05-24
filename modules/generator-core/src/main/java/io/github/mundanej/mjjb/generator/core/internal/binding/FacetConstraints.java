package io.github.mundanej.mjjb.generator.core.internal.binding;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/** Validation facets selected for one scalar value binding. */
public record FacetConstraints(
    OptionalLong minLength,
    OptionalLong maxLength,
    Optional<String> pattern,
    Optional<String> format,
    Optional<String> minimum,
    Optional<String> maximum,
    Optional<String> exclusiveMinimum,
    Optional<String> exclusiveMaximum,
    Optional<String> multipleOf) {
  public static final FacetConstraints EMPTY =
      new FacetConstraints(
          OptionalLong.empty(),
          OptionalLong.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty());

  public FacetConstraints(
      OptionalLong minLength,
      OptionalLong maxLength,
      Optional<String> pattern,
      Optional<String> format,
      Optional<String> minimum,
      Optional<String> maximum,
      Optional<String> exclusiveMinimum,
      Optional<String> exclusiveMaximum) {
    this(
        minLength,
        maxLength,
        pattern,
        format,
        minimum,
        maximum,
        exclusiveMinimum,
        exclusiveMaximum,
        Optional.empty());
  }

  public FacetConstraints {
    Objects.requireNonNull(minLength, "minLength");
    Objects.requireNonNull(maxLength, "maxLength");
    Objects.requireNonNull(pattern, "pattern");
    Objects.requireNonNull(format, "format");
    Objects.requireNonNull(minimum, "minimum");
    Objects.requireNonNull(maximum, "maximum");
    Objects.requireNonNull(exclusiveMinimum, "exclusiveMinimum");
    Objects.requireNonNull(exclusiveMaximum, "exclusiveMaximum");
    Objects.requireNonNull(multipleOf, "multipleOf");
  }

  public boolean hasStringFacets() {
    return minLength.isPresent()
        || maxLength.isPresent()
        || pattern.isPresent()
        || format.isPresent();
  }

  public boolean hasNumericFacets() {
    return minimum.isPresent()
        || maximum.isPresent()
        || exclusiveMinimum.isPresent()
        || exclusiveMaximum.isPresent()
        || multipleOf.isPresent();
  }
}
