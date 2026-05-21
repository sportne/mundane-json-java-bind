package io.github.mundanej.mjjb.examples.basicrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjjb.examples.basicrecord.generated.BasicRecord;
import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.JsonWriteException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class BasicRecordNativeSmokeTest {
  @Test
  void exercisesGeneratedBasicRecordBinding() throws JsonReadException, JsonWriteException {
    BasicRecord record =
        BasicRecordExample.read(
            "{\"active\":true,\"score\":4.5,\"count\":2,\"displayName\":\"Ada\",\"id\":\"user-1\"}");

    assertEquals(
        new BasicRecord("user-1", 2L, Optional.of("Ada"), Optional.of(4.5D), Optional.of(true)),
        record);
    assertTrue(BasicRecordExample.validate(record).isValid());
    assertEquals(
        "{\"id\":\"user-1\",\"count\":2,\"displayName\":\"Ada\",\"score\":4.5,\"active\":true}",
        BasicRecordExample.write(record));
  }

  @Test
  void reportsGeneratedReaderDiagnostic() {
    JsonReadException exception =
        assertThrows(JsonReadException.class, () -> BasicRecordExample.read("{\"count\":2}"));

    assertEquals("MJJBR-005", exception.diagnostic().code());
    assertEquals("$.id", exception.diagnostic().path().value());
  }
}
