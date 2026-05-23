package io.github.mundanej.mjjb.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class JsonReaderTest {
  @Test
  void nextNullableStringConsumesNullWithoutReadingString() throws JsonReadException {
    StubReader reader = StubReader.nullString();

    JsonField<String> field = reader.nextNullableString();

    assertTrue(field.isExplicitNull());
    assertTrue(reader.nullConsumed);
    assertFalse(reader.stringConsumed);
  }

  @Test
  void nextNullableStringReadsNonNullString() throws JsonReadException {
    StubReader reader = StubReader.string("value");

    JsonField<String> field = reader.nextNullableString();

    assertTrue(field.hasValue());
    assertEquals("value", field.requireValue());
    assertFalse(reader.nullConsumed);
    assertTrue(reader.stringConsumed);
  }

  @Test
  void nextNullableStringPropagatesReaderFailures() {
    JsonReadException failure =
        new JsonReadException(
            JsonDiagnostic.error("MJJBT-001", "Broken.", JsonPath.ROOT, JsonLocation.UNKNOWN));

    assertThrows(
        JsonReadException.class, () -> StubReader.failingPeek(failure).nextNullableString());
    assertThrows(
        JsonReadException.class, () -> StubReader.failingString(failure).nextNullableString());
  }

  private static final class StubReader implements JsonReader {
    private final JsonToken token;
    private final String string;
    private final JsonReadException peekFailure;
    private final JsonReadException stringFailure;
    private boolean nullConsumed;
    private boolean stringConsumed;

    private StubReader(
        JsonToken token,
        String string,
        JsonReadException peekFailure,
        JsonReadException stringFailure) {
      this.token = token;
      this.string = string;
      this.peekFailure = peekFailure;
      this.stringFailure = stringFailure;
    }

    static StubReader nullString() {
      return new StubReader(JsonToken.NULL, "", null, null);
    }

    static StubReader string(String value) {
      return new StubReader(JsonToken.STRING, value, null, null);
    }

    static StubReader failingPeek(JsonReadException failure) {
      return new StubReader(JsonToken.STRING, "", failure, null);
    }

    static StubReader failingString(JsonReadException failure) {
      return new StubReader(JsonToken.STRING, "", null, failure);
    }

    @Override
    public JsonToken peek() throws JsonReadException {
      if (peekFailure != null) {
        throw peekFailure;
      }
      return token;
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
    public String nextString() throws JsonReadException {
      stringConsumed = true;
      if (stringFailure != null) {
        throw stringFailure;
      }
      return string;
    }

    @Override
    public String nextNumberLiteral() {
      return "0";
    }

    @Override
    public boolean nextBoolean() {
      return false;
    }

    @Override
    public void nextNull() {
      nullConsumed = true;
    }

    @Override
    public void skipValue() {}

    @Override
    public JsonLocation location() {
      return JsonLocation.UNKNOWN;
    }
  }
}
