package io.github.mundanej.mjjb.generator.core.internal.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

final class FieldValueTypeTest {
  @Test
  void scalarFactoryUsesRequiredPrimitiveOrReferenceTypeAndOptionalAccessorType() {
    assertScalarTypes(JavaScalarType.STRING, "String", "Optional<String>", "String");
    assertScalarTypes(JavaScalarType.INTEGER, "long", "Optional<Long>", "Long");
    assertScalarTypes(JavaScalarType.NUMBER, "double", "Optional<Double>", "Double");
    assertScalarTypes(JavaScalarType.BOOLEAN, "boolean", "Optional<Boolean>", "Boolean");
  }

  @Test
  void scalarFactoryPreservesFacetAndLiteralConstraints() {
    FacetConstraints facets =
        new FacetConstraints(
            OptionalLong.of(1),
            OptionalLong.of(12),
            Optional.of("^[a-z]+$"),
            Optional.of("email"),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());
    LiteralConstraints literals =
        new LiteralConstraints(
            List.of(new LiteralValue(LiteralValue.Kind.STRING, "active")),
            Optional.of(new LiteralValue(LiteralValue.Kind.STRING, "active")),
            Optional.of(new LiteralValue(LiteralValue.Kind.STRING, "active")));

    FieldValueType valueType = FieldValueType.scalar(JavaScalarType.STRING, facets, literals);

    assertEquals(JavaScalarType.STRING, valueType.scalarType());
    assertFalse(valueType.array());
    assertFalse(valueType.nullable());
    assertTrue(valueType.minItems().isEmpty());
    assertTrue(valueType.maxItems().isEmpty());
    assertSame(facets, valueType.facets());
    assertSame(literals, valueType.literals());
  }

  @Test
  void scalarFactoryOverloadsUseEmptyFacetAndLiteralConstraints() {
    FieldValueType defaultScalar = FieldValueType.scalar(JavaScalarType.STRING);
    FieldValueType scalarWithFacets =
        FieldValueType.scalar(JavaScalarType.STRING, FacetConstraints.EMPTY);

    assertSame(FacetConstraints.EMPTY, defaultScalar.facets());
    assertSame(LiteralConstraints.EMPTY, defaultScalar.literals());
    assertSame(FacetConstraints.EMPTY, scalarWithFacets.facets());
    assertSame(LiteralConstraints.EMPTY, scalarWithFacets.literals());
  }

  @Test
  void nullableScalarFactoryWrapsRequiredAndOptionalTypesInJsonField() {
    LiteralConstraints literals =
        new LiteralConstraints(
            List.of(new LiteralValue(LiteralValue.Kind.INTEGER, "7")),
            Optional.empty(),
            Optional.empty());

    FieldValueType valueType =
        FieldValueType.nullableScalar(JavaScalarType.INTEGER, FacetConstraints.EMPTY, literals);

    assertEquals(JavaScalarType.INTEGER, valueType.scalarType());
    assertFalse(valueType.array());
    assertTrue(valueType.nullable());
    assertEquals("JsonField<Long>", valueType.requiredJavaType());
    assertEquals("JsonField<Long>", valueType.optionalJavaType());
    assertEquals("Long", valueType.nullableValueJavaType());
    assertSame(FacetConstraints.EMPTY, valueType.facets());
    assertSame(literals, valueType.literals());
  }

  @Test
  void arrayFactoryUsesListForRequiredTypeAndOptionalListForOptionalAccessorType() {
    FieldValueType valueType =
        FieldValueType.array(JavaScalarType.NUMBER, OptionalLong.of(1), OptionalLong.of(3));

    assertEquals(JavaScalarType.NUMBER, valueType.scalarType());
    assertTrue(valueType.array());
    assertFalse(valueType.nullable());
    assertEquals(1L, valueType.minItems().orElseThrow());
    assertEquals(3L, valueType.maxItems().orElseThrow());
    assertFalse(valueType.uniqueItems());
    assertEquals("List<Double>", valueType.requiredJavaType());
    assertEquals("Optional<List<Double>>", valueType.optionalJavaType());
    assertEquals("List<Double>", valueType.nullableValueJavaType());
    assertSame(FacetConstraints.EMPTY, valueType.facets());
    assertSame(LiteralConstraints.EMPTY, valueType.literals());
  }

  @Test
  void arrayFactoryOverloadsPreserveFacetAndLiteralConstraints() {
    FacetConstraints facets =
        new FacetConstraints(
            OptionalLong.empty(),
            OptionalLong.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of("1"),
            Optional.of("10"),
            Optional.empty(),
            Optional.empty());
    LiteralConstraints literals =
        new LiteralConstraints(
            List.of(new LiteralValue(LiteralValue.Kind.NUMBER, "1.5")),
            Optional.empty(),
            Optional.empty());

    FieldValueType arrayWithFacets =
        FieldValueType.array(
            JavaScalarType.NUMBER, OptionalLong.empty(), OptionalLong.of(2), facets);
    FieldValueType arrayWithFacetsAndLiterals =
        FieldValueType.array(
            JavaScalarType.NUMBER, OptionalLong.of(1), OptionalLong.of(2), facets, literals);

    assertSame(facets, arrayWithFacets.facets());
    assertSame(LiteralConstraints.EMPTY, arrayWithFacets.literals());
    assertEquals(2L, arrayWithFacets.maxItems().orElseThrow());
    assertSame(facets, arrayWithFacetsAndLiterals.facets());
    assertSame(literals, arrayWithFacetsAndLiterals.literals());
    assertEquals(1L, arrayWithFacetsAndLiterals.minItems().orElseThrow());
    assertEquals(2L, arrayWithFacetsAndLiterals.maxItems().orElseThrow());
  }

