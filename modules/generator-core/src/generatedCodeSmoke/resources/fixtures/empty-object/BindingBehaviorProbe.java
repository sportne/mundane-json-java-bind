import io.github.mundanej.mjjb.generated.GeneratedBindings;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonReader;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonValidator;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonWriter;
import io.github.mundanej.mjjb.generator.core.generated.GeneratedSourceBehaviorProbe;
import io.github.mundanej.mjjb.parser.JsonStreamReader;
import io.github.mundanej.mjjb.parser.JsonStringWriter;
import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.JsonWriteException;
import io.github.mundanej.mjjb.runtime.ValidationResult;

public final class BindingBehaviorProbe implements GeneratedSourceBehaviorProbe {
  @Override
  public void run() throws JsonReadException, JsonWriteException {
    JsonStringWriter writer = new JsonStringWriter();
    GeneratedBindingsJsonWriter.write(writer, new GeneratedBindings());
    assertJson("{}", writer.json());

    GeneratedBindings read = GeneratedBindingsJsonReader.read(new JsonStreamReader("{}"));
    assertValid(GeneratedBindingsJsonValidator.validate(read));
    assertInvalid(GeneratedBindingsJsonValidator.validate(null), "MJJBV-001", "$");
    assertReadFailure("{\"extra\":true}", "MJJBR-004", "$.extra");
  }

  private static void assertValid(ValidationResult result) {
    if (!result.isValid()) {
      throw new AssertionError("expected valid result but got " + result.errors());
    }
  }

  private static void assertInvalid(ValidationResult result, String code, String path) {
    if (result.isValid()) {
      throw new AssertionError("expected invalid result");
    }
    if (!code.equals(result.errors().getFirst().code())) {
      throw new AssertionError("expected code " + code + " but was " + result.errors());
    }
    if (!path.equals(result.errors().getFirst().path().value())) {
      throw new AssertionError("expected path " + path + " but was " + result.errors());
    }
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

  private static void assertJson(String expected, String actual) {
    if (!expected.equals(actual)) {
      throw new AssertionError("expected " + expected + " but was " + actual);
    }
  }
}
