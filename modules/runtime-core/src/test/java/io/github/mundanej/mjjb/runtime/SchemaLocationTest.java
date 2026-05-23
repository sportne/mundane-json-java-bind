package io.github.mundanej.mjjb.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import org.junit.jupiter.api.Test;

final class SchemaLocationTest {
  @Test
  void unknownUsesStableSentinelValues() {
    assertEquals(URI.create("urn:mjjb:unknown"), SchemaLocation.UNKNOWN.schemaUri());
    assertEquals("", SchemaLocation.UNKNOWN.pointer());
  }

  @Test
  void constructorAcceptsEmptyOrSlashPrefixedPointers() {
    URI schemaUri = URI.create("https://example.test/schema");

    assertEquals("", new SchemaLocation(schemaUri, "").pointer());
    assertEquals("/properties/id", new SchemaLocation(schemaUri, "/properties/id").pointer());
  }

  @Test
  void constructorRejectsNullFieldsAndInvalidPointers() {
    URI schemaUri = URI.create("https://example.test/schema");

    assertThrows(NullPointerException.class, () -> new SchemaLocation(null, ""));
    assertThrows(NullPointerException.class, () -> new SchemaLocation(schemaUri, null));
    assertThrows(
        IllegalArgumentException.class, () -> new SchemaLocation(schemaUri, "properties/id"));
  }
}
