package io.github.mundanej.mjjb.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SchemaObjectMetadataTest {
  @Test
  void constructorPreservesValuesAndDefensivelyCopiesProperties() {
    SchemaPropertyMetadata property =
        new SchemaPropertyMetadata(
            "id", "id", "/properties/id", true, "String", false, false, SchemaAnnotations.EMPTY);
    ArrayList<SchemaPropertyMetadata> properties = new ArrayList<>(List.of(property));

    SchemaObjectMetadata metadata =
        new SchemaObjectMetadata("", "GeneratedBindings", SchemaAnnotations.EMPTY, properties);
    properties.clear();

    assertEquals("", metadata.schemaPointer());
    assertEquals("GeneratedBindings", metadata.javaTypeName());
    assertEquals(SchemaAnnotations.EMPTY, metadata.annotations());
    assertEquals(List.of(property), metadata.properties());
    assertThrows(UnsupportedOperationException.class, () -> metadata.properties().clear());
  }

  @Test
  void constructorRejectsNullBlankAndInvalidValues() {
    assertThrows(
        NullPointerException.class,
        () ->
            new SchemaObjectMetadata(
                null, "GeneratedBindings", SchemaAnnotations.EMPTY, List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new SchemaObjectMetadata(
                "properties/id", "GeneratedBindings", SchemaAnnotations.EMPTY, List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new SchemaObjectMetadata("", "", SchemaAnnotations.EMPTY, List.of()));
    assertThrows(
        NullPointerException.class,
        () -> new SchemaObjectMetadata("", "GeneratedBindings", null, List.of()));
    assertThrows(
        NullPointerException.class,
        () -> new SchemaObjectMetadata("", "GeneratedBindings", SchemaAnnotations.EMPTY, null));
    assertThrows(
        NullPointerException.class,
        () ->
            new SchemaObjectMetadata(
                "", "GeneratedBindings", SchemaAnnotations.EMPTY, propertiesWithNull()));
  }

  private static List<SchemaPropertyMetadata> propertiesWithNull() {
    ArrayList<SchemaPropertyMetadata> properties = new ArrayList<>();
    properties.add(null);
    return properties;
  }
}
