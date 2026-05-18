package io.github.mundanej.mjjb.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.JsonToken;
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
  void readsWhitespaceNestedValuesEscapesAndNumberForms() throws JsonReadException {
    JsonStreamReader reader =
        new JsonStreamReader(
            "{ \"text\": \"\\\\\\\"\\/\\b\\f\\n\\r\\t\\u0041\", \"numbers\": [0, -1, 12, 3.50, 6e7, -8.25E-3], \"nested\": {\"ok\": true, \"none\": null} }");

    reader.beginObject();
    assertEquals("text", reader.nextName());
    assertEquals("\\\"/\b\f\n\r\tA", reader.nextString());
    assertEquals("numbers", reader.nextName());
    reader.beginArray();
    assertEquals("0", reader.nextNumberLiteral());
    assertEquals("-1", reader.nextNumberLiteral());
    assertEquals("12", reader.nextNumberLiteral());
    assertEquals("3.50", reader.nextNumberLiteral());
    assertEquals("6e7", reader.nextNumberLiteral());
    assertEquals("-8.25E-3", reader.nextNumberLiteral());
    assertFalse(reader.hasNext());
    reader.endArray();
    assertEquals("nested", reader.nextName());
    reader.beginObject();
    assertEquals("ok", reader.nextName());
    assertTrue(reader.nextBoolean());
    assertEquals("none", reader.nextName());
    reader.nextNull();
    assertFalse(reader.hasNext());
    reader.endObject();
    assertFalse(reader.hasNext());
    reader.endObject();
    assertEquals(JsonToken.END_DOCUMENT, reader.peek());
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
  void rejectsInvalidStringGrammarDeterministically() {
    assertReadExceptionCode("\"\\x\"", "MJJBP-013");
    assertReadExceptionCode("\"\\u12\"", "MJJBP-014");
    assertReadExceptionCode("\"\\u12x4\"", "MJJBP-015");
    assertReadExceptionCode("\"bad\nstring\"", "MJJBP-010");
    assertReadExceptionCode("\"unterminated", "MJJBP-011");
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
  void rejectsInvalidNumberGrammarDeterministically() {
    assertReadExceptionCode("-", "MJJBP-016");
    assertReadExceptionCode("1.", "MJJBP-016");
    assertReadExceptionCode("1e", "MJJBP-016");
    assertReadExceptionCode("1e+", "MJJBP-016");
    assertReadExceptionCode("01", "MJJBP-017");
    assertReadExceptionCode("-01", "MJJBP-017");
  }

  @Test
  void rejectsInvalidLiteralsAndTrailingRootContent() throws JsonReadException {
    assertReadExceptionCode("tru", "MJJBP-006");
    assertReadExceptionCode("nul", "MJJBP-018");

    JsonStreamReader booleanWithTrailingContent = new JsonStreamReader("truex");
    assertTrue(booleanWithTrailingContent.nextBoolean());
    assertReadExceptionCode(booleanWithTrailingContent::peek, "MJJBP-001");

    JsonStreamReader numberWithTrailingContent = new JsonStreamReader("1 2");
    assertEquals("1", numberWithTrailingContent.nextNumberLiteral());
    assertEquals(JsonToken.NUMBER, numberWithTrailingContent.peek());
  }

  @Test
  void rejectsInvalidContainerGrammarDeterministically() throws JsonReadException {
    JsonStreamReader trailingComma = new JsonStreamReader("[1,]");
    trailingComma.beginArray();
    assertEquals("1", trailingComma.nextNumberLiteral());
    assertReadExceptionCode(trailingComma::hasNext, "MJJBP-003");

    JsonStreamReader missingArrayComma = new JsonStreamReader("[1 2]");
    missingArrayComma.beginArray();
    assertEquals("1", missingArrayComma.nextNumberLiteral());
    assertReadExceptionCode(missingArrayComma::hasNext, "MJJBP-019");

    JsonStreamReader missingObjectComma = new JsonStreamReader("{\"a\":1 \"b\":2}");
    missingObjectComma.beginObject();
    assertEquals("a", missingObjectComma.nextName());
    assertEquals("1", missingObjectComma.nextNumberLiteral());
    assertReadExceptionCode(missingObjectComma::hasNext, "MJJBP-019");

    JsonStreamReader missingColon = new JsonStreamReader("{\"a\" 1}");
    missingColon.beginObject();
    assertReadExceptionCode(missingColon::nextName, "MJJBP-019");

    JsonStreamReader mismatchedEnd = new JsonStreamReader("{}");
    mismatchedEnd.beginObject();
    assertReadExceptionCode(mismatchedEnd::endArray, "MJJBP-020");

    assertReadExceptionCode(new JsonStreamReader("")::skipValue, "MJJBP-007");
  }

  @Test
  void reportsLocationsAcrossLines() throws JsonReadException {
    JsonStreamReader reader = new JsonStreamReader("multi.json", "{\n  \"bad\": \"\\x\"\n}");

    reader.beginObject();
    assertEquals("bad", reader.nextName());
    JsonReadException exception = assertThrows(JsonReadException.class, reader::nextString);

    assertEquals("MJJBP-013", exception.diagnostic().code());
    assertEquals("multi.json", exception.diagnostic().location().sourceName());
    assertEquals(2, exception.diagnostic().location().lineNumber());
    assertEquals(13, exception.diagnostic().location().columnNumber());
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

  @Test
  void streamsLargeArraysFromReaderIncrementally() throws JsonReadException {
    StringBuilder sourceBuilder = new StringBuilder("[");
    for (int i = 0; i < 512; i++) {
      if (i > 0) {
        sourceBuilder.append(',');
      }
      sourceBuilder.append(i);
    }
    sourceBuilder.append(']');
    CountingReader source = new CountingReader(sourceBuilder.toString());
    JsonStreamReader reader = JsonStreamReader.fromReader("large.json", source);

    assertEquals(0, source.readCount());
    reader.beginArray();
    assertTrue(reader.hasNext());
    assertEquals("0", reader.nextNumberLiteral());
    assertTrue(source.readCount() < sourceBuilder.length());
    for (int i = 1; i < 512; i++) {
      assertTrue(reader.hasNext());
      assertEquals(Integer.toString(i), reader.nextNumberLiteral());
    }
    assertFalse(reader.hasNext());
    reader.endArray();
    assertEquals(JsonToken.END_DOCUMENT, reader.peek());
  }

  private static void assertReadExceptionCode(String input, String code) {
    assertReadExceptionCode(new JsonStreamReader(input)::skipValue, code);
  }

  private static void assertReadExceptionCode(ThrowingReaderAction action, String code) {
    JsonReadException exception = assertThrows(JsonReadException.class, action::run);
    assertEquals(code, exception.diagnostic().code());
  }

  @FunctionalInterface
  private interface ThrowingReaderAction {
    void run() throws JsonReadException;
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
