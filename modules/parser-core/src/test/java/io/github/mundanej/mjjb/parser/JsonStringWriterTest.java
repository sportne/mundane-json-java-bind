package io.github.mundanej.mjjb.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjjb.runtime.JsonWriteException;
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
  void rejectsInvalidWriterStructure() throws JsonWriteException {
    JsonStringWriter writer = new JsonStringWriter();

    writer.beginObject();
    assertThrows(JsonWriteException.class, writer::json);
    assertThrows(JsonWriteException.class, () -> writer.value("missing-name"));
    writer.name("id");
    assertThrows(JsonWriteException.class, writer::endObject);
  }

  @Test
  void rejectsMultipleRootValues() throws JsonWriteException {
    JsonStringWriter writer = new JsonStringWriter();

    writer.value("one");
    assertEquals("\"one\"", writer.json());
    assertThrows(JsonWriteException.class, () -> writer.value("two"));
  }
}