  @Test
  void nullableArrayFactoryWrapsListTypeInJsonFieldForRequiredAndOptionalAccessors() {
    LiteralConstraints literals =
        new LiteralConstraints(
            List.of(new LiteralValue(LiteralValue.Kind.BOOLEAN, "true")),
            Optional.empty(),
            Optional.empty());

    FieldValueType valueType =
        FieldValueType.nullableArray(
            JavaScalarType.BOOLEAN,
            OptionalLong.of(1),
            OptionalLong.of(5),
            FacetConstraints.EMPTY,
            literals);

    assertEquals(JavaScalarType.BOOLEAN, valueType.scalarType());
    assertTrue(valueType.array());
    assertTrue(valueType.nullable());
    assertEquals(1L, valueType.minItems().orElseThrow());
    assertEquals(5L, valueType.maxItems().orElseThrow());
    assertFalse(valueType.uniqueItems());
    assertEquals("JsonField<List<Boolean>>", valueType.requiredJavaType());
    assertEquals("JsonField<List<Boolean>>", valueType.optionalJavaType());
    assertEquals("List<Boolean>", valueType.nullableValueJavaType());
    assertSame(FacetConstraints.EMPTY, valueType.facets());
    assertSame(literals, valueType.literals());
  }

  @Test
  void scalarRecordRejectsArrayItemBounds() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new FieldValueType(
                    JavaScalarType.STRING,
                    Optional.empty(),
                    false,
                    false,
                    OptionalLong.of(1),
                    OptionalLong.empty(),
                    FacetConstraints.EMPTY,
                    LiteralConstraints.EMPTY));

    assertEquals("scalar fields must not have array item bounds", exception.getMessage());
  }

  @Test
  void arrayFactoryPreservesUniqueItems() {
    FieldValueType valueType =
        FieldValueType.array(
            JavaScalarType.STRING,
            OptionalLong.empty(),
            OptionalLong.empty(),
            true,
            FacetConstraints.EMPTY,
            LiteralConstraints.EMPTY);

    assertTrue(valueType.array());
    assertTrue(valueType.uniqueItems());
  }

  @Test
  void scalarRecordRejectsUniqueItems() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new FieldValueType(
                    JavaScalarType.STRING,
                    Optional.empty(),
                    false,
                    false,
                    OptionalLong.empty(),
                    OptionalLong.empty(),
                    true,
                    FacetConstraints.EMPTY,
                    LiteralConstraints.EMPTY));

    assertEquals("scalar fields must not have uniqueItems", exception.getMessage());
  }

  @Test
  void factoriesRejectNullScalarTypes() {
    assertThrows(NullPointerException.class, () -> FieldValueType.scalar(null));
    assertThrows(
        NullPointerException.class, () -> FieldValueType.scalar(null, FacetConstraints.EMPTY));
    assertThrows(
        NullPointerException.class,
        () -> FieldValueType.scalar(null, FacetConstraints.EMPTY, LiteralConstraints.EMPTY));
    assertThrows(
        NullPointerException.class,
        () ->
            FieldValueType.nullableScalar(null, FacetConstraints.EMPTY, LiteralConstraints.EMPTY));
    assertThrows(
        NullPointerException.class,
        () -> FieldValueType.array(null, OptionalLong.empty(), OptionalLong.empty()));
    assertThrows(
        NullPointerException.class,
        () ->
            FieldValueType.array(
                null, OptionalLong.empty(), OptionalLong.empty(), FacetConstraints.EMPTY));
    assertThrows(
        NullPointerException.class,
        () ->
            FieldValueType.array(
                null,
                OptionalLong.empty(),
                OptionalLong.empty(),
                FacetConstraints.EMPTY,
                LiteralConstraints.EMPTY));
    assertThrows(
        NullPointerException.class,
        () ->
            FieldValueType.nullableArray(
                null,
                OptionalLong.empty(),
                OptionalLong.empty(),
                FacetConstraints.EMPTY,
                LiteralConstraints.EMPTY));
  }

  @Test
  void recordConstructorRejectsNullSupportingConstraints() {
    assertThrows(
        NullPointerException.class,
        () ->
            new FieldValueType(
                JavaScalarType.STRING,
                Optional.empty(),
                true,
                false,
                null,
                OptionalLong.empty(),
                FacetConstraints.EMPTY,
                LiteralConstraints.EMPTY));
    assertThrows(
        NullPointerException.class,
        () ->
            new FieldValueType(
                JavaScalarType.STRING,
                Optional.empty(),
                true,
                false,
                OptionalLong.empty(),
                null,
                FacetConstraints.EMPTY,
                LiteralConstraints.EMPTY));
    assertThrows(
        NullPointerException.class,
        () ->
            new FieldValueType(
                JavaScalarType.STRING,
                Optional.empty(),
                true,
                false,
                OptionalLong.empty(),
                OptionalLong.empty(),
                null,
                LiteralConstraints.EMPTY));
    assertThrows(
        NullPointerException.class,
        () ->
            new FieldValueType(
                JavaScalarType.STRING,
                Optional.empty(),
                true,
                false,
                OptionalLong.empty(),
                OptionalLong.empty(),
                FacetConstraints.EMPTY,
                null));
  }

  private static void assertScalarTypes(
      JavaScalarType scalarType,
      String requiredJavaType,
      String optionalJavaType,
      String nullableValueJavaType) {
    FieldValueType valueType = FieldValueType.scalar(scalarType);

    assertEquals(scalarType, valueType.scalarType());
    assertFalse(valueType.array());
    assertFalse(valueType.nullable());
    assertEquals(requiredJavaType, valueType.requiredJavaType());
    assertEquals(optionalJavaType, valueType.optionalJavaType());
    assertEquals(nullableValueJavaType, valueType.nullableValueJavaType());
  }
}
