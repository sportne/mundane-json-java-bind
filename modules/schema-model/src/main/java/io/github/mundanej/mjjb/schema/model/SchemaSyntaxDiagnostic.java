package io.github.mundanej.mjjb.schema.model;

import java.util.Objects;

/** Diagnostic produced while parsing a JSON Schema document as JSON syntax. */
public record SchemaSyntaxDiagnostic(String code, String message, JsonPointer pointer) {
  public SchemaSyntaxDiagnostic {
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
