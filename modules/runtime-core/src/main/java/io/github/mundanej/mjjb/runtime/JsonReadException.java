package io.github.mundanej.mjjb.runtime;

import java.util.Objects;

/** Exception thrown for JSON parse or generated-reader failures. */
public final class JsonReadException extends Exception {
  private static final long serialVersionUID = 1L;

  private final transient JsonDiagnostic diagnostic;

  public JsonReadException(JsonDiagnostic diagnostic) {
    super(Objects.requireNonNull(diagnostic, "diagnostic").message());
    this.diagnostic = diagnostic;
  }

  public JsonDiagnostic diagnostic() {
    return diagnostic;
  }
}
