package io.github.mundanej.mjjb.examples.basicrecord.generated;

import io.github.mundanej.mjjb.runtime.JsonPath;
import io.github.mundanej.mjjb.runtime.ValidationError;
import io.github.mundanej.mjjb.runtime.ValidationErrors;
import io.github.mundanej.mjjb.runtime.ValidationMode;
import io.github.mundanej.mjjb.runtime.ValidationResult;
import java.util.Objects;

public final class BasicRecordJsonValidator {
  private BasicRecordJsonValidator() {}

  public static ValidationResult validate(BasicRecord value) {
    return validate(value, ValidationMode.ACCUMULATE);
  }

  public static ValidationResult validate(BasicRecord value, ValidationMode mode) {
    Objects.requireNonNull(mode, "mode");
    ValidationErrors errors = ValidationErrors.create(mode);
    if (value == null) {
      errors.add(ValidationError.of("MJJBV-001", "Expected generated object value.", JsonPath.ROOT));
      return errors.toResult();
    }
    if (value.id() == null) {
      if (!errors.add(ValidationError.of("MJJBV-002", "Required property 'id' must not be null.", JsonPath.ROOT.property("id")))) {
        return errors.toResult();
      }
    }
    if (value.displayName() == null) {
      if (!errors.add(ValidationError.of("MJJBV-003", "Optional property container 'displayName' must not be null.", JsonPath.ROOT.property("displayName")))) {
        return errors.toResult();
      }
    }
    if (value.score() == null) {
      if (!errors.add(ValidationError.of("MJJBV-003", "Optional property container 'score' must not be null.", JsonPath.ROOT.property("score")))) {
        return errors.toResult();
      }
    }
    if (value.score() != null && value.score().isPresent()) {
      if (!validateFinite(errors, value.score().orElseThrow(), JsonPath.ROOT.property("score"))) {
        return errors.toResult();
      }
    }
    if (value.active() == null) {
      if (!errors.add(ValidationError.of("MJJBV-003", "Optional property container 'active' must not be null.", JsonPath.ROOT.property("active")))) {
        return errors.toResult();
      }
    }
    return errors.toResult();
  }

  private static boolean validateFinite(ValidationErrors errors, double value, JsonPath path) {
    if (Double.isFinite(value)) {
      return true;
    }
    return errors.add(ValidationError.of("MJJBV-004", "Expected finite JSON number.", path));
  }
}
