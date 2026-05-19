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
            "open",
            "record",
            Optional.of(1L),
            Optional.of(1.5D),
            Optional.of(true),
            Optional.empty(),
            List.of("red", "blue"),
            Optional.of(List.of(true, true)));
    assertValid(GeneratedBindingsJsonValidator.validate(valid));

    GeneratedBindings optionalAbsent =
        new GeneratedBindings(
            "closed",
            "record",
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            List.of("red"),
            Optional.empty());
    assertValid(GeneratedBindingsJsonValidator.validate(optionalAbsent));

    assertEquals(Optional.of("open"), GeneratedBindings.defaultStatus());
    assertEquals(Optional.of(1L), GeneratedBindings.defaultPriority());
    assertEquals(Optional.of(1.5D), GeneratedBindings.defaultScore());
    assertEquals(Optional.of(true), GeneratedBindings.defaultActive());
    assertEquals(Optional.empty(), GeneratedBindings.defaultVoided());

    GeneratedBindings invalid =
        new GeneratedBindings(
            "draft",
            "other",
            Optional.of(3L),
            Optional.of(3.0D),
            Optional.of(false),
            Optional.of("x"),
            List.of("red", "green"),
            Optional.of(List.of(true, false)));
    ValidationResult accumulated = GeneratedBindingsJsonValidator.validate(invalid);
    assertInvalidCodes(
        accumulated,
        "MJJBV-015",
        "MJJBV-016",
        "MJJBV-015",
        "MJJBV-015",
        "MJJBV-016",
        "MJJBV-016",
        "MJJBV-016",
        "MJJBV-015",
        "MJJBV-016");
    assertInvalidPaths(
        accumulated,
        "$.status",
        "$.kind",
        "$.priority",
        "$.score",
        "$.score",
        "$.active",
        "$.voided",
        "$.tags[1]",
        "$.flags[1]");

    ValidationResult failFast =
        GeneratedBindingsJsonValidator.validate(invalid, ValidationMode.FAIL_FAST);
    assertInvalidCodes(failFast, "MJJBV-015");
    assertInvalidPaths(failFast, "$.status");
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

  private static void assertEquals(Object expected, Object actual) {
    if (!expected.equals(actual)) {
      throw new AssertionError("expected " + expected + " but got " + actual);
    }
  }
}
