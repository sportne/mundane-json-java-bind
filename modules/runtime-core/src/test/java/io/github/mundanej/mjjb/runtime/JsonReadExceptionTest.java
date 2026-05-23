package io.github.mundanej.mjjb.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class JsonReadExceptionTest {
  @Test
  void exceptionMessageComesFromDiagnostic() {
    JsonDiagnostic diagnostic =
        JsonDiagnostic.error("MJJBT-001", "Bad value.", JsonPath.ROOT, JsonLocation.UNKNOWN);

    JsonReadException exception = new JsonReadException(diagnostic);

    assertEquals("Bad value.", exception.getMessage());
    assertSame(diagnostic, exception.diagnostic());
  }

  @Test
  void constructorRejectsNullDiagnostic() {
    assertThrows(NullPointerException.class, JsonReadExceptionTest::createWithNullDiagnostic);
  }

  private static JsonReadException createWithNullDiagnostic() {
    return new JsonReadException(null);
  }
}
