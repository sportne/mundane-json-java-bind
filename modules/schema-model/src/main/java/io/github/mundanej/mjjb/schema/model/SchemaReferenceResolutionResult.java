package io.github.mundanej.mjjb.schema.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Result of normalizing same-document JSON Schema references. */
public record SchemaReferenceResolutionResult(
    Optional<SchemaSyntaxValue> root, List<SchemaReferenceDiagnostic> diagnostics) {
  public SchemaReferenceResolutionResult {
    Objects.requireNonNull(root, "root");
    diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
  }

  static SchemaReferenceResolutionResult success(SchemaSyntaxValue root) {
    return new SchemaReferenceResolutionResult(Optional.of(root), List.of());
  }

  static SchemaReferenceResolutionResult failure(List<SchemaReferenceDiagnostic> diagnostics) {
    return new SchemaReferenceResolutionResult(Optional.empty(), diagnostics);
  }
}
