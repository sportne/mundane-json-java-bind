package io.github.mundanej.mjjb.generator.core.generated;

/** Failure raised by generated-source verification. */
public final class GeneratedSourceVerificationException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public GeneratedSourceVerificationException(String message) {
    super(message);
  }

  public GeneratedSourceVerificationException(String message, Throwable cause) {
    super(message, cause);
  }
}
