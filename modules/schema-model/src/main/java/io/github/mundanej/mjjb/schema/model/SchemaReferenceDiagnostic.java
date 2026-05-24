package io.github.mundanej.mjjb.schema.model;

import java.util.Objects;

/** Diagnostic produced while resolving same-document JSON Schema references. */
public record SchemaReferenceDiagnostic(String code, String message, JsonPointer pointer) {
  public SchemaReferenceDiagnostic {
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(message, "message");
    Objects.requireNonNull(pointer, "pointer");
    if (code.isBlank()) {
      throw new IllegalArgumentException("code must not be blank");
    }
    if (message.isBlank()) {
      throw new IllegalArgumentException("message must not be blank");
    }
  }
}
