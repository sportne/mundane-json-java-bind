package io.github.mundanej.mjjb.schema.model;

import java.util.List;
import java.util.Objects;

/** Immutable result for parsing a JSON Schema document into syntax values. */
public record SchemaSyntaxParseResult(
    SchemaSyntaxValue root, List<SchemaSyntaxDiagnostic> diagnostics) {
  public SchemaSyntaxParseResult {
    diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    if (root == null && diagnostics.isEmpty()) {
      throw new IllegalArgumentException("root is required when diagnostics are empty");
    }
    if (root != null && !diagnostics.isEmpty()) {
      throw new IllegalArgumentException("root must be null when diagnostics are present");
    }
  }

  public static SchemaSyntaxParseResult success(SchemaSyntaxValue root) {
    return new SchemaSyntaxParseResult(Objects.requireNonNull(root, "root"), List.of());
  }

  public static SchemaSyntaxParseResult failure(List<SchemaSyntaxDiagnostic> diagnostics) {
    Objects.requireNonNull(diagnostics, "diagnostics");
    if (diagnostics.isEmpty()) {
      throw new IllegalArgumentException("diagnostics must not be empty");
    }
    return new SchemaSyntaxParseResult(null, diagnostics);
  }

  public boolean successful() {
    return diagnostics.isEmpty();
  }
}
