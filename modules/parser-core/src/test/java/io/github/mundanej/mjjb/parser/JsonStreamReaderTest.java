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
  void streamsDuplicateObjectPropertiesInOrderWithDistinctLocations() throws JsonReadException {
    JsonStreamReader reader = new JsonStreamReader("duplicates.json", "{\"id\":1,\"id\":2}");

    reader.beginObject();
    assertTrue(reader.hasNext());
    assertEquals("id", reader.nextName());
    assertEquals(1, reader.location().lineNumber());
    assertEquals(7, reader.location().columnNumber());
    assertEquals("1", reader.nextNumberLiteral());
    assertTrue(reader.hasNext());
    assertEquals("id", reader.nextName());
    assertEquals(1, reader.location().lineNumber());
    assertEquals(14, reader.location().columnNumber());
    assertEquals("2", reader.nextNumberLiteral());
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
  void readsEmptyContainersAndHasNextIsStable() throws JsonReadException {
    JsonStreamReader reader = new JsonStreamReader("{\"emptyObject\":{},\"emptyArray\":[]}");

    reader.beginObject();
    assertTrue(reader.hasNext());
    assertTrue(reader.hasNext());
    assertEquals("emptyObject", reader.nextName());
    reader.beginObject();
    assertFalse(reader.hasNext());
    assertFalse(reader.hasNext());
    reader.endObject();
    assertEquals("emptyArray", reader.nextName());
    reader.beginArray();
    assertFalse(reader.hasNext());
    assertFalse(reader.hasNext());
    reader.endArray();
    assertFalse(reader.hasNext());
    reader.endObject();
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
    JsonStreamReader reader =
        new JsonStreamReader("[0,-12.50e+2,1.01,1e05,\"a\\u0042\",null,false]");

    reader.beginArray();
    assertEquals("0", reader.nextNumberLiteral());
    assertEquals("-12.50e+2", reader.nextNumberLiteral());
    assertEquals("1.01", reader.nextNumberLiteral());
    assertEquals("1e05", reader.nextNumberLiteral());
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
  void rejectsMethodsCalledInWrongState() throws JsonReadException {
    assertReadExceptionCode(new JsonStreamReader("\"name\"")::nextName, "MJJBP-021");
    assertReadExceptionCode(new JsonStreamReader("[]")::beginObject, "MJJBP-019");
    assertReadExceptionCode(new JsonStreamReader("{}")::beginArray, "MJJBP-019");

    JsonStreamReader object = new JsonStreamReader("{\"id\":1}");
    object.beginObject();
    assertEquals("id", object.nextName());
    assertReadExceptionCode(object::endObject, "MJJBP-002");
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
  void fromReaderUsesChunkedReads() throws JsonReadException {
    CountingReader source = new CountingReader("\"chunked\"");
    JsonStreamReader reader = JsonStreamReader.fromReader("test.json", source);

    assertEquals("chunked", reader.nextString());

    assertTrue(source.readCallCount() <= 2);
    assertTrue(source.maxRequestedLength() > 1);
    assertTrue(source.maxReturnedLength() > 1);
  }

  @Test
  void streamsLargeArraysFromReaderIncrementally() throws JsonReadException {
    StringBuilder sourceBuilder = new StringBuilder("[");
    for (int i = 0; i < 4096; i++) {
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
    for (int i = 1; i < 4096; i++) {
      assertTrue(reader.hasNext());
      assertEquals(Integer.toString(i), reader.nextNumberLiteral());
    }
    assertFalse(reader.hasNext());
    reader.endArray();
    assertEquals(JsonToken.END_DOCUMENT, reader.peek());
  }

  @Test
  void streamsMediumObjectArrayDocumentFromReader() throws JsonReadException {
    String sourceText = mediumObjectArrayDocument(512);
    CountingReader source = new CountingReader(sourceText);
    JsonStreamReader reader = JsonStreamReader.fromReader("medium.json", source);
    int sum = 0;

    reader.beginObject();
    assertEquals("records", reader.nextName());
    reader.beginArray();
    assertTrue(reader.hasNext());
    reader.beginObject();
    assertEquals("id", reader.nextName());
    assertEquals("id-0", reader.nextString());
    assertEquals("value", reader.nextName());
    sum += Integer.parseInt(reader.nextNumberLiteral());
    assertEquals("active", reader.nextName());
    assertTrue(reader.nextBoolean());
    assertFalse(reader.hasNext());
    reader.endObject();
    assertTrue(source.readCount() < sourceText.length());
    for (int i = 1; i < 512; i++) {
      assertTrue(reader.hasNext());
      reader.beginObject();
      assertEquals("id", reader.nextName());
      assertEquals("id-" + i, reader.nextString());
      assertEquals("value", reader.nextName());
      sum += Integer.parseInt(reader.nextNumberLiteral());
      assertEquals("active", reader.nextName());
      assertEquals(i % 2 == 0, reader.nextBoolean());
      assertFalse(reader.hasNext());
      reader.endObject();
    }
    assertFalse(reader.hasNext());
    reader.endArray();
    assertFalse(reader.hasNext());
    reader.endObject();

    assertEquals(130816, sum);
    assertEquals(JsonToken.END_DOCUMENT, reader.peek());
  }

  @Test
  void readsLongReaderStringAcrossChunkBoundaries() throws JsonReadException {
    String prefix = "a".repeat(5000);
    JsonStreamReader reader =
        JsonStreamReader.fromReader(
            "long-string.json", new CountingReader("\"" + prefix + "\\u0042\""));

    assertEquals(prefix + "B", reader.nextString());
    assertEquals(JsonToken.END_DOCUMENT, reader.peek());
  }

  @Test
  void readsLongReaderNumberAcrossChunkBoundaries() throws JsonReadException {
    String number = "-1" + "2".repeat(5000) + ".25e+2";
    JsonStreamReader reader =
        JsonStreamReader.fromReader("long-number.json", new CountingReader(number));

    assertEquals(number, reader.nextNumberLiteral());
    assertEquals(JsonToken.END_DOCUMENT, reader.peek());
  }

  @Test
  void reportsReaderBackedInvalidJsonLocationsAcrossChunks() throws JsonReadException {
    String input = "{\"records\":[\n" + "{\"value\":\"" + "a".repeat(5000) + "\\x\"}\n]}";
    JsonStreamReader reader =
        JsonStreamReader.fromReader("reader-invalid.json", new CountingReader(input));

    reader.beginObject();
    assertEquals("records", reader.nextName());
    reader.beginArray();
    assertTrue(reader.hasNext());
    reader.beginObject();
    assertEquals("value", reader.nextName());
    JsonReadException exception = assertThrows(JsonReadException.class, reader::nextString);

    assertEquals("MJJBP-013", exception.diagnostic().code());
    assertEquals("reader-invalid.json", exception.diagnostic().location().sourceName());
    assertEquals(2, exception.diagnostic().location().lineNumber());
    assertEquals(5013, exception.diagnostic().location().columnNumber());
  }

  @Test
  void rejectsReaderThatReturnsZeroCharacters() {
    JsonStreamReader reader =
        JsonStreamReader.fromReader(
            "zero-read.json",
            new Reader() {
              @Override
              public int read(char[] buffer, int offset, int length) {
                return 0;
              }

              @Override
              public void close() {}
            });

    JsonReadException exception = assertThrows(JsonReadException.class, reader::peek);
    assertEquals("MJJBP-023", exception.diagnostic().code());
  }

  private static void assertReadExceptionCode(String input, String code) {
    assertReadExceptionCode(new JsonStreamReader(input)::skipValue, code);
  }

  private static void assertReadExceptionCode(ThrowingReaderAction action, String code) {
    JsonReadException exception = assertThrows(JsonReadException.class, action::run);
    assertEquals(code, exception.diagnostic().code());
  }

  private static String mediumObjectArrayDocument(int records) {
    StringBuilder builder = new StringBuilder("{\"records\":[");
    for (int i = 0; i < records; i++) {
      if (i > 0) {
        builder.append(',');
      }
      builder
          .append("{\"id\":\"id-")
          .append(i)
          .append("\",\"value\":")
          .append(i)
          .append(",\"active\":")
          .append(i % 2 == 0)
          .append('}');
    }
    builder.append("]}");
    return builder.toString();
  }

  @FunctionalInterface
  private interface ThrowingReaderAction {
    void run() throws JsonReadException;
  }

  private static final class CountingReader extends Reader {
    private final String source;
    private int index;
    private int readCount;
    private int readCallCount;
    private int maxRequestedLength;
    private int maxReturnedLength;

    private CountingReader(String source) {
      this.source = source;
    }

    @Override
    public int read(char[] buffer, int offset, int length) {
      readCallCount++;
      maxRequestedLength = Math.max(maxRequestedLength, length);
      if (index >= source.length()) {
        return -1;
      }
      int read = Math.min(length, source.length() - index);
      source.getChars(index, index + read, buffer, offset);
      index += read;
      readCount += read;
      maxReturnedLength = Math.max(maxReturnedLength, read);
      return read;
    }

    @Override
    public void close() {}

    private int readCount() {
      return readCount;
    }

    private int readCallCount() {
      return readCallCount;
    }

    private int maxRequestedLength() {
      return maxRequestedLength;
    }

    private int maxReturnedLength() {
      return maxReturnedLength;
    }
  }
}
