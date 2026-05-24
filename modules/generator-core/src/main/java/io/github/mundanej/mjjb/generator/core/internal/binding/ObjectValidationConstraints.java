package io.github.mundanej.mjjb.generator.core.internal.binding;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/** Object-level validation constraints selected for generated validators. */
public record ObjectValidationConstraints(
    OptionalLong minProperties,
    OptionalLong maxProperties,
    Optional<FacetConstraints> propertyNames,
    List<DependentRequired> dependentRequired) {
  public static final ObjectValidationConstraints EMPTY =
      new ObjectValidationConstraints(
          OptionalLong.empty(), OptionalLong.empty(), Optional.empty(), List.of());

  public ObjectValidationConstraints {
    Objects.requireNonNull(minProperties, "minProperties");
    Objects.requireNonNull(maxProperties, "maxProperties");
    Objects.requireNonNull(propertyNames, "propertyNames");
    dependentRequired = List.copyOf(Objects.requireNonNull(dependentRequired, "dependentRequired"));
  }

  public boolean hasConstraints() {
    return minProperties.isPresent()
        || maxProperties.isPresent()
        || propertyNames.isPresent()
        || !dependentRequired.isEmpty();
  }
}
