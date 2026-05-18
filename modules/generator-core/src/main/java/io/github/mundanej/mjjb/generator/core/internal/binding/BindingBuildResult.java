package io.github.mundanej.mjjb.generator.core.internal.binding;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Result of building generator binding IR. */
public record BindingBuildResult(
    Optional<BindingModel> model, List<BindingDiagnostic> diagnostics) {
  public BindingBuildResult {
    Objects.requireNonNull(model, "model");
    Objects.requireNonNull(diagnostics, "diagnostics");
    diagnostics = List.copyOf(diagnostics);
    if (model.isPresent() != diagnostics.isEmpty()) {
      throw new IllegalArgumentException("model and diagnostics are mutually exclusive");
    }
  }

  public static BindingBuildResult success(BindingModel model) {
    return new BindingBuildResult(Optional.of(model), List.of());
  }

  public static BindingBuildResult failure(List<BindingDiagnostic> diagnostics) {
    return new BindingBuildResult(Optional.empty(), diagnostics);
  }
}
