package io.github.mundanej.mjjb.runtime;

import java.util.Objects;

/** Best-effort source location for JSON input or output diagnostics. */
public record JsonLocation(String sourceName, long offset, int lineNumber, int columnNumber) {
  public static final JsonLocation UNKNOWN = new JsonLocation("", -1L, -1, -1);

  public JsonLocation {
    Objects.requireNonNull(sourceName, "sourceName");
    if (offset < -1L) {
      throw new IllegalArgumentException("offset must be -1 for unknown or non-negative");
    }
    validateCoordinate("lineNumber", lineNumber);
    validateCoordinate("columnNumber", columnNumber);
  }

  private static void validateCoordinate(String name, int value) {
    if (value == 0 || value < -1) {
      throw new IllegalArgumentException(name + " must be -1 for unknown or greater than zero");
    }
  }
}
