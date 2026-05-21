package io.github.mundanej.mjjb.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.JsonWriteException;
import org.junit.jupiter.api.Test;

final class ParserNativeSmokeTest {
  @Test
  void readsAndWritesJsonDeterministically() throws JsonReadException, JsonWriteException {
    JsonStreamReader reader = new JsonStreamReader("{\"id\":\"u-1\",\"values\":[1,true,null]}");

    reader.beginObject();
    assertTrue(reader.hasNext());
    assertEquals("id", reader.nextName());
    assertEquals("u-1", reader.nextString());
    assertTrue(reader.hasNext());
    assertEquals("values", reader.nextName());
    reader.beginArray();
    assertEquals("1", reader.nextNumberLiteral());
    assertTrue(reader.nextBoolean());
    reader.nextNull();
    assertFalse(reader.hasNext());
    reader.endArray();
    assertFalse(reader.hasNext());
    reader.endObject();

    JsonStringWriter writer = new JsonStringWriter();
    writer.beginObject();
    writer.name("id");
    writer.value("u-1");
    writer.name("ok");
    writer.value(true);
    writer.endObject();
    assertEquals("{\"id\":\"u-1\",\"ok\":true}", writer.json());
  }

  @Test
  void reportsDeterministicFailures() throws JsonReadException {
    JsonStreamReader reader = new JsonStreamReader("{\"bad\":\"\\x\"}");
    reader.beginObject();
    assertEquals("bad", reader.nextName());

    JsonReadException readException = assertThrows(JsonReadException.class, reader::nextString);
    JsonWriteException writeException =
        assertThrows(JsonWriteException.class, () -> new JsonStringWriter().number("01"));

    assertEquals("MJJBP-013", readException.diagnostic().code());
    assertEquals("Leading zeroes are not valid JSON numbers.", writeException.getMessage());
  }
}
