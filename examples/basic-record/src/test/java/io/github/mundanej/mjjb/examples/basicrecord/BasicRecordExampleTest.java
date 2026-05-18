package io.github.mundanej.mjjb.examples.basicrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjjb.examples.basicrecord.generated.BasicRecord;
import io.github.mundanej.mjjb.examples.basicrecord.generated.BasicRecordJsonValidator;
import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.JsonWriteException;
import io.github.mundanej.mjjb.runtime.ValidationResult;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class BasicRecordExampleTest {
  @Test
  void readsValidatesAndWritesBasicRecord() throws JsonReadException, JsonWriteException {
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
  void normalizesRequiredOnlyRecord() throws JsonReadException, JsonWriteException {
    assertEquals(
        "{\"id\":\"user-2\",\"count\":3}",
        BasicRecordExample.normalize("{\"count\":3,\"id\":\"user-2\"}"));
  }

  @Test
  void reportsDeterministicReadDiagnostics() {
    assertReadDiagnostic("{\"id\":\"user-1\",\"count\":2,\"extra\":true}", "MJJBR-004", "$.extra");
    assertReadDiagnostic("{\"count\":2}", "MJJBR-005", "$.id");
    assertReadDiagnostic("{\"id\":\"user-1\",\"count\":\"2\"}", "MJJBR-007", "$.count");
  }

  @Test
  void reportsDeterministicValidationDiagnostics() {
    ValidationResult result =
        BasicRecordJsonValidator.validate(
            new BasicRecord(
                "bad",
                1L,
                Optional.empty(),
                Optional.of(Double.POSITIVE_INFINITY),
                Optional.empty()));

    assertEquals("MJJBV-004", result.errors().getFirst().code());
    assertEquals("$.score", result.errors().getFirst().path().value());
  }

  private static void assertReadDiagnostic(String json, String code, String path) {
    JsonReadException exception =
        assertThrows(JsonReadException.class, () -> BasicRecordExample.read(json));
    assertEquals(code, exception.diagnostic().code());
    assertEquals(path, exception.diagnostic().path().value());
  }
}
