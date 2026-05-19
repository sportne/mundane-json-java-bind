import io.github.mundanej.mjjb.generated.GeneratedBindings;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class BindingBehaviorProbe implements GeneratedSourceBehaviorProbe {
  @Override
  public void run() throws JsonReadException, JsonWriteException {
    ArrayList<String> originalTags = new ArrayList<>(List.of("red", "blue"));
    GeneratedBindings value =
        new GeneratedBindings(
            originalTags,
            List.of(1L, 2L),
            Optional.of(List.of(1.5D, 2.25D)),
            Optional.of(List.of(true, false)));
    originalTags.add("mutated");
    assertList(List.of("red", "blue"), value.tags());
    assertImmutable(value.tags());
    assertValid(GeneratedBindingsJsonValidator.validate(value));

    JsonStringWriter writer = new JsonStringWriter();
    GeneratedBindingsJsonWriter.write(writer, value);
    assertJson(
        "{\"tags\":[\"red\",\"blue\"],\"counts\":[1,2],\"scores\":[1.5,2.25],\"flags\":[true,false]}",
        writer.json());

    GeneratedBindings outOfOrder =
        GeneratedBindingsJsonReader.read(
            new JsonStreamReader(
                "{\"flags\":[false,true],\"counts\":[5,6],\"scores\":[3.5],\"tags\":[\"x\"]}"));
    assertBinding(
        new GeneratedBindings(
            List.of("x"),
            List.of(5L, 6L),
            Optional.of(List.of(3.5D)),
            Optional.of(List.of(false, true))),
        outOfOrder);

    JsonStringWriter requiredOnlyWriter = new JsonStringWriter();
    GeneratedBindingsJsonWriter.write(
        requiredOnlyWriter,
        new GeneratedBindings(List.of("only"), List.of(), Optional.empty(), Optional.empty()));
    assertJson("{\"tags\":[\"only\"],\"counts\":[]}", requiredOnlyWriter.json());

    assertReadFailure("{\"counts\":[]}", "MJJBR-005", "$.tags");
    assertReadFailure("{\"tags\":\"red\",\"counts\":[]}", "MJJBR-010", "$.tags");
    assertReadFailure("{\"tags\":[\"red\"],\"counts\":[1.5]}", "MJJBR-007", "$.counts[0]");
    assertReadFailure(
        "{\"tags\":[\"red\"],\"counts\":[9223372036854775808]}", "MJJBR-007", "$.counts[0]");
    assertReadFailure(
        "{\"tags\":[\"red\"],\"counts\":[],\"scores\":[1e9999]}", "MJJBR-008", "$.scores[0]");
    assertReadFailure("{\"tags\":[\"\\x\"],\"counts\":[]}", "MJJBP-013", "$.tags[0]");

    assertValidationFailures();
    assertNonFiniteNumberRejectedByWriter();
  }

  private static void assertValidationFailures() {
    GeneratedBindings invalid =
        new GeneratedBindings(
            List.of(),
            List.of(),
            Optional.of(List.of(Double.NaN, Double.POSITIVE_INFINITY, 1.0D)),
            Optional.empty());

    ValidationResult accumulated = GeneratedBindingsJsonValidator.validate(invalid);
    assertInvalidCodes(accumulated, "MJJBV-005", "MJJBV-006", "MJJBV-004", "MJJBV-004");
    assertInvalidPaths(accumulated, "$.tags", "$.scores", "$.scores[0]", "$.scores[1]");

    ValidationResult failFast =
        GeneratedBindingsJsonValidator.validate(invalid, ValidationMode.FAIL_FAST);
    assertInvalidCodes(failFast, "MJJBV-005");
    assertInvalidPaths(failFast, "$.tags");
  }

  private static void assertNonFiniteNumberRejectedByWriter() {
    try {
      GeneratedBindingsJsonWriter.write(
          new JsonStringWriter(),
          new GeneratedBindings(
              List.of("bad"), List.of(), Optional.of(List.of(Double.NaN)), Optional.empty()));
      throw new AssertionError("expected non-finite score item to be rejected");
    } catch (JsonWriteException expected) {
      if (!expected.getMessage().contains("scores")) {
        throw new AssertionError("expected scores in message but was " + expected.getMessage());
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

  private static void assertImmutable(List<String> values) {
    try {
      values.add("new");
      throw new AssertionError("expected immutable list");
    } catch (UnsupportedOperationException expected) {
      // Expected immutable list contract.
    }
  }

  private static void assertBinding(GeneratedBindings expected, GeneratedBindings actual) {
    if (!expected.equals(actual)) {
      throw new AssertionError("expected " + expected + " but was " + actual);
    }
  }

  private static void assertList(List<String> expected, List<String> actual) {
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
