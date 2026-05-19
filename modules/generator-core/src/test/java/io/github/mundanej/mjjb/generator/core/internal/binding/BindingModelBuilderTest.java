package io.github.mundanej.mjjb.generator.core.internal.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjjb.schema.model.SchemaSyntaxParseResult;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxParser;
import java.util.List;
import org.junit.jupiter.api.Test;

final class BindingModelBuilderTest {
  @Test
  void buildsDeterministicIrForScalarObjectSchema() {
    BindingBuildResult result =
        build(
            """
            {
              "type": "object",
              "properties": {
                "id": {"type": "string"},
                "count": {"type": "integer"},
                "score": {"type": "number"},
                "active": {"type": "boolean"}
              },
              "required": ["id", "count"],
              "additionalProperties": false
            }
            """);

    assertTrue(result.diagnostics().isEmpty());
    BindingModel model = result.model().orElseThrow();
    assertEquals("example.generated", model.packageName());
    assertEquals("GeneratedBindings", model.rootTypeName());
    assertEquals("GeneratedBindings", model.rootObject().javaTypeName());
    assertEquals("", model.rootObject().schemaPointer().value());
    assertEquals(List.of("id", "count", "score", "active"), jsonPropertyNames(model));
    assertEquals(List.of("id", "count", "score", "active"), javaFieldNames(model));
    assertEquals(
        List.of(
            JavaScalarType.STRING,
            JavaScalarType.INTEGER,
            JavaScalarType.NUMBER,
            JavaScalarType.BOOLEAN),
        scalarTypes(model));
    assertEquals(List.of(true, true, false, false), requiredFlags(model));
    assertEquals(
        List.of("/properties/id", "/properties/count", "/properties/score", "/properties/active"),
        schemaPointers(model));
  }

  @Test
  void acceptsObjectSchemaWithoutPropertiesAsEmptyBinding() {
    BindingBuildResult result =
        build(
            """
            {
              "type": "object",
              "additionalProperties": false
            }
            """);

    assertTrue(result.diagnostics().isEmpty());
    assertTrue(result.model().orElseThrow().rootObject().fields().isEmpty());
  }

  @Test
  void mapsJavaFieldNamesDeterministically() {
    BindingBuildResult result =
        build(
            """
            {
              "type": "object",
              "properties": {
                "user-id": {"type": "string"},
                "displayName": {"type": "string"},
                "USER_NAME": {"type": "string"},
                "class": {"type": "string"},
                "123-name": {"type": "string"},
                "!!!": {"type": "string"}
              },
              "additionalProperties": false
            }
            """);

    assertTrue(result.diagnostics().isEmpty());
    assertEquals(
        List.of("userId", "displayName", "userName", "classValue", "value123Name", "value"),
        javaFieldNames(result.model().orElseThrow()));
  }

  @Test
  void exposesOptionalJavaTypesForScalarMappings() {
    assertEquals("Optional<String>", JavaScalarType.STRING.optionalJavaType());
    assertEquals("Optional<Long>", JavaScalarType.INTEGER.optionalJavaType());
    assertEquals("Optional<Double>", JavaScalarType.NUMBER.optionalJavaType());
    assertEquals("Optional<Boolean>", JavaScalarType.BOOLEAN.optionalJavaType());
  }

  @Test
  void buildsArrayBindingsForHomogeneousScalarItems() {
    BindingBuildResult result =
        build(
            """
            {
              "type": "object",
              "properties": {
                "tags": {"type": "array", "items": {"type": "string"}, "minItems": 1},
                "scores": {"type": "array", "items": {"type": "number"}, "maxItems": 3}
              },
              "required": ["tags"],
              "additionalProperties": false
            }
            """);

    assertTrue(result.diagnostics().isEmpty());
    BindingModel model = result.model().orElseThrow();
    assertEquals(List.of("tags", "scores"), jsonPropertyNames(model));
    assertEquals(List.of(true, true), arrayFlags(model));
    assertEquals(List.of(JavaScalarType.STRING, JavaScalarType.NUMBER), scalarTypes(model));
    assertEquals(List.of(true, false), requiredFlags(model));
    assertEquals("List<String>", model.rootObject().fields().get(0).valueType().requiredJavaType());
    assertEquals(
        "Optional<List<Double>>",
        model.rootObject().fields().get(1).valueType().optionalJavaType());
    assertEquals(1L, model.rootObject().fields().get(0).valueType().minItems().orElseThrow());
    assertEquals(3L, model.rootObject().fields().get(1).valueType().maxItems().orElseThrow());
  }

  @Test
  void rejectsJavaFieldNameCollisions() {
    BindingBuildResult result =
        build(
            """
            {
              "type": "object",
              "properties": {
                "user-id": {"type": "string"},
                "user_id": {"type": "string"}
              },
              "additionalProperties": false
            }
            """);

    assertFalse(result.diagnostics().isEmpty());
    assertEquals(BindingDiagnostic.NAME_COLLISION_CODE, result.diagnostics().getFirst().code());
    assertEquals("/properties/user_id", result.diagnostics().getFirst().pointer().value());
  }

  @Test
  void rejectsMissingRootObjectType() {
    BindingBuildResult result = build("{\"additionalProperties\":false}");

    assertEquals(BindingDiagnostic.ROOT_TYPE_CODE, result.diagnostics().getFirst().code());
    assertEquals("", result.diagnostics().getFirst().pointer().value());
  }

