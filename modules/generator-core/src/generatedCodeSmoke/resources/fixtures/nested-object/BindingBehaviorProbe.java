import io.github.mundanej.mjjb.generated.GeneratedBindings;
import io.github.mundanej.mjjb.generated.GeneratedBindings.GeneratedBindingsProfile;
import io.github.mundanej.mjjb.generated.GeneratedBindings.GeneratedBindingsProfileAddress;
import io.github.mundanej.mjjb.generated.GeneratedBindings.GeneratedBindingsSettings;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonReader;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonValidator;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonWriter;
import io.github.mundanej.mjjb.generator.core.generated.GeneratedSourceBehaviorProbe;
import io.github.mundanej.mjjb.parser.JsonStreamReader;
import io.github.mundanej.mjjb.parser.JsonStringWriter;
import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.JsonWriteException;
import io.github.mundanej.mjjb.runtime.ValidationMode;
import io.github.mundanej.mjjb.runtime.ValidationResult;
import java.util.Optional;

public final class BindingBehaviorProbe implements GeneratedSourceBehaviorProbe {
  @Override
  public void run() throws JsonReadException, JsonWriteException {
    GeneratedBindings value =
        GeneratedBindingsJsonReader.read(
            new JsonStreamReader(
                "{\"settings\":{\"enabled\":true},\"profile\":{\"address\":{\"postalCode\":\"12345\",\"city\":\"Paris\"},\"name\":\"Ada\"},\"label\":\"OK\"}"));

    assertBinding(
        new GeneratedBindings(
            new GeneratedBindingsProfile(
                "Ada", new GeneratedBindingsProfileAddress("Paris", Optional.of("12345"))),
            Optional.of(new GeneratedBindingsSettings(Optional.of(true))),
            "OK"),
        value);
    assertValid(GeneratedBindingsJsonValidator.validate(value));

    JsonStringWriter writer = new JsonStringWriter();
    GeneratedBindingsJsonWriter.write(writer, value);
    assertJson(
        "{\"profile\":{\"name\":\"Ada\",\"address\":{\"city\":\"Paris\",\"postalCode\":\"12345\"}},\"settings\":{\"enabled\":true},\"label\":\"OK\"}",
        writer.json());

    assertReadFailure(
        "{\"profile\":{\"address\":{\"city\":\"Paris\"}},\"label\":\"OK\"}",
        "MJJBR-005",
        "$.profile.name");
    assertReadFailure(
        "{\"profile\":{\"name\":\"Ada\",\"address\":{\"city\":\"Paris\",\"extra\":1}},\"label\":\"OK\"}",
        "MJJBR-004",
        "$.profile.address.extra");
    assertReadFailure(
        "{\"profile\":{\"name\":\"Ada\",\"address\":{\"city\":\"Paris\",\"city\":\"Lyon\"}},\"label\":\"OK\"}",
        "MJJBR-003",
        "$.profile.address.city");

    assertValidationFailures();
  }

  private static void assertValidationFailures() {
    GeneratedBindings invalid =
        new GeneratedBindings(
            new GeneratedBindingsProfile(
                "A", new GeneratedBindingsProfileAddress("Paris", Optional.of("bad"))),
            Optional.empty(),
            "A");

    ValidationResult accumulated = GeneratedBindingsJsonValidator.validate(invalid);
    assertInvalidCodes(accumulated, "MJJBV-007", "MJJBV-009", "MJJBV-007");
    assertInvalidPaths(accumulated, "$.profile.name", "$.profile.address.postalCode", "$.label");

    ValidationResult failFast =
        GeneratedBindingsJsonValidator.validate(invalid, ValidationMode.FAIL_FAST);
    assertInvalidCodes(failFast, "MJJBV-007");
    assertInvalidPaths(failFast, "$.profile.name");
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
    }
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

  private static void assertBinding(GeneratedBindings expected, GeneratedBindings actual) {
    if (!expected.equals(actual)) {
      throw new AssertionError("expected " + expected + " but was " + actual);
    }
  }

  private static void assertJson(String expected, String actual) {
    if (!expected.equals(actual)) {
      throw new AssertionError("expected " + expected + " but was " + actual);
    }
  }
}
