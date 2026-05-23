package io.github.mundanej.mjjb.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ValidationResultTest {
  @Test
  void validFactoryCreatesSuccessfulResultWithoutErrors() {
    ValidationResult result = ValidationResult.valid();

    assertTrue(result.isValid());
    assertEquals(List.of(), result.errors());
  }

  @Test
  void invalidFactoryAcceptsSingleError() {
    ValidationError error = error("MJJBT-001");

    ValidationResult result = ValidationResult.invalid(error);

    assertFalse(result.isValid());
    assertEquals(List.of(error), result.errors());
  }

  @Test
  void invalidFactoryAcceptsMultipleErrors() {
    ValidationError first = error("MJJBT-001");
    ValidationError second = error("MJJBT-002");
    ValidationError third = error("MJJBT-003");

    ValidationResult result = ValidationResult.invalid(first, second, third);

    assertFalse(result.isValid());
    assertEquals(List.of(first, second, third), result.errors());
  }

  @Test
  void invalidListFactoryAcceptsMultipleErrors() {
    ValidationError first = error("MJJBT-001");
    ValidationError second = error("MJJBT-002");

    ValidationResult result = ValidationResult.invalid(List.of(first, second));

    assertFalse(result.isValid());
    assertEquals(List.of(first, second), result.errors());
  }

  @Test
  void invalidListFactoryRejectsNullAndEmptyErrors() {
    assertThrows(
        NullPointerException.class, () -> ValidationResult.invalid((List<ValidationError>) null));
    assertThrows(IllegalArgumentException.class, () -> ValidationResult.invalid(List.of()));
  }

  @Test
  void invalidFactoriesRejectNullErrorElements() {
    ArrayList<ValidationError> errors = new ArrayList<>();
    errors.add(error("MJJBT-001"));
    errors.add(null);

    assertThrows(NullPointerException.class, () -> ValidationResult.invalid(errors));
    assertThrows(
        NullPointerException.class, () -> ValidationResult.invalid((ValidationError) null));
    assertThrows(
        NullPointerException.class,
        () -> ValidationResult.invalid(error("MJJBT-001"), (ValidationError) null));
  }

  @Test
  void invalidFactoryRejectsNullVarargsArray() {
    assertThrows(
        NullPointerException.class,
        () -> ValidationResult.invalid(error("MJJBT-001"), (ValidationError[]) null));
  }

  @Test
  void errorsAreDefensivelyCopiedAndReturnedAsImmutableList() {
    ValidationError first = error("MJJBT-001");
    ValidationError second = error("MJJBT-002");
    ArrayList<ValidationError> source = new ArrayList<>();
    source.add(first);

    ValidationResult result = ValidationResult.invalid(source);
    source.add(second);

    assertEquals(List.of(first), result.errors());
    assertThrows(UnsupportedOperationException.class, () -> result.errors().add(second));
  }

  private static ValidationError error(String code) {
    return ValidationError.of(code, "Broken value.", JsonPath.ROOT);
  }
}
