package io.github.mundanej.mjjb.runtime;

import java.util.Objects;

/** Stable diagnostic value with JSON instance and schema locations where known. */
public record JsonDiagnostic(
    String code,
    JsonDiagnosticSeverity severity,
    String message,
    JsonPath path,
    JsonLocation location,
    SchemaLocation schemaLocation) {
  public JsonDiagnostic {
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(severity, "severity");
    Objects.requireNonNull(message, "message");
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(location, "location");
    Objects.requireNonNull(schemaLocation, "schemaLocation");
    if (code.isBlank()) {
      throw new IllegalArgumentException("code must not be blank");
    }
    if (message.isBlank()) {
      throw new IllegalArgumentException("message must not be blank");
    }
  }

  public static JsonDiagnostic error(
      String code, String message, JsonPath path, JsonLocation location) {
    return new JsonDiagnostic(
        code, JsonDiagnosticSeverity.ERROR, message, path, location, SchemaLocation.UNKNOWN);
  }
}
