package io.github.mundanej.mjjb.testkit;

import io.github.mundanej.mjjb.runtime.ValidationResult;
import java.util.Objects;

/** Small assertions for generated-binding tests without forcing a test framework on users. */
public final class GeneratedBindingAssertions {
  private GeneratedBindingAssertions() {}

  public static void assertValid(ValidationResult result) {
    Objects.requireNonNull(result, "result");
    if (!result.isValid()) {
      throw new AssertionError("Expected valid result but got " + result.errors());
    }
  }

  public static void assertInvalid(ValidationResult result) {
    Objects.requireNonNull(result, "result");
    if (result.isValid()) {
      throw new AssertionError("Expected invalid result.");
    }
  }
}
