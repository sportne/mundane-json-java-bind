import io.github.mundanej.mjjb.generated.GeneratedBindings;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonValidator;
import io.github.mundanej.mjjb.generator.core.generated.GeneratedSourceBehaviorProbe;
import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.JsonWriteException;
import io.github.mundanej.mjjb.runtime.ValidationMode;
import io.github.mundanej.mjjb.runtime.ValidationResult;
import java.util.List;
import java.util.Optional;

public final class BindingBehaviorProbe implements GeneratedSourceBehaviorProbe {
  @Override
  public void run() throws JsonReadException, JsonWriteException {
    GeneratedBindings valid =
        new GeneratedBindings(
            "A\uD83D\uDE00",
            Optional.of("OK"),
            Optional.of("A\uD83D\uDE00"),
            Optional.of("2026-05-18"),
            Optional.of("2026-05-18T12:30:00Z"),
            Optional.of("123e4567-e89b-12d3-a456-426614174000"),
            5L,
            Optional.of(0.5D),
            List.of("ada", "bob"),
            Optional.of(List.of(0.0D, 100.0D)));
    assertValid(GeneratedBindingsJsonValidator.validate(valid));

    GeneratedBindings invalid =
        new GeneratedBindings(
            "a",
            Optional.of("no"),
            Optional.of("ABCDE"),
            Optional.of("2026-02-30"),
            Optional.of("2026-05-18T12:30:00"),
            Optional.of("not-a-uuid"),
            0L,
            Optional.of(1.0D),
            List.of("x", "AL"),
            Optional.of(List.of(-1.0D, 101.0D)));

    ValidationResult accumulated = GeneratedBindingsJsonValidator.validate(invalid);
    assertInvalidCodes(
        accumulated,
        "MJJBV-007",
        "MJJBV-009",
        "MJJBV-008",
        "MJJBV-010",
        "MJJBV-010",
        "MJJBV-010",
        "MJJBV-011",
        "MJJBV-014",
        "MJJBV-007",
        "MJJBV-009",
        "MJJBV-011",
        "MJJBV-012");
    assertInvalidPaths(
        accumulated,
        "$.code",
        "$.symbol",
        "$.label",
        "$.eventDate",
        "$.createdAt",
        "$.identifier",
        "$.count",
        "$.ratio",
        "$.names[0]",
        "$.names[1]",
        "$.scores[0]",
        "$.scores[1]");

    ValidationResult failFast =
        GeneratedBindingsJsonValidator.validate(invalid, ValidationMode.FAIL_FAST);
    assertInvalidCodes(failFast, "MJJBV-007");
    assertInvalidPaths(failFast, "$.code");
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
