package io.github.mundanej.mjjb.testkit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjjb.runtime.JsonPath;
import io.github.mundanej.mjjb.runtime.ValidationError;
import io.github.mundanej.mjjb.runtime.ValidationResult;
import org.junit.jupiter.api.Test;

final class GeneratedBindingAssertionsTest {
  @Test
  void acceptsMatchingValidationStates() {
    GeneratedBindingAssertions.assertValid(ValidationResult.valid());
    GeneratedBindingAssertions.assertInvalid(invalidResult());
  }

  @Test
  void rejectsNullResults() {
    NullPointerException validFailure =
        assertThrows(
            NullPointerException.class, () -> GeneratedBindingAssertions.assertValid(null));
    NullPointerException invalidFailure =
        assertThrows(
            NullPointerException.class, () -> GeneratedBindingAssertions.assertInvalid(null));

    assertAll(
        () -> assertEquals("result", validFailure.getMessage()),
        () -> assertEquals("result", invalidFailure.getMessage()));
  }

  @Test
  void assertInvalidFailsForValidResultWithClearMessage() {
    AssertionError failure =
        assertThrows(
            AssertionError.class,
            () -> GeneratedBindingAssertions.assertInvalid(ValidationResult.valid()));

    assertEquals("Expected invalid result.", failure.getMessage());
  }

  @Test
  void assertValidFailsForInvalidResultWithValidationDetails() {
    ValidationResult invalid = invalidResult();

    AssertionError failure =
        assertThrows(AssertionError.class, () -> GeneratedBindingAssertions.assertValid(invalid));

    assertAll(
        () -> assertTrue(failure.getMessage().startsWith("Expected valid result but got ")),
        () -> assertTrue(failure.getMessage().contains("MJJBT-001")),
        () -> assertTrue(failure.getMessage().contains("Broken.")),
        () -> assertTrue(failure.getMessage().contains("$.name")));
  }

  @Test
  void assertValidFailureReportsMultipleErrorCodes() {
    ValidationResult invalid =
        ValidationResult.invalid(
            ValidationError.of("MJJBT-001", "Broken.", JsonPath.ROOT.property("name")),
            ValidationError.of("MJJBT-002", "Still broken.", JsonPath.ROOT.property("age")));

    AssertionError failure =
        assertThrows(AssertionError.class, () -> GeneratedBindingAssertions.assertValid(invalid));

    assertAll(
        () -> assertTrue(failure.getMessage().contains("MJJBT-001")),
        () -> assertTrue(failure.getMessage().contains("MJJBT-002")),
        () -> assertTrue(failure.getMessage().contains("$.name")),
        () -> assertTrue(failure.getMessage().contains("$.age")));
  }

  private static ValidationResult invalidResult() {
    return ValidationResult.invalid(
        ValidationError.of("MJJBT-001", "Broken.", JsonPath.ROOT.property("name")));
  }
}
