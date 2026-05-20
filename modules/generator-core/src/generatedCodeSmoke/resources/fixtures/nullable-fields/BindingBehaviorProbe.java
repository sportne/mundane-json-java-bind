import io.github.mundanej.mjjb.generated.GeneratedBindings;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonReader;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonValidator;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonWriter;
import io.github.mundanej.mjjb.generator.core.generated.GeneratedSourceBehaviorProbe;
import io.github.mundanej.mjjb.parser.JsonStreamReader;
import io.github.mundanej.mjjb.parser.JsonStringWriter;
import io.github.mundanej.mjjb.runtime.JsonField;
import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.JsonWriteException;
import io.github.mundanej.mjjb.runtime.ValidationMode;
import io.github.mundanej.mjjb.runtime.ValidationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class BindingBehaviorProbe implements GeneratedSourceBehaviorProbe {
  @Override
  public void run() throws JsonReadException, JsonWriteException {
    GeneratedBindings nulls =
        GeneratedBindingsJsonReader.read(new JsonStreamReader("{\"tags\":null,\"nickname\":null}"));
    assertBinding(
        new GeneratedBindings(
            JsonField.explicitNull(),
            JsonField.absent(),
            JsonField.absent(),
            JsonField.absent(),
            JsonField.explicitNull(),
            JsonField.absent()),
        nulls);
    assertValid(GeneratedBindingsJsonValidator.validate(nulls));

    JsonStringWriter nullWriter = new JsonStringWriter();
    GeneratedBindingsJsonWriter.write(nullWriter, nulls);
    assertJson("{\"nickname\":null,\"tags\":null}", nullWriter.json());

    GeneratedBindings values =
        GeneratedBindingsJsonReader.read(
            new JsonStreamReader(
                "{\"flags\":[true],\"tags\":[\"red\"],\"score\":9.5,\"code\":\"OK\",\"status\":null,\"nickname\":\"Ada\"}"));
    assertBinding(
        new GeneratedBindings(
            JsonField.value("Ada"),
            JsonField.explicitNull(),
            JsonField.value("OK"),
            JsonField.value(9.5D),
            JsonField.value(List.of("red")),
            JsonField.value(List.of(true))),
        values);
    assertValid(GeneratedBindingsJsonValidator.validate(values));

    JsonStringWriter valueWriter = new JsonStringWriter();
    GeneratedBindingsJsonWriter.write(valueWriter, values);
    assertJson(
        "{\"nickname\":\"Ada\",\"status\":null,\"code\":\"OK\",\"score\":9.5,\"tags\":[\"red\"],\"flags\":[true]}",
        valueWriter.json());

    assertEquals(Optional.empty(), GeneratedBindings.defaultNickname());
    assertEquals(Optional.of("OK"), GeneratedBindings.defaultCode());
    assertDefensiveCopy();
    assertValidationFailures();
    assertReadFailure("{}", "MJJBR-005", "$.nickname");
    assertReadFailure("{\"nickname\":1,\"tags\":null}", "MJJBR-006", "$.nickname");
    assertNonFiniteNumberRejectedByWriter();
  }

  private static void assertDefensiveCopy() {
    ArrayList<String> mutable = new ArrayList<>(List.of("red"));
    GeneratedBindings value =
        new GeneratedBindings(
            JsonField.value("Ada"),
            JsonField.absent(),
            JsonField.absent(),
            JsonField.absent(),
            JsonField.value(mutable),
            JsonField.absent());
    mutable.add("mutated");
    assertEquals(List.of("red"), value.tags().requireValue());
    try {
      value.tags().requireValue().add("new");
      throw new AssertionError("expected immutable nullable list value");
    } catch (UnsupportedOperationException expected) {
      // Expected immutable list contract.
    }
  }

  private static void assertValidationFailures() {
    GeneratedBindings invalid =
        new GeneratedBindings(
            JsonField.absent(),
            JsonField.value("ready"),
            JsonField.value("BAD"),
            JsonField.value(11.0D),
            JsonField.value(List.of("xx", "green")),
            JsonField.value(List.of(false)));

    ValidationResult accumulated = GeneratedBindingsJsonValidator.validate(invalid);
    assertInvalidCodes(
        accumulated,
        "MJJBV-017",
        "MJJBV-016",
        "MJJBV-016",
        "MJJBV-012",
        "MJJBV-007",
        "MJJBV-015",
        "MJJBV-015",
        "MJJBV-016");
    assertInvalidPaths(
        accumulated,
        "$.nickname",
        "$.status",
        "$.code",
        "$.score",
        "$.tags[0]",
        "$.tags[0]",
        "$.tags[1]",
        "$.flags[0]");

    ValidationResult failFast =
        GeneratedBindingsJsonValidator.validate(invalid, ValidationMode.FAIL_FAST);
    assertInvalidCodes(failFast, "MJJBV-017");
    assertInvalidPaths(failFast, "$.nickname");
  }

  private static void assertNonFiniteNumberRejectedByWriter() {
    try {
      GeneratedBindingsJsonWriter.write(
          new JsonStringWriter(),
          new GeneratedBindings(
              JsonField.value("Ada"),
              JsonField.absent(),
              JsonField.absent(),
              JsonField.value(Double.NaN),
              JsonField.value(List.of("red")),
              JsonField.absent()));
      throw new AssertionError("expected non-finite nullable score to be rejected");
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

  private static void assertEquals(Object expected, Object actual) {
    if (!expected.equals(actual)) {
      throw new AssertionError("expected " + expected + " but was " + actual);
    }
  }
}
