package io.github.mundanej.mjjb.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class SchemaRootMetadataTest {
  @Test
  void constructorPreservesValuesAndDefensivelyCopiesBranches() {
    SchemaObjectMetadata object =
        new SchemaObjectMetadata("", "GeneratedBindings", SchemaAnnotations.EMPTY, List.of());
    SchemaBranchMetadata branch = new SchemaBranchMetadata("card", "Card", object);
    ArrayList<SchemaBranchMetadata> branches = new ArrayList<>(List.of(branch));

    SchemaRootMetadata metadata =
        new SchemaRootMetadata(
            "https://json-schema.org/draft/2020-12/schema",
            "GeneratedBindings",
            object,
            Optional.of("kind"),
            branches);
    branches.clear();

    assertEquals("https://json-schema.org/draft/2020-12/schema", metadata.dialect());
    assertEquals("GeneratedBindings", metadata.rootTypeName());
    assertEquals(object, metadata.rootObject());
    assertEquals("kind", metadata.tagPropertyName().orElseThrow());
    assertEquals(List.of(branch), metadata.branches());
    assertThrows(UnsupportedOperationException.class, () -> metadata.branches().clear());
  }

  @Test
  void constructorRejectsNullBlankAndNullBranchValues() {
    SchemaObjectMetadata object =
        new SchemaObjectMetadata("", "GeneratedBindings", SchemaAnnotations.EMPTY, List.of());

    assertThrows(
        NullPointerException.class,
        () ->
            new SchemaRootMetadata(null, "GeneratedBindings", object, Optional.empty(), List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new SchemaRootMetadata("", "GeneratedBindings", object, Optional.empty(), List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new SchemaRootMetadata("dialect", "", object, Optional.empty(), List.of()));
    assertThrows(
        NullPointerException.class,
        () ->
            new SchemaRootMetadata(
                "dialect", "GeneratedBindings", null, Optional.empty(), List.of()));
    assertThrows(
        NullPointerException.class,
        () -> new SchemaRootMetadata("dialect", "GeneratedBindings", object, null, List.of()));
    assertThrows(
        NullPointerException.class,
        () ->
            new SchemaRootMetadata("dialect", "GeneratedBindings", object, Optional.empty(), null));
    assertThrows(
        NullPointerException.class,
        () ->
            new SchemaRootMetadata(
                "dialect", "GeneratedBindings", object, Optional.empty(), branchesWithNull()));
  }

  private static List<SchemaBranchMetadata> branchesWithNull() {
    ArrayList<SchemaBranchMetadata> branches = new ArrayList<>();
    branches.add(null);
    return branches;
  }
}
