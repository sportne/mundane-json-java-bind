package io.github.mundanej.mjjb.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

final class SchemaBranchMetadataTest {
  @Test
  void constructorPreservesBranchMetadata() {
    SchemaObjectMetadata object =
        new SchemaObjectMetadata("/oneOf/0", "Card", SchemaAnnotations.EMPTY, List.of());

    SchemaBranchMetadata metadata = new SchemaBranchMetadata("card", "Card", object);

    assertEquals("card", metadata.tagValue());
    assertEquals("Card", metadata.javaTypeName());
    assertEquals(object, metadata.object());
  }

  @Test
  void constructorAllowsBlankTagValueBecauseSchemaConstMayBeBlank() {
    SchemaObjectMetadata object =
        new SchemaObjectMetadata("/oneOf/0", "BlankTag", SchemaAnnotations.EMPTY, List.of());

    assertEquals("", new SchemaBranchMetadata("", "BlankTag", object).tagValue());
  }

  @Test
  void constructorRejectsNullFieldsAndBlankJavaTypeName() {
    SchemaObjectMetadata object =
        new SchemaObjectMetadata("/oneOf/0", "Card", SchemaAnnotations.EMPTY, List.of());

    assertThrows(NullPointerException.class, () -> new SchemaBranchMetadata(null, "Card", object));
    assertThrows(NullPointerException.class, () -> new SchemaBranchMetadata("card", null, object));
    assertThrows(NullPointerException.class, () -> new SchemaBranchMetadata("card", "Card", null));
    assertThrows(
        IllegalArgumentException.class, () -> new SchemaBranchMetadata("card", "", object));
  }
}
