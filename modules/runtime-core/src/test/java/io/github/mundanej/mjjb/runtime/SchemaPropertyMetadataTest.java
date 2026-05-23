package io.github.mundanej.mjjb.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class SchemaPropertyMetadataTest {
  @Test
  void constructorPreservesPropertyMetadata() {
    SchemaPropertyMetadata metadata =
        new SchemaPropertyMetadata(
            "id", "id", "/properties/id", true, "String", false, false, SchemaAnnotations.EMPTY);

    assertEquals("id", metadata.jsonName());
    assertEquals("id", metadata.javaFieldName());
    assertEquals("/properties/id", metadata.schemaPointer());
    assertTrue(metadata.required());
    assertEquals("String", metadata.javaType());
    assertFalse(metadata.nullable());
    assertFalse(metadata.array());
    assertEquals(SchemaAnnotations.EMPTY, metadata.annotations());
  }

  @Test
  void constructorAcceptsEmptyRootPointer() {
    SchemaPropertyMetadata metadata =
        new SchemaPropertyMetadata(
            "root", "root", "", false, "String", true, false, SchemaAnnotations.EMPTY);

    assertEquals("", metadata.schemaPointer());
  }

  @Test
  void constructorRejectsNullBlankAndInvalidPointerValues() {
    assertThrows(
        NullPointerException.class,
        () ->
            new SchemaPropertyMetadata(
                null, "id", "", true, "String", false, false, SchemaAnnotations.EMPTY));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new SchemaPropertyMetadata(
                "", "id", "", true, "String", false, false, SchemaAnnotations.EMPTY));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new SchemaPropertyMetadata(
                "id", "", "", true, "String", false, false, SchemaAnnotations.EMPTY));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new SchemaPropertyMetadata(
                "id",
                "id",
                "properties/id",
                true,
                "String",
                false,
                false,
                SchemaAnnotations.EMPTY));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new SchemaPropertyMetadata(
                "id", "id", "", true, "", false, false, SchemaAnnotations.EMPTY));
    assertThrows(
        NullPointerException.class,
        () -> new SchemaPropertyMetadata("id", "id", "", true, "String", false, false, null));
  }
}