  @Test
  void rejectsNonObjectRootType() {
    BindingBuildResult result = build("{\"type\":\"array\",\"additionalProperties\":false}");

    assertEquals(BindingDiagnostic.ROOT_TYPE_CODE, result.diagnostics().getFirst().code());
    assertEquals("/type", result.diagnostics().getFirst().pointer().value());
  }

  @Test
  void rejectsMissingAdditionalPropertiesFalse() {
    BindingBuildResult result = build("{\"type\":\"object\"}");

    assertEquals(
        BindingDiagnostic.ADDITIONAL_PROPERTIES_CODE, result.diagnostics().getFirst().code());
    assertEquals("", result.diagnostics().getFirst().pointer().value());
  }

  @Test
  void rejectsUnsupportedAdditionalPropertiesValue() {
    BindingBuildResult result = build("{\"type\":\"object\",\"additionalProperties\":true}");

    assertEquals(
        BindingDiagnostic.ADDITIONAL_PROPERTIES_CODE, result.diagnostics().getFirst().code());
    assertEquals("/additionalProperties", result.diagnostics().getFirst().pointer().value());
  }

  @Test
  void rejectsPropertySchemaWithoutType() {
    BindingBuildResult result =
        build(
            """
            {
              "type": "object",
              "properties": {
                "id": {"description": "identifier"}
              },
              "additionalProperties": false
            }
            """);

    assertEquals(
        BindingDiagnostic.MISSING_PROPERTY_TYPE_CODE, result.diagnostics().getFirst().code());
    assertEquals("/properties/id", result.diagnostics().getFirst().pointer().value());
  }

  @Test
  void rejectsUnsupportedPropertyTypes() {
    BindingBuildResult result =
        build(
            """
            {
              "type": "object",
              "properties": {
                "child": {"type": "object"}
              },
              "additionalProperties": false
            }
            """);

    assertEquals(List.of("/properties/child/type"), diagnosticPointers(result));
    assertEquals(
        List.of(BindingDiagnostic.UNSUPPORTED_PROPERTY_TYPE_CODE), diagnosticCodes(result));
  }

  @Test
  void rejectsUnsupportedArrayBindingShapes() {
    BindingBuildResult result =
        build(
            """
            {
              "type": "object",
              "properties": {
                "missingItems": {"type": "array"},
                "missingItemType": {"type": "array", "items": {}},
                "nested": {"type": "array", "items": {"type": "array"}},
                "backwards": {"type": "array", "items": {"type": "string"}, "minItems": 2, "maxItems": 1}
              },
              "additionalProperties": false
            }
            """);

    assertEquals(
        List.of(
            "/properties/backwards/maxItems",
            "/properties/missingItemType/items",
            "/properties/missingItems",
            "/properties/nested/items/type"),
        diagnosticPointers(result));
    assertEquals(
        List.of(
            BindingDiagnostic.INVALID_ARRAY_BOUNDS_CODE,
            BindingDiagnostic.MISSING_ARRAY_ITEM_TYPE_CODE,
            BindingDiagnostic.MISSING_ARRAY_ITEMS_CODE,
            BindingDiagnostic.UNSUPPORTED_PROPERTY_TYPE_CODE),
        diagnosticCodes(result));
  }

  @Test
  void rejectsUnknownRequiredProperties() {
    BindingBuildResult result =
        build(
            """
            {
              "type": "object",
              "properties": {
                "id": {"type": "string"}
              },
              "required": ["missing"],
              "additionalProperties": false
            }
            """);

    assertEquals(BindingDiagnostic.UNKNOWN_REQUIRED_CODE, result.diagnostics().getFirst().code());
    assertEquals("/required/0", result.diagnostics().getFirst().pointer().value());
  }

  private static BindingBuildResult build(String schema) {
    SchemaSyntaxParseResult parseResult = SchemaSyntaxParser.parse(schema);
    assertTrue(parseResult.diagnostics().isEmpty());
    return new BindingModelBuilder()
        .build(parseResult.root(), "example.generated", "GeneratedBindings");
  }

  private static List<String> jsonPropertyNames(BindingModel model) {
    return model.rootObject().fields().stream().map(FieldBinding::jsonPropertyName).toList();
  }

  private static List<String> javaFieldNames(BindingModel model) {
    return model.rootObject().fields().stream().map(FieldBinding::javaFieldName).toList();
  }

  private static List<JavaScalarType> scalarTypes(BindingModel model) {
    return model.rootObject().fields().stream().map(FieldBinding::scalarType).toList();
  }

  private static List<Boolean> requiredFlags(BindingModel model) {
    return model.rootObject().fields().stream().map(FieldBinding::required).toList();
  }

  private static List<Boolean> arrayFlags(BindingModel model) {
    return model.rootObject().fields().stream().map(FieldBinding::array).toList();
  }

  private static List<String> schemaPointers(BindingModel model) {
    return model.rootObject().fields().stream()
        .map(field -> field.schemaPointer().value())
        .toList();
  }

  private static List<String> diagnosticPointers(BindingBuildResult result) {
    return result.diagnostics().stream().map(diagnostic -> diagnostic.pointer().value()).toList();
  }

  private static List<String> diagnosticCodes(BindingBuildResult result) {
    return result.diagnostics().stream().map(BindingDiagnostic::code).toList();
  }
}
