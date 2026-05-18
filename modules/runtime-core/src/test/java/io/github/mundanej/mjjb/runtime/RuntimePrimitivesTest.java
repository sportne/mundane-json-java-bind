package io.github.mundanej.mjjb.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

final class RuntimePrimitivesTest {
  @Test
  void jsonPathBuildsDeterministicPaths() {
    assertEquals(
        "$.user.names[1]", JsonPath.ROOT.property("user").property("names").index(1).value());
    assertThrows(IllegalArgumentException.class, () -> new JsonPath(""));
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          JsonPath ignored = JsonPath.ROOT.index(-1);
          assertEquals("$[-1]", ignored.value());
        });
  }

  @Test
  void jsonFieldPreservesAbsentNullAndValue() {
    assertTrue(JsonField.absent().isAbsent());
    assertTrue(JsonField.explicitNull().isExplicitNull());
    assertEquals("abc", JsonField.value("abc").requireValue());
    assertThrows(NullPointerException.class, () -> JsonField.value(null));
  }

  @Test
  void validationResultRequiresErrorsForInvalidResult() {
    assertTrue(ValidationResult.valid().isValid());
    ValidationError error = ValidationError.of("MJJBT-001", "Broken value.", JsonPath.ROOT);
    ValidationResult result = ValidationResult.invalid(error);
    assertFalse(result.isValid());
    assertEquals("MJJBT-001", result.errors().getFirst().code());
  }

  @Test
  void failFastAccumulatorSignalsStopAfterFirstError() {
    ValidationErrors errors = ValidationErrors.create(ValidationMode.FAIL_FAST);
    assertFalse(errors.add(ValidationError.of("MJJBT-001", "Broken value.", JsonPath.ROOT)));
    assertFalse(errors.toResult().isValid());
    assertThrows(IllegalArgumentException.class, () -> ValidationResult.invalid(List.of()));
  }

  @Test
  void accumulateAccumulatorSignalsContinueAfterEachError() {
    ValidationErrors errors = ValidationErrors.create(ValidationMode.ACCUMULATE);

    assertTrue(errors.add(ValidationError.of("MJJBT-001", "Broken value.", JsonPath.ROOT)));
    assertTrue(
        errors.add(
            ValidationError.of("MJJBT-002", "Also broken.", JsonPath.ROOT.property("field"))));

    ValidationResult result = errors.toResult();
    assertFalse(result.isValid());
    assertEquals(
        List.of("MJJBT-001", "MJJBT-002"),
        result.errors().stream().map(ValidationError::code).toList());
  }

  @Test
  void diagnosticsAndReadExceptionsCarryStableValues() {
    JsonDiagnostic diagnostic =
        JsonDiagnostic.error(
            "MJJBT-002", "Bad token.", JsonPath.ROOT, new JsonLocation("test.json", 3L, 1, 4));
    JsonReadException exception = new JsonReadException(diagnostic);

    assertEquals(JsonDiagnosticSeverity.ERROR, diagnostic.severity());
    assertEquals("Bad token.", exception.getMessage());
    assertEquals(diagnostic, exception.diagnostic());
    assertTrue(JsonToken.values().length > 0);
    assertTrue(JsonDiagnosticSeverity.values().length > 0);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new JsonDiagnostic(
                "",
                JsonDiagnosticSeverity.ERROR,
                "Bad token.",
                JsonPath.ROOT,
                JsonLocation.UNKNOWN,
                SchemaLocation.UNKNOWN));
  }

  @Test
  void locationAndSchemaLocationValidateCoordinatesAndPointers() {
    assertThrows(IllegalArgumentException.class, () -> new JsonLocation("test.json", -2L, 1, 1));
    assertThrows(IllegalArgumentException.class, () -> new JsonLocation("test.json", 0L, 0, 1));
    assertThrows(
        IllegalArgumentException.class,
        () -> new SchemaLocation(URI.create("urn:test"), "not-a-pointer"));
  }

  @Test
  void writeExceptionsValidateMessages() {
    JsonWriteException exception = new JsonWriteException("Cannot write.");
    JsonWriteException caused = new JsonWriteException("Cannot write.", exception);

    assertEquals("Cannot write.", exception.getMessage());
    assertEquals(exception, caused.getCause());
    assertThrows(IllegalArgumentException.class, RuntimePrimitivesTest::blankWriteException);
  }

  @Test
  void readerDefaultReadsNullableStringState() throws JsonReadException {
    assertTrue(new NullableStringReader(true).nextNullableString().isExplicitNull());
    assertEquals("value", new NullableStringReader(false).nextNullableString().requireValue());
  }

  private static final class NullableStringReader implements JsonReader {
    private final boolean nullValue;

    private NullableStringReader(boolean nullValue) {
      this.nullValue = nullValue;
    }

    @Override
    public JsonToken peek() {
      return nullValue ? JsonToken.NULL : JsonToken.STRING;
    }

    @Override
    public void beginObject() {}

    @Override
    public void endObject() {}

    @Override
    public void beginArray() {}

    @Override
    public void endArray() {}

    @Override
    public boolean hasNext() {
      return false;
    }

    @Override
    public String nextName() {
      return "";
    }

    @Override
    public String nextString() {
      return "value";
    }

    @Override
    public String nextNumberLiteral() {
      return "1";
    }

    @Override
    public boolean nextBoolean() {
      return true;
    }

    @Override
    public void nextNull() {}

    @Override
    public void skipValue() {}

    @Override
    public JsonLocation location() {
      return JsonLocation.UNKNOWN;
    }
  }

  private static JsonWriteException blankWriteException() {
    return new JsonWriteException("");
  }
}
