package io.github.mundanej.mjjb.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Small accumulator used by generated validators. */
public final class ValidationErrors {
  private final ValidationMode mode;
  private final ArrayList<ValidationError> errors = new ArrayList<>();

  private ValidationErrors(ValidationMode mode) {
    this.mode = Objects.requireNonNull(mode, "mode");
  }

  public static ValidationErrors create(ValidationMode mode) {
    return new ValidationErrors(mode);
  }

  public boolean add(ValidationError error) {
    errors.add(Objects.requireNonNull(error, "error"));
    return mode == ValidationMode.ACCUMULATE;
  }

  public List<ValidationError> errors() {
    return List.copyOf(errors);
  }

  public ValidationResult toResult() {
    if (errors.isEmpty()) {
      return ValidationResult.valid();
    }
    return ValidationResult.invalid(errors);
  }
}
