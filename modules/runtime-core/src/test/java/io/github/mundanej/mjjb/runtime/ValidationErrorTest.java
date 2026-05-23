package io.github.mundanej.mjjb.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import org.junit.jupiter.api.Test;

final class ValidationErrorTest {
  @Test
  void factoryPreservesCodeMessageAndPathWithUnknownLocations() {
    JsonPath path = JsonPath.ROOT.property("name");

    ValidationError error = ValidationError.of("MJJBT-001", "Name is required.", path);

    assertEquals("MJJBT-001", error.code());
    assertEquals("Name is required.", error.message());
    assertSame(path, error.path());
    assertEquals(JsonLocation.UNKNOWN, error.location());
    assertEquals(SchemaLocation.UNKNOWN, error.schemaLocation());
  }

  @Test
  void constructorPreservesAllFields() {
    JsonPath path = JsonPath.ROOT.property("items").index(2);
    JsonLocation location = new JsonLocation("example.json", 42L, 3, 9);
    SchemaLocation schemaLocation =
        new SchemaLocation(URI.create("https://example.test/schema"), "/properties/items");

    ValidationError error =
        new ValidationError(
            "MJJBT-002", "Item does not match schema.", path, location, schemaLocation);

    assertEquals("MJJBT-002", error.code());
    assertEquals("Item does not match schema.", error.message());
    assertSame(path, error.path());
    assertSame(location, error.location());
    assertSame(schemaLocation, error.schemaLocation());
  }

  @Test
  void constructorRejectsBlankCode() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ValidationError(
                "", "Broken value.", JsonPath.ROOT, JsonLocation.UNKNOWN, SchemaLocation.UNKNOWN));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ValidationError(
                " \t\n",
                "Broken value.",
                JsonPath.ROOT,
                JsonLocation.UNKNOWN,
                SchemaLocation.UNKNOWN));
  }

  @Test
  void constructorRejectsBlankMessage() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ValidationError(
                "MJJBT-001", "", JsonPath.ROOT, JsonLocation.UNKNOWN, SchemaLocation.UNKNOWN));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ValidationError(
                "MJJBT-001", " \t\n", JsonPath.ROOT, JsonLocation.UNKNOWN, SchemaLocation.UNKNOWN));
  }

  @Test
  void constructorRejectsNullFields() {
    assertThrows(
        NullPointerException.class,
        () ->
            new ValidationError(
                null,
                "Broken value.",
                JsonPath.ROOT,
                JsonLocation.UNKNOWN,
                SchemaLocation.UNKNOWN));
    assertThrows(
        NullPointerException.class,
        () ->
            new ValidationError(
                "MJJBT-001", null, JsonPath.ROOT, JsonLocation.UNKNOWN, SchemaLocation.UNKNOWN));
    assertThrows(
        NullPointerException.class,
        () ->
            new ValidationError(
                "MJJBT-001", "Broken value.", null, JsonLocation.UNKNOWN, SchemaLocation.UNKNOWN));
    assertThrows(
        NullPointerException.class,
        () ->
            new ValidationError(
                "MJJBT-001", "Broken value.", JsonPath.ROOT, null, SchemaLocation.UNKNOWN));
    assertThrows(
        NullPointerException.class,
        () ->
            new ValidationError(
                "MJJBT-001", "Broken value.", JsonPath.ROOT, JsonLocation.UNKNOWN, null));
  }
}
