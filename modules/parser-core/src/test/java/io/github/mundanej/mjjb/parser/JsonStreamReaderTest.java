package io.github.mundanej.mjjb.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjjb.runtime.JsonReadException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

final class JsonStreamReaderTest {
  @Test
  void readsObjectPropertiesInStreamingOrder() throws JsonReadException {
    JsonStreamReader reader = new JsonStreamReader("{\"id\":\"u-1\",\"active\":true,\"count\":12}");

    reader.beginObject();
    assertTrue(reader.hasNext());
    assertEquals("id", reader.nextName());
    assertEquals("u-1", reader.nextString());
    assertTrue(reader.hasNext());
    assertEquals("active", reader.nextName());
    assertTrue(reader.nextBoolean());
    assertTrue(reader.hasNext());
    assertEquals("count", reader.nextName());
    assertEquals("12", reader.nextNumberLiteral());
    assertFalse(reader.hasNext());
    reader.endObject();
  }

  @Test
  void readsArraysWithCallerHasNextLoop() throws JsonReadException {
    JsonStreamReader reader = new JsonStreamReader("[\"a\",\"b\"]");

    reader.beginArray();
    assertTrue(reader.hasNext());
    assertEquals("a", reader.nextString());
    assertTrue(reader.hasNext());
    assertEquals("b", reader.nextString());
    assertFalse(reader.hasNext());
    reader.endArray();
  }

  @Test
  void reportsInvalidEscapesWithLocation() throws JsonReadException {
    JsonStreamReader reader = new JsonStreamReader("{\"bad\":\"\\x\"}");

    reader.beginObject();
    assertEquals("bad", reader.nextName());
    JsonReadException exception = assertThrows(JsonReadException.class, reader::nextString);
    assertEquals("MJJBP-013", exception.diagnostic().code());
    assertEquals(1, exception.diagnostic().location().lineNumber());
  }

  @Test
  void readsNumbersUnicodeNullsAndBooleans() throws JsonReadException {
    JsonStreamReader reader = new JsonStreamReader("[0,-12.50e+2,\"a\\u0042\",null,false]");

    reader.beginArray();
    assertEquals("0", reader.nextNumberLiteral());
    assertEquals("-12.50e+2", reader.nextNumberLiteral());
    assertEquals("aB", reader.nextString());
    reader.nextNull();
    assertFalse(reader.nextBoolean());
    assertFalse(reader.hasNext());
    reader.endArray();
  }

  @Test
  void skipsNestedValues() throws JsonReadException {
    JsonStreamReader reader =
        new JsonStreamReader("{\"skip\":{\"a\":[true,null]},\"keep\":\"yes\"}");

    reader.beginObject();
    assertEquals("skip", reader.nextName());
    reader.skipValue();
    assertEquals("keep", reader.nextName());
    assertEquals("yes", reader.nextString());
    assertFalse(reader.hasNext());
    reader.endObject();
  }

  @Test
  void rejectsTrailingCommasAndLeadingZeroes() throws JsonReadException {
    JsonStreamReader trailingComma = new JsonStreamReader("[1,]");
    trailingComma.beginArray();
    assertEquals("1", trailingComma.nextNumberLiteral());
    assertThrows(JsonReadException.class, trailingComma::hasNext);

    JsonStreamReader leadingZero = new JsonStreamReader("01");
    assertThrows(JsonReadException.class, leadingZero::nextNumberLiteral);
  }

  @Test
  void readsFromReader() throws IOException, JsonReadException {
    JsonStreamReader reader = JsonStreamReader.fromReader("test.json", new StringReader("\"ok\""));

    assertEquals("ok", reader.nextString());
  }

  @Test
  void fromReaderDoesNotEagerlyDrainInput() throws JsonReadException {
    CountingReader source = new CountingReader("\"ok\"");
    JsonStreamReader reader = JsonStreamReader.fromReader("test.json", source);

    assertEquals(0, source.readCount());
    assertEquals("ok", reader.nextString());
    assertTrue(source.readCount() > 0);
  }

  private static final class CountingReader extends Reader {
    private final String source;
    private int index;
    private int readCount;

    private CountingReader(String source) {
      this.source = source;
    }

    @Override
    public int read(char[] buffer, int offset, int length) {
      if (index >= source.length()) {
        return -1;
      }
      buffer[offset] = source.charAt(index++);
      readCount++;
      return 1;
    }

    @Override
    public void close() {}

    private int readCount() {
      return readCount;
    }
  }
}
