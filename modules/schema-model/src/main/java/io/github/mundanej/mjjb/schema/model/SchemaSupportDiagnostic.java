package io.github.mundanej.mjjb.schema.model;

import java.util.Objects;

/** Diagnostic produced while checking a schema against the active generated-binding profile. */
public record SchemaSupportDiagnostic(String code, String message, JsonPointer pointer) {
  public SchemaSupportDiagnostic {
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
