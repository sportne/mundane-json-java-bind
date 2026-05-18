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
import java.util.Optional;

public final class BindingBehaviorProbe implements GeneratedSourceBehaviorProbe {
  @Override
  public void run() throws JsonReadException, JsonWriteException {
    JsonStringWriter first = new JsonStringWriter();
    GeneratedBindingsJsonWriter.write(
        first,
        new GeneratedBindings("id-1", 7L, Optional.of("Ada"), Optional.of(3.5D), Optional.empty()));
    assertJson("{\"id\":\"id-1\",\"count\":7,\"displayName\":\"Ada\",\"score\":3.5}", first.json());

    JsonStringWriter second = new JsonStringWriter();
    GeneratedBindingsJsonWriter.write(
        second,
        new GeneratedBindings("id-2", 8L, Optional.empty(), Optional.empty(), Optional.of(true)));
    assertJson("{\"id\":\"id-2\",\"count\":8,\"active\":true}", second.json());

    GeneratedBindings outOfOrder =
        GeneratedBindingsJsonReader.read(
            new JsonStreamReader(
                "{\"active\":false,\"score\":2.25,\"count\":42,\"displayName\":\"Ada\",\"id\":\"u-1\"}"));
    assertBinding(
        new GeneratedBindings(
            "u-1", 42L, Optional.of("Ada"), Optional.of(2.25D), Optional.of(false)),
        outOfOrder);
    assertValid(GeneratedBindingsJsonValidator.validate(outOfOrder));

    GeneratedBindings requiredOnly =
        GeneratedBindingsJsonReader.read(new JsonStreamReader("{\"id\":\"u-2\",\"count\":3}"));
    assertBinding(
        new GeneratedBindings("u-2", 3L, Optional.empty(), Optional.empty(), Optional.empty()),
        requiredOnly);
    assertValid(GeneratedBindingsJsonValidator.validate(requiredOnly));

    assertReadFailure("{\"count\":1}", "MJJBR-005", "$.id");
    assertReadFailure("{\"id\":\"u\",\"count\":1,\"id\":\"again\"}", "MJJBR-003", "$.id");
    assertReadFailure("{\"id\":\"u\",\"count\":1,\"extra\":true}", "MJJBR-004", "$.extra");
    assertReadFailure("{\"id\":null,\"count\":1}", "MJJBR-006", "$.id");
    assertReadFailure("{\"id\":\"u\",\"count\":\"1\"}", "MJJBR-007", "$.count");
    assertReadFailure("{\"id\":\"u\",\"count\":1.5}", "MJJBR-007", "$.count");
    assertReadFailure("{\"id\":\"u\",\"count\":9223372036854775808}", "MJJBR-007", "$.count");
    assertReadFailure("{\"id\":\"u\",\"count\":1,\"score\":1e9999}", "MJJBR-008", "$.score");
    assertReadFailure("{\"id\":\"u\",\"count\":1,\"active\":1}", "MJJBR-009", "$.active");
    assertReadFailure("{\"id\":\"\\x\",\"count\":1}", "MJJBP-013", "$.id");
    assertReadFailure("{\"id\":\"u\",\"count\":1} []", "MJJBR-002", "$");

    assertNonFiniteNumberRejected();
  }

  private static void assertNonFiniteNumberRejected() {
    try {
      GeneratedBindingsJsonWriter.write(
          new JsonStringWriter(),
          new GeneratedBindings(
              "bad", 1L, Optional.empty(), Optional.of(Double.NaN), Optional.empty()));
      throw new AssertionError("expected non-finite score to be rejected");
    } catch (JsonWriteException expected) {
      if (!expected.getMessage().contains("score")) {
        throw new AssertionError("expected score in message but was " + expected.getMessage());
      }
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

  private static void assertValid(ValidationResult result) {
    if (!result.isValid()) {
      throw new AssertionError("expected valid result but got " + result.errors());
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
