import io.github.mundanej.mjjb.generated.GeneratedBindings;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonReader;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonValidator;
import io.github.mundanej.mjjb.generator.core.generated.GeneratedSourceBehaviorProbe;
import io.github.mundanej.mjjb.parser.JsonStreamReader;
import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.JsonWriteException;
import io.github.mundanej.mjjb.runtime.ValidationMode;
import io.github.mundanej.mjjb.runtime.ValidationResult;
import java.util.Map;
import java.util.Optional;

public final class BindingBehaviorProbe implements GeneratedSourceBehaviorProbe {
  @Override
  public void run() throws JsonReadException, JsonWriteException {
    GeneratedBindings valid =
        new GeneratedBindings(
            "root", Optional.of("manual"), Optional.of("detail"), Map.of("x-a", "value"));
    assertValid(GeneratedBindingsJsonValidator.validate(valid));

    GeneratedBindings read =
        GeneratedBindingsJsonReader.read(
            new JsonStreamReader(
                "{\"id\":\"root\",\"mode\":\"manual\",\"details\":\"detail\",\"x-a\":\"value\"}"));
    assertValid(GeneratedBindingsJsonValidator.validate(read));

    assertInvalid(
        GeneratedBindingsJsonValidator.validate(
            new GeneratedBindings("root", Optional.empty(), Optional.empty(), Map.of())),
        "MJJBV-018",
        "$");
    assertInvalid(
        GeneratedBindingsJsonValidator.validate(
            new GeneratedBindings("root", Optional.of("manual"), Optional.empty(), Map.of())),
        "MJJBV-020",
        "$.details");
    assertInvalid(
        GeneratedBindingsJsonValidator.validate(
            new GeneratedBindings(
                "root",
                Optional.of("manual"),
                Optional.of("detail"),
                Map.of("x-a", "value", "x-b", "value"))),
        "MJJBV-019",
        "$");
    assertInvalid(
        GeneratedBindingsJsonValidator.validate(
            new GeneratedBindings(
                "root",
                Optional.of("manual"),
                Optional.of("detail"),
                Map.of("x-toolong", "value"))),
        "MJJBV-008",
        "$[\"x-toolong\"]");

    ValidationResult failFast =
        GeneratedBindingsJsonValidator.validate(
            new GeneratedBindings("root", Optional.of("manual"), Optional.empty(), Map.of()),
            ValidationMode.FAIL_FAST);
    assertInvalid(failFast, "MJJBV-020", "$.details");
  }

  private static void assertValid(ValidationResult result) {
    if (!result.isValid()) {
      throw new AssertionError("expected valid result but got " + result.errors());
    }
  }

  private static void assertInvalid(
      ValidationResult result, String expectedCode, String expectedPath) {
    if (result.isValid()) {
      throw new AssertionError("expected invalid validation result");
    }
    if (!expectedCode.equals(result.errors().getFirst().code())
        || !expectedPath.equals(result.errors().getFirst().path().value())) {
      throw new AssertionError("unexpected validation errors " + result.errors());
    }
  }
}
