package io.github.mundanej.mjjb.generator.core.internal.binding;

import java.util.List;
import java.util.Objects;

/** dependentRequired rule for one triggering property. */
public record DependentRequired(String propertyName, List<String> requiredProperties) {
  public DependentRequired {
    Objects.requireNonNull(propertyName, "propertyName");
    requiredProperties =
        List.copyOf(Objects.requireNonNull(requiredProperties, "requiredProperties"));
  }
}
