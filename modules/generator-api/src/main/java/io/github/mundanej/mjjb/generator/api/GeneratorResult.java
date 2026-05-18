package io.github.mundanej.mjjb.generator.api;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Public immutable generator result. */
public record GeneratorResult(List<Path> generatedSources, List<GeneratorDiagnostic> diagnostics) {
  public GeneratorResult {
    Objects.requireNonNull(generatedSources, "generatedSources");
    Objects.requireNonNull(diagnostics, "diagnostics");
    generatedSources = List.copyOf(generatedSources);
    diagnostics = List.copyOf(diagnostics);
  }

  public static GeneratorResult success(List<Path> generatedSources) {
    return new GeneratorResult(generatedSources, List.of());
  }

  public static GeneratorResult failure(List<GeneratorDiagnostic> diagnostics) {
    return new GeneratorResult(List.of(), diagnostics);
  }

  public boolean successful() {
    return diagnostics.isEmpty();
  }
}
