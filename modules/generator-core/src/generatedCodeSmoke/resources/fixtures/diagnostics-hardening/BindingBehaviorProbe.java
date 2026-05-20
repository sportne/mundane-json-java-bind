import io.github.mundanej.mjjb.generated.GeneratedBindings;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonReader;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonValidator;
import io.github.mundanej.mjjb.generator.core.generated.GeneratedSourceBehaviorProbe;
import io.github.mundanej.mjjb.parser.JsonStreamReader;
import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.ValidationMode;
import io.github.mundanej.mjjb.runtime.ValidationResult;
import java.util.List;
import java.util.Optional;

public final class BindingBehaviorProbe implements GeneratedSourceBehaviorProbe {
  @Override
  public void run() throws JsonReadException {
    GeneratedBindings value =
        GeneratedBindingsJsonReader.read(
            new JsonStreamReader(
                "{\"labels\":[\"aa\"],\"scores\":[1.5],\"count\":3,\"name\":\"Ada\"}"));
    assertBinding(
        new GeneratedBindings("Ada", 3L, Optional.of(List.of(1.5D)), Optional.of(List.of("aa"))),
        value);

    assertReadFailure("{\"name\":\"Ada\",\"name\":\"Grace\",\"count\":1}", "MJJBR-003", "$.name");
    assertReadFailure("{\"name\":\"Ada\",\"count\":1,\"a.b\":true}", "MJJBR-004", "$[\"a.b\"]");
    assertReadFailure("{\"count\":1}", "MJJBR-005", "$.name");
    assertReadFailure("{\"name\":true,\"count\":1}", "MJJBR-006", "$.name");
    assertReadFailure("{\"name\":\"Ada\",\"count\":1,\"scores\":true}", "MJJBR-010", "$.scores");
    assertReadFailure(
        "{\"name\":\"Ada\",\"count\":1,\"scores\":[\"bad\"]}", "MJJBR-008", "$.scores[0]");
    assertReadFailure("{\"name\":\"\\x\",\"count\":1}", "MJJBP-013", "$.name");
    assertReadFailure("{\"name\":\"Ada\",\"count\":1} true", "MJJBR-002", "$");

    assertValidationOrdering();
  }

  private static void assertValidationOrdering() {
    GeneratedBindings invalid =
        new GeneratedBindings(
            "A",
            3L,
            Optional.of(List.of(Double.NaN, Double.POSITIVE_INFINITY, 1.0D)),
            Optional.of(List.of("x")));

    ValidationResult accumulated = GeneratedBindingsJsonValidator.validate(invalid);
    assertInvalidCodes(
        accumulated, "MJJBV-007", "MJJBV-006", "MJJBV-004", "MJJBV-004", "MJJBV-007");
    assertInvalidPaths(
        accumulated, "$.name", "$.scores", "$.scores[0]", "$.scores[1]", "$.labels[0]");

    ValidationResult failFast =
        GeneratedBindingsJsonValidator.validate(invalid, ValidationMode.FAIL_FAST);
    assertInvalidCodes(failFast, "MJJBV-007");
    assertInvalidPaths(failFast, "$.name");
  }

  private static void assertReadFailure(String json, String code, String path) {
    try {
      GeneratedBindingsJsonReader.read(new JsonStreamReader(json));
      throw new AssertionError("expected read failure " + code);
    } catch (JsonReadException expected) {
      if (!code.equals(expected.diagnostic().code())) {
        throw new AssertionError("expected code " + code + " but was " + expected.diagnostic());
      }
      if (!path.equals(expected.diagnostic().path().value())) {
        throw new AssertionError("expected path " + path + " but was " + expected.diagnostic());
      }
      if (expected.diagnostic().location().lineNumber() < 1) {
        throw new AssertionError("expected readable location but was " + expected.diagnostic());
      }
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

  private static void assertBinding(GeneratedBindings expected, GeneratedBindings actual) {
    if (!expected.equals(actual)) {
      throw new AssertionError("expected " + expected + " but was " + actual);
    }
  }
}
