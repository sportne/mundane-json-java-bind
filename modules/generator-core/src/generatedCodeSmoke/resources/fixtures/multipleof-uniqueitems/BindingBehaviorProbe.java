import io.github.mundanej.mjjb.generated.GeneratedBindings;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonValidator;
import io.github.mundanej.mjjb.generator.core.generated.GeneratedSourceBehaviorProbe;
import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.JsonWriteException;
import io.github.mundanej.mjjb.runtime.ValidationMode;
import io.github.mundanej.mjjb.runtime.ValidationResult;
import java.util.List;

public final class BindingBehaviorProbe implements GeneratedSourceBehaviorProbe {
  @Override
  public void run() throws JsonReadException, JsonWriteException {
    GeneratedBindings valid =
        new GeneratedBindings(
            10L, 1.25D, List.of(2L, 4L, 6L), List.of("alpha", "beta"), List.of(1.0D, 1.5D));
    assertValid(GeneratedBindingsJsonValidator.validate(valid));

    GeneratedBindings invalid =
        new GeneratedBindings(
            11L, 1.3D, List.of(2L, 3L, 2L), List.of("same", "same"), List.of(1.0D, 1.00D));

    ValidationResult accumulated = GeneratedBindingsJsonValidator.validate(invalid);
    assertInvalidCodes(
        accumulated, "MJJBV-021", "MJJBV-021", "MJJBV-021", "MJJBV-022", "MJJBV-022", "MJJBV-022");
    assertInvalidPaths(
        accumulated,
        "$.stepCount",
        "$.ratio",
        "$.steps[1]",
        "$.steps[2]",
        "$.labels[1]",
        "$.scores[1]");

    ValidationResult failFast =
        GeneratedBindingsJsonValidator.validate(invalid, ValidationMode.FAIL_FAST);
    assertInvalidCodes(failFast, "MJJBV-021");
    assertInvalidPaths(failFast, "$.stepCount");

    GeneratedBindings nonFinite =
        new GeneratedBindings(
            10L, 1.25D, List.of(2L, 4L), List.of("alpha", "beta"), List.of(Double.NaN, Double.NaN));
    ValidationResult nonFiniteAccumulated = GeneratedBindingsJsonValidator.validate(nonFinite);
    assertInvalidCodes(nonFiniteAccumulated, "MJJBV-004", "MJJBV-004");
    assertInvalidPaths(nonFiniteAccumulated, "$.scores[0]", "$.scores[1]");
  }

  private static void assertValid(ValidationResult result) {
    if (!result.isValid()) {
      throw new AssertionError("expected valid result but got " + result.errors());
    }
  }

  private static void assertInvalidCodes(ValidationResult result, String... expectedCodes) {
    if (result.isValid()) {
      throw new AssertionError("expected invalid validation result");
    }
    if (result.errors().size() != expectedCodes.length) {
      throw new AssertionError(
          "expected " + expectedCodes.length + " errors but got " + result.errors());
    }
    for (int index = 0; index < expectedCodes.length; index++) {
      if (!expectedCodes[index].equals(result.errors().get(index).code())) {
        throw new AssertionError("unexpected validation errors " + result.errors());
      }
    }
  }

  private static void assertInvalidPaths(ValidationResult result, String... expectedPaths) {
    for (int index = 0; index < expectedPaths.length; index++) {
      if (!expectedPaths[index].equals(result.errors().get(index).path().value())) {
        throw new AssertionError("unexpected validation paths " + result.errors());
      }
    }
  }
}
