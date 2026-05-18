package io.github.mundanej.mjjb.runtime;

import java.util.Objects;

/** Generated validation failure value. */
public record ValidationError(
    String code,
    String message,
    JsonPath path,
    JsonLocation location,
    SchemaLocation schemaLocation) {
  public ValidationError {
    Objects.requireNonNull(code, "code");
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

  public static ValidationError of(String code, String message, JsonPath path) {
    return new ValidationError(code, message, path, JsonLocation.UNKNOWN, SchemaLocation.UNKNOWN);
  }
}
