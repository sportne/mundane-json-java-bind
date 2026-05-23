package io.github.mundanej.mjjb.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjjb.runtime.JsonWriteException;
import java.util.List;
import org.junit.jupiter.api.Test;

final class JsonStringWriterTest {
  @Test
  void writesDeterministicJson() throws JsonWriteException {
    JsonStringWriter writer = new JsonStringWriter();

    writer.beginObject();
    writer.name("id");
    writer.value("u-1");
    writer.name("values");
    writer.beginArray();
    writer.number("1");
    writer.nullValue();
    writer.endArray();
    writer.endObject();

    assertEquals("{\"id\":\"u-1\",\"values\":[1,null]}", writer.json());
  }

  @Test
  void writesNestedObjectsArraysBooleansNullsAndNumbers() throws JsonWriteException {
    JsonStringWriter writer = new JsonStringWriter();

    writer.beginObject();
    writer.name("first");
    writer.beginArray();
    writer.number("0");
    writer.number("-1");
    writer.number("12");
    writer.number("3.50");
    writer.number("6e7");
    writer.number("-8.25E-3");
    writer.endArray();
    writer.name("second");
    writer.beginObject();
    writer.name("enabled");
    writer.value(true);
    writer.name("missing");
    writer.nullValue();
    writer.endObject();
    writer.endObject();

    assertEquals(
        "{\"first\":[0,-1,12,3.50,6e7,-8.25E-3],\"second\":{\"enabled\":true,\"missing\":null}}",
        writer.json());
  }

  @Test
  void escapesStringsDeterministically() throws JsonWriteException {
    JsonStringWriter writer = new JsonStringWriter();

    writer.beginArray();
    writer.value("\"\\\b\f\n\r\t");
    writer.value("control-\u0001");
    writer.value("unicode-\u20ac");
    writer.endArray();

    assertEquals(
        "[\"\\\"\\\\\\b\\f\\n\\r\\t\",\"control-\\u0001\",\"unicode-\u20ac\"]", writer.json());
  }

  @Test
  void rejectsInvalidWriterStructure() throws JsonWriteException {
    JsonStringWriter writer = new JsonStringWriter();

    writer.beginObject();
    assertThrows(JsonWriteException.class, writer::json);
    assertThrows(JsonWriteException.class, () -> writer.value("missing-name"));
    writer.name("id");
    assertThrows(JsonWriteException.class, writer::endObject);
  }

  @Test
  void rejectsAdditionalInvalidWriterStructures() throws JsonWriteException {
    JsonStringWriter nameOutsideObject = new JsonStringWriter();
    assertThrows(JsonWriteException.class, () -> nameOutsideObject.name("id"));

    JsonStringWriter nameBeforeValue = new JsonStringWriter();
    nameBeforeValue.beginObject();
    nameBeforeValue.name("id");
    assertThrows(JsonWriteException.class, () -> nameBeforeValue.name("other"));

    JsonStringWriter wrongContainerEnd = new JsonStringWriter();
    wrongContainerEnd.beginArray();
    assertThrows(JsonWriteException.class, wrongContainerEnd::endObject);

    JsonStringWriter objectValueBeforeName = new JsonStringWriter();
    objectValueBeforeName.beginObject();
    assertThrows(JsonWriteException.class, () -> objectValueBeforeName.value("missing-name"));
  }

  @Test
  void rejectsInvalidNumberLiterals() {
    List<String> invalidNumbers =
        List.of("", "-", "01", "-01", "1.", ".1", "1e", "1e+", "NaN", "Infinity");

    for (String invalidNumber : invalidNumbers) {
      JsonStringWriter writer = new JsonStringWriter();
      assertThrows(JsonWriteException.class, () -> writer.number(invalidNumber), invalidNumber);
    }
  }

  @Test
  void writesEmptyAndScalarRootValues() throws JsonWriteException {
    JsonStringWriter objectWriter = new JsonStringWriter();
    objectWriter.beginObject();
    objectWriter.endObject();
    assertEquals("{}", objectWriter.json());
    assertEquals("{}", objectWriter.json());

    JsonStringWriter arrayWriter = new JsonStringWriter();
    arrayWriter.beginArray();
    arrayWriter.endArray();
    assertEquals("[]", arrayWriter.json());

    JsonStringWriter booleanWriter = new JsonStringWriter();
    booleanWriter.value(false);
    assertEquals("false", booleanWriter.json());

    JsonStringWriter numberWriter = new JsonStringWriter();
    numberWriter.number("-0");
    assertEquals("-0", numberWriter.json());

    JsonStringWriter nullWriter = new JsonStringWriter();
    nullWriter.nullValue();
    assertEquals("null", nullWriter.json());
  }

  @Test
  void rejectsNullNamesStringsAndNumberLiterals() {
    JsonStringWriter writer = new JsonStringWriter();

    assertThrows(NullPointerException.class, () -> writer.name(null));
    assertThrows(NullPointerException.class, () -> writer.value(null));
    assertThrows(NullPointerException.class, () -> writer.number(null));
  }

  @Test
  void rejectsMultipleRootValues() throws JsonWriteException {
    JsonStringWriter writer = new JsonStringWriter();

    writer.value("one");
    assertEquals("\"one\"", writer.json());
    assertThrows(JsonWriteException.class, () -> writer.value("two"));
  }
}
