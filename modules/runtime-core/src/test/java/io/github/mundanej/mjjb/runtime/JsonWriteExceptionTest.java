package io.github.mundanej.mjjb.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class JsonWriteExceptionTest {
  @Test
  void constructorsPreserveMessageAndCause() {
    RuntimeException cause = new RuntimeException("cause");

    JsonWriteException exception = new JsonWriteException("Cannot write.");
    JsonWriteException caused = new JsonWriteException("Cannot write.", cause);

    assertEquals("Cannot write.", exception.getMessage());
    assertEquals("Cannot write.", caused.getMessage());
    assertSame(cause, caused.getCause());
  }

  @Test
  void constructorsRejectNullOrBlankMessages() {
    assertThrows(IllegalArgumentException.class, JsonWriteExceptionTest::createWithNullMessage);
    assertThrows(IllegalArgumentException.class, JsonWriteExceptionTest::createWithEmptyMessage);
    assertThrows(IllegalArgumentException.class, JsonWriteExceptionTest::createWithBlankMessage);
    assertThrows(
        IllegalArgumentException.class, JsonWriteExceptionTest::createWithNullMessageAndCause);
    assertThrows(
        IllegalArgumentException.class, JsonWriteExceptionTest::createWithEmptyMessageAndCause);
    assertThrows(
        IllegalArgumentException.class, JsonWriteExceptionTest::createWithBlankMessageAndCause);
  }

  private static JsonWriteException createWithNullMessage() {
    return new JsonWriteException(null);
  }

  private static JsonWriteException createWithEmptyMessage() {
    return new JsonWriteException("");
  }

  private static JsonWriteException createWithBlankMessage() {
    return new JsonWriteException(" \t\n");
  }

  private static JsonWriteException createWithNullMessageAndCause() {
    return new JsonWriteException(null, null);
  }

  private static JsonWriteException createWithEmptyMessageAndCause() {
    return new JsonWriteException("", null);
  }

  private static JsonWriteException createWithBlankMessageAndCause() {
    return new JsonWriteException(" \t\n", null);
  }
}
