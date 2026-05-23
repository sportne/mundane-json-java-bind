package io.github.mundanej.mjjb.examples.basicrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjjb.examples.basicrecord.generated.BasicRecord;
import io.github.mundanej.mjjb.examples.basicrecord.generated.BasicRecordJsonValidator;
import io.github.mundanej.mjjb.examples.basicrecord.generated.BasicRecordJsonWriter;
import io.github.mundanej.mjjb.parser.JsonStringWriter;
import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.JsonWriteException;
import io.github.mundanej.mjjb.runtime.ValidationMode;
import io.github.mundanej.mjjb.runtime.ValidationResult;
import java.lang.reflect.Constructor;
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
    assertReadDiagnostic("{\"id\":\"user-1\",\"id\":\"user-2\",\"count\":2}", "MJJBR-003", "$.id");
    assertReadDiagnostic("{\"id\":\"user-1\",\"count\":2} true", "MJJBR-002", "$");
    assertReadDiagnostic("[]", "MJJBR-001", "$");
    assertReadDiagnostic(
        "{\"id\":\"user-1\",\"count\":2,\"displayName\":null}", "MJJBR-006", "$.displayName");
    assertReadDiagnostic("{\"id\":\"user-1\",\"count\":2,\"score\":null}", "MJJBR-008", "$.score");
    assertReadDiagnostic(
        "{\"id\":\"user-1\",\"count\":2,\"active\":null}", "MJJBR-009", "$.active");
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

  @Test
  void recordConstructorRejectsNullRequiredAndOptionalContainers() {
    assertThrows(
        NullPointerException.class,
        () -> new BasicRecord(null, 1L, Optional.empty(), Optional.empty(), Optional.empty()));
    assertThrows(
        NullPointerException.class,
        () -> new BasicRecord("user-1", 1L, null, Optional.empty(), Optional.empty()));
    assertThrows(
        NullPointerException.class,
        () -> new BasicRecord("user-1", 1L, Optional.empty(), null, Optional.empty()));
    assertThrows(
        NullPointerException.class,
        () -> new BasicRecord("user-1", 1L, Optional.empty(), Optional.empty(), null));
  }

  @Test
  void preservesOptionalAbsentAndPresentStates() throws JsonReadException, JsonWriteException {
    BasicRecord absent = BasicRecordExample.read("{\"id\":\"user-1\",\"count\":2}");
    assertEquals(
        new BasicRecord("user-1", 2L, Optional.empty(), Optional.empty(), Optional.empty()),
        absent);
    assertEquals("{\"id\":\"user-1\",\"count\":2}", BasicRecordExample.write(absent));

    BasicRecord present =
        BasicRecordExample.read(
            "{\"id\":\"user-1\",\"count\":2,\"displayName\":\"\",\"score\":0,\"active\":false}");
    assertEquals(
        new BasicRecord("user-1", 2L, Optional.of(""), Optional.of(0.0D), Optional.of(false)),
        present);
    assertEquals(
        "{\"id\":\"user-1\",\"count\":2,\"displayName\":\"\",\"score\":0.0,\"active\":false}",
        BasicRecordExample.write(present));
  }

  @Test
  void writerRejectsNonFiniteNumbersAndNullContainers() {
    assertThrows(
        JsonWriteException.class,
        () ->
            BasicRecordExample.write(
                new BasicRecord(
                    "user-1", 1L, Optional.empty(), Optional.of(Double.NaN), Optional.empty())));
    assertThrows(JsonWriteException.class, () -> BasicRecordExample.write(nonFiniteRecord()));
    assertThrows(NullPointerException.class, () -> BasicRecordExample.write(null));
    assertThrows(
        NullPointerException.class,
        () ->
            BasicRecordJsonWriter.write(new JsonStringWriter(), invalidRecordWithoutConstructor()));
  }

  @Test
  void validatorAccumulatesByDefaultAndCanFailFast() throws ReflectiveOperationException {
    BasicRecord invalid = invalidRecordWithoutConstructor();

    assertValidationDiagnostics(
        BasicRecordJsonValidator.validate(invalid),
        new String[] {"MJJBV-002", "MJJBV-003", "MJJBV-003", "MJJBV-003"},
        new String[] {"$.id", "$.displayName", "$.score", "$.active"});
    assertValidationDiagnostics(
        BasicRecordJsonValidator.validate(invalid, ValidationMode.FAIL_FAST),
        new String[] {"MJJBV-002"},
        new String[] {"$.id"});
  }

  private static void assertReadDiagnostic(String json, String code, String path) {
    JsonReadException exception =
        assertThrows(JsonReadException.class, () -> BasicRecordExample.read(json));
    assertEquals(code, exception.diagnostic().code());
    assertEquals(path, exception.diagnostic().path().value());
  }

  private static void assertValidationDiagnostics(
      ValidationResult result, String[] expectedCodes, String[] expectedPaths) {
    assertEquals(expectedCodes.length, result.errors().size());
    assertEquals(expectedPaths.length, result.errors().size());
    for (int index = 0; index < expectedCodes.length; index++) {
      assertEquals(expectedCodes[index], result.errors().get(index).code());
      assertEquals(expectedPaths[index], result.errors().get(index).path().value());
    }
  }

  private static BasicRecord nonFiniteRecord() {
    return new BasicRecord(
        "user-1", 1L, Optional.empty(), Optional.of(Double.POSITIVE_INFINITY), Optional.empty());
  }

  private static BasicRecord invalidRecordWithoutConstructor() throws ReflectiveOperationException {
    Class<?> factoryType = Class.forName("sun.reflect.ReflectionFactory");
    Object factory = factoryType.getMethod("getReflectionFactory").invoke(null);
    Constructor<Object> objectConstructor = Object.class.getDeclaredConstructor();
    Constructor<?> constructor =
        (Constructor<?>)
            factoryType
                .getMethod("newConstructorForSerialization", Class.class, Constructor.class)
                .invoke(factory, BasicRecord.class, objectConstructor);
    constructor.setAccessible(true);
    return (BasicRecord) constructor.newInstance();
  }
}
