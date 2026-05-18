package io.github.mundanej.mjjb.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Immutable generated validation result. */
public record ValidationResult(List<ValidationError> errors) {
  public ValidationResult {
    Objects.requireNonNull(errors, "errors");
    errors = List.copyOf(errors);
  }

  public static ValidationResult valid() {
    return new ValidationResult(List.of());
  }

  public static ValidationResult invalid(List<ValidationError> errors) {
    Objects.requireNonNull(errors, "errors");
    if (errors.isEmpty()) {
      throw new IllegalArgumentException("errors must not be empty");
    }
    return new ValidationResult(errors);
  }

  public static ValidationResult invalid(ValidationError first, ValidationError... rest) {
    Objects.requireNonNull(first, "first");
    Objects.requireNonNull(rest, "rest");
    ArrayList<ValidationError> errors = new ArrayList<>();
    errors.add(first);
    errors.addAll(List.of(rest));
    return invalid(errors);
  }

  public boolean isValid() {
    return errors.isEmpty();
  }
}
