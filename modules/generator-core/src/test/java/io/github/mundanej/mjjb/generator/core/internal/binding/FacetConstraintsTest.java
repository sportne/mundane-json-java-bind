package io.github.mundanej.mjjb.generator.core.internal.binding;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

final class FacetConstraintsTest {
  @Test
  void emptyConstraintsHaveNoFacets() {
    FacetConstraints constraints = FacetConstraints.EMPTY;

    assertAll(
        () -> assertFalse(constraints.minLength().isPresent()),
        () -> assertFalse(constraints.maxLength().isPresent()),
        () -> assertFalse(constraints.pattern().isPresent()),
        () -> assertFalse(constraints.format().isPresent()),
        () -> assertFalse(constraints.minimum().isPresent()),
        () -> assertFalse(constraints.maximum().isPresent()),
        () -> assertFalse(constraints.exclusiveMinimum().isPresent()),
        () -> assertFalse(constraints.exclusiveMaximum().isPresent()),
        () -> assertFalse(constraints.hasStringFacets()),
        () -> assertFalse(constraints.hasNumericFacets()));
  }

  @Test
  void detectsStringFacetsIndependently() {
    assertAll(
        () -> assertHasOnlyStringFacets(emptyWithMinLength(1)),
        () -> assertHasOnlyStringFacets(emptyWithMaxLength(12)),
        () -> assertHasOnlyStringFacets(emptyWithPattern("^[A-Z]+$")),
        () -> assertHasOnlyStringFacets(emptyWithFormat("uuid")));
  }

  @Test
  void detectsNumericFacetsIndependently() {
    assertAll(
        () -> assertHasOnlyNumericFacets(emptyWithMinimum("1")),
        () -> assertHasOnlyNumericFacets(emptyWithMaximum("10")),
        () -> assertHasOnlyNumericFacets(emptyWithExclusiveMinimum("0")),
        () -> assertHasOnlyNumericFacets(emptyWithExclusiveMaximum("11")));
  }

  @Test
  void preservesPatternAndFormatValues() {
    FacetConstraints constraints = emptyWithPatternAndFormat("^[a-z]{2,4}$", "email");

    assertEquals("^[a-z]{2,4}$", constraints.pattern().orElseThrow());
    assertEquals("email", constraints.format().orElseThrow());
  }

  @Test
  void preservesNumericFacetTextValues() {
    FacetConstraints constraints =
        new FacetConstraints(
            OptionalLong.empty(),
            OptionalLong.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of("1.00"),
            Optional.of("10e2"),
            Optional.of("-5"),
            Optional.of("99.5"));

    assertAll(
        () -> assertEquals("1.00", constraints.minimum().orElseThrow()),
        () -> assertEquals("10e2", constraints.maximum().orElseThrow()),
        () -> assertEquals("-5", constraints.exclusiveMinimum().orElseThrow()),
        () -> assertEquals("99.5", constraints.exclusiveMaximum().orElseThrow()));
  }

  @Test
  void rejectsNullConstructorArguments() {
    assertAll(
        () ->
            assertThrows(
                NullPointerException.class,
                () ->
                    new FacetConstraints(
                        null,
                        OptionalLong.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty())),
        () ->
            assertThrows(
                NullPointerException.class,
                () ->
                    new FacetConstraints(
                        OptionalLong.empty(),
                        null,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty())),
        () ->
            assertThrows(
                NullPointerException.class,
                () ->
                    new FacetConstraints(
                        OptionalLong.empty(),
                        OptionalLong.empty(),
                        null,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty())),
        () ->
            assertThrows(
                NullPointerException.class,
                () ->
                    new FacetConstraints(
                        OptionalLong.empty(),
                        OptionalLong.empty(),
                        Optional.empty(),
                        null,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty())),
        () ->
            assertThrows(
                NullPointerException.class,
                () ->
                    new FacetConstraints(
                        OptionalLong.empty(),
                        OptionalLong.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        null,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty())),
        () ->
            assertThrows(
                NullPointerException.class,
                () ->
                    new FacetConstraints(
                        OptionalLong.empty(),
                        OptionalLong.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        null,
                        Optional.empty(),
                        Optional.empty())),
        () ->
            assertThrows(
                NullPointerException.class,
                () ->
                    new FacetConstraints(
                        OptionalLong.empty(),
                        OptionalLong.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        null,
                        Optional.empty())),
        () ->
            assertThrows(
                NullPointerException.class,
                () ->
                    new FacetConstraints(
                        OptionalLong.empty(),
                        OptionalLong.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        null)));
  }

  private static void assertHasOnlyStringFacets(FacetConstraints constraints) {
    assertTrue(constraints.hasStringFacets());
    assertFalse(constraints.hasNumericFacets());
  }

  private static void assertHasOnlyNumericFacets(FacetConstraints constraints) {
    assertFalse(constraints.hasStringFacets());
    assertTrue(constraints.hasNumericFacets());
  }

  private static FacetConstraints emptyWithMinLength(long minLength) {
    return new FacetConstraints(
        OptionalLong.of(minLength),
        OptionalLong.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  private static FacetConstraints emptyWithMaxLength(long maxLength) {
    return new FacetConstraints(
        OptionalLong.empty(),
        OptionalLong.of(maxLength),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  private static FacetConstraints emptyWithPattern(String pattern) {
    return emptyWithPatternAndFormat(pattern, null);
  }

  private static FacetConstraints emptyWithFormat(String format) {
    return emptyWithPatternAndFormat(null, format);
  }

  private static FacetConstraints emptyWithPatternAndFormat(String pattern, String format) {
    return new FacetConstraints(
        OptionalLong.empty(),
        OptionalLong.empty(),
        Optional.ofNullable(pattern),
        Optional.ofNullable(format),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  private static FacetConstraints emptyWithMinimum(String minimum) {
    return new FacetConstraints(
        OptionalLong.empty(),
        OptionalLong.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of(minimum),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  private static FacetConstraints emptyWithMaximum(String maximum) {
    return new FacetConstraints(
        OptionalLong.empty(),
        OptionalLong.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of(maximum),
        Optional.empty(),
        Optional.empty());
  }

  private static FacetConstraints emptyWithExclusiveMinimum(String exclusiveMinimum) {
    return new FacetConstraints(
        OptionalLong.empty(),
        OptionalLong.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of(exclusiveMinimum),
        Optional.empty());
  }

  private static FacetConstraints emptyWithExclusiveMaximum(String exclusiveMaximum) {
    return new FacetConstraints(
        OptionalLong.empty(),
        OptionalLong.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of(exclusiveMaximum));
  }
}
