package io.github.mundanej.mjjb.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class JsonLocationTest {
  @Test
  void unknownUsesStableSentinelValues() {
    assertEquals("", JsonLocation.UNKNOWN.sourceName());
    assertEquals(-1L, JsonLocation.UNKNOWN.offset());
    assertEquals(-1, JsonLocation.UNKNOWN.lineNumber());
    assertEquals(-1, JsonLocation.UNKNOWN.columnNumber());
  }

  @Test
  void constructorPreservesKnownCoordinates() {
    JsonLocation location = new JsonLocation("sample.json", 12L, 3, 9);

    assertEquals("sample.json", location.sourceName());
    assertEquals(12L, location.offset());
    assertEquals(3, location.lineNumber());
    assertEquals(9, location.columnNumber());
  }

  @Test
  void constructorRejectsInvalidCoordinates() {
    assertThrows(NullPointerException.class, () -> new JsonLocation(null, 0L, 1, 1));
    assertThrows(IllegalArgumentException.class, () -> new JsonLocation("sample.json", -2L, 1, 1));
    assertThrows(IllegalArgumentException.class, () -> new JsonLocation("sample.json", 0L, 0, 1));
    assertThrows(IllegalArgumentException.class, () -> new JsonLocation("sample.json", 0L, -2, 1));
    assertThrows(IllegalArgumentException.class, () -> new JsonLocation("sample.json", 0L, 1, 0));
    assertThrows(IllegalArgumentException.class, () -> new JsonLocation("sample.json", 0L, 1, -2));
  }
}
