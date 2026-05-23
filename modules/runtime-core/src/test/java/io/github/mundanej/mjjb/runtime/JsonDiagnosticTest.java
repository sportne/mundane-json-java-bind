package io.github.mundanej.mjjb.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import org.junit.jupiter.api.Test;

final class JsonDiagnosticTest {
  @Test
  void errorFactoryDefaultsToErrorSeverityAndUnknownSchemaLocation() {
    JsonPath path = JsonPath.ROOT.property("name");
    JsonLocation location = new JsonLocation("person.json", 12L, 2, 5);

    JsonDiagnostic diagnostic =
        JsonDiagnostic.error("MJJBT-001", "Name is required.", path, location);

    assertEquals("MJJBT-001", diagnostic.code());
    assertEquals(JsonDiagnosticSeverity.ERROR, diagnostic.severity());
    assertEquals("Name is required.", diagnostic.message());
    assertEquals(path, diagnostic.path());
    assertEquals(location, diagnostic.location());
    assertEquals(SchemaLocation.UNKNOWN, diagnostic.schemaLocation());
  }

  @Test
  void constructorPreservesAllDiagnosticFields() {
    JsonPath path = JsonPath.ROOT.property("items").index(1);
    JsonLocation location = new JsonLocation("payload.json", 42L, 4, 9);
    SchemaLocation schemaLocation =
        new SchemaLocation(URI.create("https://example.test/schema.json"), "/properties/items");

    JsonDiagnostic diagnostic =
        new JsonDiagnostic(
            "MJJBT-002",
            JsonDiagnosticSeverity.WARNING,
            "Item may be ignored.",
            path,
            location,
            schemaLocation);

    assertEquals("MJJBT-002", diagnostic.code());
    assertEquals(JsonDiagnosticSeverity.WARNING, diagnostic.severity());
    assertEquals("Item may be ignored.", diagnostic.message());
    assertEquals(path, diagnostic.path());
    assertEquals(location, diagnostic.location());
    assertEquals(schemaLocation, diagnostic.schemaLocation());
  }

  @Test
  void constructorRejectsBlankCodeAndMessage() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new JsonDiagnostic(
                "",
                JsonDiagnosticSeverity.ERROR,
                "Message.",
                JsonPath.ROOT,
                JsonLocation.UNKNOWN,
                SchemaLocation.UNKNOWN));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new JsonDiagnostic(
                "   ",
                JsonDiagnosticSeverity.ERROR,
                "Message.",
                JsonPath.ROOT,
                JsonLocation.UNKNOWN,
                SchemaLocation.UNKNOWN));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new JsonDiagnostic(
                "MJJBT-003",
                JsonDiagnosticSeverity.ERROR,
                "",
                JsonPath.ROOT,
                JsonLocation.UNKNOWN,
                SchemaLocation.UNKNOWN));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new JsonDiagnostic(
                "MJJBT-003",
                JsonDiagnosticSeverity.ERROR,
                "   ",
                JsonPath.ROOT,
                JsonLocation.UNKNOWN,
                SchemaLocation.UNKNOWN));
  }

  @Test
  void constructorRejectsNullFields() {
    assertThrows(
        NullPointerException.class,
        () ->
            new JsonDiagnostic(
                null,
                JsonDiagnosticSeverity.ERROR,
                "Message.",
                JsonPath.ROOT,
                JsonLocation.UNKNOWN,
                SchemaLocation.UNKNOWN));
    assertThrows(
        NullPointerException.class,
        () ->
            new JsonDiagnostic(
                "MJJBT-004",
                null,
                "Message.",
                JsonPath.ROOT,
                JsonLocation.UNKNOWN,
                SchemaLocation.UNKNOWN));
    assertThrows(
        NullPointerException.class,
        () ->
            new JsonDiagnostic(
                "MJJBT-004",
                JsonDiagnosticSeverity.ERROR,
                null,
                JsonPath.ROOT,
                JsonLocation.UNKNOWN,
                SchemaLocation.UNKNOWN));
    assertThrows(
        NullPointerException.class,
        () ->
            new JsonDiagnostic(
                "MJJBT-004",
                JsonDiagnosticSeverity.ERROR,
                "Message.",
                null,
                JsonLocation.UNKNOWN,
                SchemaLocation.UNKNOWN));
    assertThrows(
        NullPointerException.class,
        () ->
            new JsonDiagnostic(
                "MJJBT-004",
                JsonDiagnosticSeverity.ERROR,
                "Message.",
                JsonPath.ROOT,
                null,
                SchemaLocation.UNKNOWN));
    assertThrows(
        NullPointerException.class,
        () ->
            new JsonDiagnostic(
                "MJJBT-004",
                JsonDiagnosticSeverity.ERROR,
                "Message.",
                JsonPath.ROOT,
                JsonLocation.UNKNOWN,
                null));
  }
}
