package io.github.mundanej.mjjb.generator.api;

import java.nio.file.Path;
import java.util.Objects;

/** Public immutable generator diagnostic. */
public record GeneratorDiagnostic(
    String code, String message, Path schemaPath, String schemaPointer) {
  public GeneratorDiagnostic {
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(message, "message");
    schemaPointer = schemaPointer == null ? "" : schemaPointer;
    if (code.isBlank()) {
      throw new IllegalArgumentException("code must not be blank");
    }
    if (message.isBlank()) {
      throw new IllegalArgumentException("message must not be blank");
    }
  }

  public String toManifestLine() {
    String path = schemaPath == null ? "<unknown>" : schemaPath.toString();
    return code + " | " + path + "#" + schemaPointer + " | " + message;
  }
}
