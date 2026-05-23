package io.github.mundanej.mjjb.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class SchemaSyntaxDiagnosticTest {
  @Test
  void preservesConstructorFields() {
    JsonPointer pointer = JsonPointer.ROOT.property("type");

    SchemaSyntaxDiagnostic diagnostic =
        new SchemaSyntaxDiagnostic("invalid-json", "Expected a JSON object", pointer);

    assertEquals("invalid-json", diagnostic.code());
    assertEquals("Expected a JSON object", diagnostic.message());
    assertEquals(pointer, diagnostic.pointer());
  }

  @Test
  void rejectsNullCode() {
    NullPointerException exception =
        assertThrows(
            NullPointerException.class,
            () -> new SchemaSyntaxDiagnostic(null, "Expected a JSON object", JsonPointer.ROOT));

    assertEquals("code", exception.getMessage());
  }

  @Test
  void rejectsNullMessage() {
    NullPointerException exception =
        assertThrows(
            NullPointerException.class,
            () -> new SchemaSyntaxDiagnostic("invalid-json", null, JsonPointer.ROOT));

    assertEquals("message", exception.getMessage());
  }

  @Test
  void rejectsNullPointer() {
    NullPointerException exception =
        assertThrows(
            NullPointerException.class,
            () -> new SchemaSyntaxDiagnostic("invalid-json", "Expected a JSON object", null));

    assertEquals("pointer", exception.getMessage());
  }

  @Test
  void rejectsBlankCode() {
    IllegalArgumentException emptyException =
        assertThrows(
            IllegalArgumentException.class,
            () -> new SchemaSyntaxDiagnostic("", "Expected a JSON object", JsonPointer.ROOT));
    IllegalArgumentException whitespaceException =
        assertThrows(
            IllegalArgumentException.class,
            () -> new SchemaSyntaxDiagnostic(" \t", "Expected a JSON object", JsonPointer.ROOT));

    assertEquals("code must not be blank", emptyException.getMessage());
    assertEquals("code must not be blank", whitespaceException.getMessage());
  }

  @Test
  void rejectsBlankMessage() {
    IllegalArgumentException emptyException =
        assertThrows(
            IllegalArgumentException.class,
            () -> new SchemaSyntaxDiagnostic("invalid-json", "", JsonPointer.ROOT));
    IllegalArgumentException whitespaceException =
        assertThrows(
            IllegalArgumentException.class,
            () -> new SchemaSyntaxDiagnostic("invalid-json", " \t", JsonPointer.ROOT));

    assertEquals("message must not be blank", emptyException.getMessage());
    assertEquals("message must not be blank", whitespaceException.getMessage());
  }

  @Test
  void usesRecordEqualityAndHashCode() {
    JsonPointer pointer = JsonPointer.ROOT.property("type");
    SchemaSyntaxDiagnostic diagnostic =
        new SchemaSyntaxDiagnostic("invalid-json", "Expected a JSON object", pointer);
    SchemaSyntaxDiagnostic same =
        new SchemaSyntaxDiagnostic("invalid-json", "Expected a JSON object", pointer);
    SchemaSyntaxDiagnostic differentCode =
        new SchemaSyntaxDiagnostic("unexpected-token", "Expected a JSON object", pointer);
    SchemaSyntaxDiagnostic differentMessage =
        new SchemaSyntaxDiagnostic("invalid-json", "Unexpected token", pointer);
    SchemaSyntaxDiagnostic differentPointer =
        new SchemaSyntaxDiagnostic(
            "invalid-json", "Expected a JSON object", JsonPointer.ROOT.property("properties"));

    assertEquals(same, diagnostic);
    assertEquals(same.hashCode(), diagnostic.hashCode());
    assertNotEquals(differentCode, diagnostic);
    assertNotEquals(differentMessage, diagnostic);
    assertNotEquals(differentPointer, diagnostic);
  }

  @Test
  void includesFieldValuesInToString() {
    SchemaSyntaxDiagnostic diagnostic =
        new SchemaSyntaxDiagnostic(
            "invalid-json", "Expected a JSON object", JsonPointer.ROOT.property("type"));

    assertEquals(
        "SchemaSyntaxDiagnostic[code=invalid-json, message=Expected a JSON object, pointer=JsonPointer[value=/type]]",
        diagnostic.toString());
  }
}
