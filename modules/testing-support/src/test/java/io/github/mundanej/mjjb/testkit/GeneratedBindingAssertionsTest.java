package io.github.mundanej.mjjb.testkit;

import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjjb.runtime.JsonPath;
import io.github.mundanej.mjjb.runtime.ValidationError;
import io.github.mundanej.mjjb.runtime.ValidationResult;
import org.junit.jupiter.api.Test;

final class GeneratedBindingAssertionsTest {
  @Test
  void assertsValidationStates() {
    GeneratedBindingAssertions.assertValid(ValidationResult.valid());
    ValidationResult invalid =
        ValidationResult.invalid(ValidationError.of("MJJBT-001", "Broken.", JsonPath.ROOT));
    GeneratedBindingAssertions.assertInvalid(invalid);
    assertThrows(AssertionError.class, () -> GeneratedBindingAssertions.assertValid(invalid));
  }
}
