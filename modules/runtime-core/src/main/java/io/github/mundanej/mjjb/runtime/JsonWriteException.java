package io.github.mundanej.mjjb.runtime;

/** Exception thrown for JSON writer failures. */
public final class JsonWriteException extends Exception {
  private static final long serialVersionUID = 1L;

  public JsonWriteException(String message) {
    super(message);
    if (message == null || message.isBlank()) {
      throw new IllegalArgumentException("message must not be blank");
    }
  }

  public JsonWriteException(String message, Throwable cause) {
    super(message, cause);
    if (message == null || message.isBlank()) {
      throw new IllegalArgumentException("message must not be blank");
    }
  }
}
