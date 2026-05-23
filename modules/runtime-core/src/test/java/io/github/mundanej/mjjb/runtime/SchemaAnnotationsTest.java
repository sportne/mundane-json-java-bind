package io.github.mundanej.mjjb.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class SchemaAnnotationsTest {
  @Test
  void emptyUsesAbsentOptionalsAndNoExamples() {
    assertTrue(SchemaAnnotations.EMPTY.title().isEmpty());
    assertTrue(SchemaAnnotations.EMPTY.description().isEmpty());
    assertTrue(SchemaAnnotations.EMPTY.comment().isEmpty());
    assertTrue(SchemaAnnotations.EMPTY.examplesJson().isEmpty());
    assertTrue(SchemaAnnotations.EMPTY.deprecated().isEmpty());
    assertTrue(SchemaAnnotations.EMPTY.readOnly().isEmpty());
    assertTrue(SchemaAnnotations.EMPTY.writeOnly().isEmpty());
    assertTrue(SchemaAnnotations.EMPTY.defaultJson().isEmpty());
  }

  @Test
  void constructorPreservesValuesAndDefensivelyCopiesExamples() {
    ArrayList<String> examples = new ArrayList<>(List.of("{\"id\":1}"));

    SchemaAnnotations annotations =
        new SchemaAnnotations(
            Optional.of("Title"),
            Optional.of("Description"),
            Optional.of("Comment"),
            examples,
            Optional.of(true),
            Optional.of(false),
            Optional.of(true),
            Optional.of("\"default\""));
    examples.add("{\"id\":2}");

    assertEquals("Title", annotations.title().orElseThrow());
    assertEquals("Description", annotations.description().orElseThrow());
    assertEquals("Comment", annotations.comment().orElseThrow());
    assertEquals(List.of("{\"id\":1}"), annotations.examplesJson());
    assertEquals(true, annotations.deprecated().orElseThrow());
    assertEquals(false, annotations.readOnly().orElseThrow());
    assertEquals(true, annotations.writeOnly().orElseThrow());
    assertEquals("\"default\"", annotations.defaultJson().orElseThrow());
    assertThrows(UnsupportedOperationException.class, () -> annotations.examplesJson().add("{}"));
  }

  @Test
  void constructorRejectsNullFieldsAndNullExampleElements() {
    assertThrows(
        NullPointerException.class,
        () ->
            new SchemaAnnotations(
                null,
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()));
    assertThrows(
        NullPointerException.class,
        () ->
            new SchemaAnnotations(
                Optional.empty(),
                null,
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()));
    assertThrows(
        NullPointerException.class,
        () ->
            new SchemaAnnotations(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                null,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()));
    assertThrows(
        NullPointerException.class,
        () ->
            new SchemaAnnotations(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                listWithNullExample(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()));
  }

  private static List<String> listWithNullExample() {
    ArrayList<String> values = new ArrayList<>();
    values.add(null);
    return values;
  }
}
