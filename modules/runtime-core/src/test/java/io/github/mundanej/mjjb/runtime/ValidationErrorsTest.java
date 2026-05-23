package io.github.mundanej.mjjb.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ValidationErrorsTest {
  @Test
  void failFastAddReturnsFalseAndAccumulateAddReturnsTrue() {
    ValidationError error = error("MJJBT-001");

    assertFalse(ValidationErrors.create(ValidationMode.FAIL_FAST).add(error));
    assertTrue(ValidationErrors.create(ValidationMode.ACCUMULATE).add(error));
  }

  @Test
  void emptyAccumulatorConvertsToValidResult() {
    ValidationResult result = ValidationErrors.create(ValidationMode.ACCUMULATE).toResult();

    assertTrue(result.isValid());
    assertEquals(List.of(), result.errors());
  }

  @Test
  void accumulatedErrorsPreserveInsertionOrder() {
    ValidationError first = error("MJJBT-001");
    ValidationError second = error("MJJBT-002");
    ValidationError third = error("MJJBT-003");
    ValidationErrors errors = ValidationErrors.create(ValidationMode.ACCUMULATE);

    errors.add(first);
    errors.add(second);
    errors.add(third);

    assertEquals(List.of(first, second, third), errors.errors());
    assertEquals(List.of(first, second, third), errors.toResult().errors());
  }

  @Test
  void errorsReturnsImmutableSnapshot() {
    ValidationError first = error("MJJBT-001");
    ValidationError second = error("MJJBT-002");
    ValidationErrors errors = ValidationErrors.create(ValidationMode.ACCUMULATE);
    errors.add(first);

    List<ValidationError> snapshot = errors.errors();
    assertThrows(UnsupportedOperationException.class, () -> snapshot.add(second));

    errors.add(second);

    assertEquals(List.of(first), snapshot);
    assertEquals(List.of(first, second), errors.errors());
  }

  @Test
  void toResultReturnsImmutableSnapshot() {
    ValidationError first = error("MJJBT-001");
    ValidationError second = error("MJJBT-002");
    ValidationErrors errors = ValidationErrors.create(ValidationMode.ACCUMULATE);
    errors.add(first);

    ValidationResult result = errors.toResult();
    assertThrows(UnsupportedOperationException.class, () -> result.errors().add(second));

    errors.add(second);

    assertEquals(List.of(first), result.errors());
    assertEquals(List.of(first, second), errors.toResult().errors());
  }

  @Test
  void createRejectsNullMode() {
    assertThrows(NullPointerException.class, () -> ValidationErrors.create(null));
  }

  @Test
  void addRejectsNullErrorWithoutChangingAccumulatedErrors() {
    ValidationErrors errors = ValidationErrors.create(ValidationMode.ACCUMULATE);

    assertThrows(NullPointerException.class, () -> errors.add(null));

    assertEquals(List.of(), errors.errors());
    assertTrue(errors.toResult().isValid());
  }

  private static ValidationError error(String code) {
    return ValidationError.of(code, "Broken value.", JsonPath.ROOT.property(code));
  }
}
