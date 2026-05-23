package io.github.mundanej.mjjb.generator.core.internal.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjjb.schema.model.JsonPointer;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class BindingModelTest {
  @Test
  void convenienceConstructorPreservesCoreValuesAndDefaultsTaggedUnionToEmpty() {
    ObjectBinding rootObject = rootObject();

    BindingModel model = new BindingModel("example.generated", "GeneratedBindings", rootObject);

    assertEquals("example.generated", model.packageName());
    assertEquals("GeneratedBindings", model.rootTypeName());
    assertSame(rootObject, model.rootObject());
    assertEquals(Optional.empty(), model.taggedUnion());
  }

  @Test
  void canonicalConstructorPreservesPackageRootObjectAndTaggedUnion() {
    ObjectBinding rootObject = rootObject();
    TaggedUnionBinding taggedUnion =
        new TaggedUnionBinding(
            "kind",
            JsonPointer.ROOT,
            List.of(
                new TaggedUnionBranch(
                    "person", new ObjectBinding("Person", JsonPointer.ROOT, List.of()))));
    Optional<TaggedUnionBinding> taggedUnionOptional = Optional.of(taggedUnion);

    BindingModel model =
        new BindingModel("example.tagged", "TaggedBindings", rootObject, taggedUnionOptional);

    assertEquals("example.tagged", model.packageName());
    assertEquals("TaggedBindings", model.rootTypeName());
    assertSame(rootObject, model.rootObject());
    assertSame(taggedUnionOptional, model.taggedUnion());
    assertSame(taggedUnion, model.taggedUnion().orElseThrow());
  }

  @Test
  void canonicalConstructorPreservesExplicitEmptyTaggedUnion() {
    ObjectBinding rootObject = rootObject();
    Optional<TaggedUnionBinding> taggedUnionOptional = Optional.empty();

    BindingModel model =
        new BindingModel("example.generated", "GeneratedBindings", rootObject, taggedUnionOptional);

    assertSame(taggedUnionOptional, model.taggedUnion());
    assertTrue(model.taggedUnion().isEmpty());
  }

  @ParameterizedTest
  @MethodSource("blankPackageAndRootValues")
  void rejectsBlankPackageAndRootNames(
      String packageName, String rootTypeName, String expectedMessage) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new BindingModel(packageName, rootTypeName, rootObject()));

    assertEquals(expectedMessage, exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("nullFieldArguments")
  void rejectsNullFields(
      String packageName,
      String rootTypeName,
      ObjectBinding rootObject,
      Optional<TaggedUnionBinding> taggedUnion,
      String expectedMessage) {
    NullPointerException exception =
        assertThrows(
            NullPointerException.class,
            () -> new BindingModel(packageName, rootTypeName, rootObject, taggedUnion));

    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  void optionalFieldValueTypeUsesOptionalForAbsentNonNullableFieldsOnly() {
    assertEquals(
        "Optional<String>", FieldValueType.scalar(JavaScalarType.STRING).optionalJavaType());
    assertEquals(
        "Optional<List<Long>>",
        FieldValueType.array(JavaScalarType.INTEGER, OptionalLong.empty(), OptionalLong.empty())
            .optionalJavaType());
    assertEquals(
        "JsonField<Double>",
        FieldValueType.nullableScalar(
                JavaScalarType.NUMBER, FacetConstraints.EMPTY, LiteralConstraints.EMPTY)
            .optionalJavaType());
    assertEquals(
        "JsonField<List<Boolean>>",
        FieldValueType.nullableArray(
                JavaScalarType.BOOLEAN,
                OptionalLong.empty(),
                OptionalLong.empty(),
                FacetConstraints.EMPTY,
                LiteralConstraints.EMPTY)
            .optionalJavaType());
  }

  @Test
  void taggedUnionOptionalCanBeInspectedWithOptionalApi() {
    BindingModel model = new BindingModel("example.generated", "GeneratedBindings", rootObject());

    assertFalse(model.taggedUnion().isPresent());
    assertEquals(
        "fallback",
        model.taggedUnion().map(TaggedUnionBinding::tagPropertyName).orElse("fallback"));
  }

  private static Stream<Arguments> blankPackageAndRootValues() {
    return Stream.of(
        Arguments.of("", "GeneratedBindings", "packageName must not be blank"),
        Arguments.of(" \t", "GeneratedBindings", "packageName must not be blank"),
        Arguments.of("example.generated", "", "rootTypeName must not be blank"),
        Arguments.of("example.generated", " \n", "rootTypeName must not be blank"));
  }

  private static Stream<Arguments> nullFieldArguments() {
    return Stream.of(
        Arguments.of(null, "GeneratedBindings", rootObject(), Optional.empty(), "packageName"),
        Arguments.of("example.generated", null, rootObject(), Optional.empty(), "rootTypeName"),
        Arguments.of(
            "example.generated", "GeneratedBindings", null, Optional.empty(), "rootObject"),
        Arguments.of("example.generated", "GeneratedBindings", rootObject(), null, "taggedUnion"));
  }

  private static ObjectBinding rootObject() {
    return new ObjectBinding(
        "GeneratedBindings",
        JsonPointer.ROOT,
        List.of(
            new FieldBinding(
                "id",
                "id",
                FieldValueType.scalar(JavaScalarType.STRING),
                true,
                JsonPointer.ROOT.property("properties").property("id"))));
  }
}
