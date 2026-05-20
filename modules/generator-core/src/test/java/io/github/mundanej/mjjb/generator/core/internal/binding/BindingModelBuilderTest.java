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
  void buildsNullableFieldBindings() {
    BindingBuildResult result =
        build(
            """
            {
              "type": "object",
              "properties": {
                "name": {"type": ["null", "string"]},
                "scores": {"type": ["array", "null"], "items": {"type": "number"}}
              },
              "required": ["name"],
              "additionalProperties": false
            }
            """);

    assertTrue(result.diagnostics().isEmpty());
    BindingModel model = result.model().orElseThrow();
    assertEquals(List.of(true, true), nullableFlags(model));
    assertEquals(
        "JsonField<String>", model.rootObject().fields().get(0).valueType().requiredJavaType());
    assertEquals(
        "JsonField<String>", model.rootObject().fields().get(0).valueType().optionalJavaType());
    assertEquals(
        "JsonField<List<Double>>",
        model.rootObject().fields().get(1).valueType().requiredJavaType());
    assertEquals(List.of(true, false), requiredFlags(model));
  }

  @Test
  void collectsScalarAndArrayItemFacets() {
    BindingBuildResult result =
        build(
            """
            {
              "type": "object",
              "properties": {
                "code": {
                  "type": "string",
                  "minLength": 2,
                  "maxLength": 4,
                  "pattern": "^[A-Z]+$",
                  "format": "uuid"
                },
                "count": {
                  "type": "integer",
                  "minimum": 1,
                  "maximum": 10,
                  "exclusiveMinimum": 0,
                  "exclusiveMaximum": 11
                },
                "names": {
                  "type": "array",
                  "items": {"type": "string", "minLength": 1, "pattern": "^[a-z]+$"}
                }
              },
              "additionalProperties": false
            }
            """);

    assertTrue(result.diagnostics().isEmpty());
    List<FieldBinding> fields = result.model().orElseThrow().rootObject().fields();
    assertEquals(2L, fields.get(0).valueType().facets().minLength().orElseThrow());
    assertEquals(4L, fields.get(0).valueType().facets().maxLength().orElseThrow());
    assertEquals("^[A-Z]+$", fields.get(0).valueType().facets().pattern().orElseThrow());
    assertEquals("uuid", fields.get(0).valueType().facets().format().orElseThrow());
    assertEquals("1", fields.get(1).valueType().facets().minimum().orElseThrow());
    assertEquals("10", fields.get(1).valueType().facets().maximum().orElseThrow());
    assertEquals("0", fields.get(1).valueType().facets().exclusiveMinimum().orElseThrow());
    assertEquals("11", fields.get(1).valueType().facets().exclusiveMaximum().orElseThrow());
    assertEquals(1L, fields.get(2).valueType().facets().minLength().orElseThrow());
    assertEquals("^[a-z]+$", fields.get(2).valueType().facets().pattern().orElseThrow());
  }

  @Test
  void collectsScalarAndArrayItemLiteralConstraints() {
    BindingBuildResult result =
        build(
            """
            {
              "type": "object",
              "properties": {
                "status": {"type": "string", "enum": ["open", "closed", null], "default": "open"},
                "count": {"type": "integer", "const": 3, "default": 3},
                "ratio": {"type": "number", "enum": [1.5, 2e0], "const": 1.5},
                "flags": {"type": "array", "items": {"type": "boolean", "enum": [true, null]}}
              },
              "additionalProperties": false
            }
            """);

    assertTrue(result.diagnostics().isEmpty());
    List<FieldBinding> fields = result.model().orElseThrow().rootObject().fields();
    assertEquals(3, fields.get(0).valueType().literals().enumValues().size());
    assertEquals("open", fields.get(0).valueType().literals().defaultValue().orElseThrow().value());
    assertEquals("3", fields.get(1).valueType().literals().constValue().orElseThrow().value());
    assertEquals("3", fields.get(1).valueType().literals().defaultValue().orElseThrow().value());
    assertEquals(2, fields.get(2).valueType().literals().enumValues().size());
    assertEquals("1.5", fields.get(2).valueType().literals().constValue().orElseThrow().value());
    assertEquals(2, fields.get(3).valueType().literals().enumValues().size());
  }

  @Test
  void collectsSchemaAnnotationMetadata() {
    BindingBuildResult result =
        build(
            """
            {
              "title": "Root title",
              "description": "Root description",
              "$comment": "Root comment",
              "examples": [{"id": "abc"}],
              "deprecated": true,
              "readOnly": false,
              "writeOnly": true,
              "type": "object",
              "properties": {
                "id": {
                  "type": "string",
                  "title": "Identifier",
                  "examples": ["abc"],
                  "default": "abc"
                }
              },
              "required": ["id"],
              "additionalProperties": false
            }
            """);

    assertTrue(result.diagnostics().isEmpty());
    BindingModel model = result.model().orElseThrow();
    assertEquals("Root title", model.rootObject().annotations().title().orElseThrow());
    assertEquals("Root description", model.rootObject().annotations().description().orElseThrow());
    assertEquals("Root comment", model.rootObject().annotations().comment().orElseThrow());
    assertEquals("{\"id\":\"abc\"}", model.rootObject().annotations().examplesJson().getFirst());
    assertEquals(true, model.rootObject().annotations().deprecated().orElseThrow());
    assertEquals(false, model.rootObject().annotations().readOnly().orElseThrow());
    assertEquals(true, model.rootObject().annotations().writeOnly().orElseThrow());
    FieldBinding field = model.rootObject().fields().getFirst();
    assertEquals("Identifier", field.annotations().title().orElseThrow());
    assertEquals("\"abc\"", field.annotations().examplesJson().getFirst());
    assertEquals("\"abc\"", field.annotations().defaultJson().orElseThrow());
  }

  @Test
  void buildsTaggedOneOfBindings() {
    BindingBuildResult result =
        build(
            """
            {
              "oneOf": [
                {
                  "type": "object",
                  "properties": {
                    "kind": {"type": "string", "const": "card"},
                    "last4": {"type": "string"},
                    "amount": {"type": "number"}
                  },
                  "required": ["kind", "last4"],
                  "additionalProperties": false
                },
                {
                  "type": "object",
                  "properties": {
                    "kind": {"type": "string", "const": "bank-transfer"},
                    "iban": {"type": "string"},
                    "urgent": {"type": "boolean"}
                  },
                  "required": ["kind", "iban"],
                  "additionalProperties": false
                }
              ]
            }
            """);

    assertTrue(result.diagnostics().isEmpty());
    BindingModel model = result.model().orElseThrow();
    TaggedUnionBinding union = model.taggedUnion().orElseThrow();
    assertEquals("kind", union.tagPropertyName());
    assertEquals(List.of("card", "bank-transfer"), tagValues(union));
    assertEquals(List.of("Card", "BankTransfer"), branchTypeNames(union));
    assertEquals(List.of("last4", "amount"), branchJsonPropertyNames(union.branches().get(0)));
    assertEquals(List.of("iban", "urgent"), branchJsonPropertyNames(union.branches().get(1)));
    assertEquals(List.of(true, false), branchRequiredFlags(union.branches().get(0)));
    assertEquals(List.of(true, false), branchRequiredFlags(union.branches().get(1)));
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
  void rejectsUnsupportedLiteralConstraintShapes() {
    BindingBuildResult result =
        build(
            """
            {
              "type": "object",
              "properties": {
                "badString": {"type": "string", "enum": [{"id": "x"}]},
                "badInteger": {"type": "integer", "const": 1.5},
                "badDefault": {"type": "boolean", "default": []},
                "arrayLevel": {"type": "array", "items": {"type": "string"}, "enum": [["x"]]}
              },
              "additionalProperties": false
            }
            """);

    assertEquals(
        List.of(
            "/properties/arrayLevel/enum",
            "/properties/badDefault/default",
            "/properties/badInteger/const",
            "/properties/badString/enum/0"),
        diagnosticPointers(result));
    assertEquals(
        List.of(
            BindingDiagnostic.UNSUPPORTED_LITERAL_CONSTRAINT_CODE,
            BindingDiagnostic.UNSUPPORTED_LITERAL_CONSTRAINT_CODE,
            BindingDiagnostic.UNSUPPORTED_LITERAL_CONSTRAINT_CODE,
            BindingDiagnostic.UNSUPPORTED_LITERAL_CONSTRAINT_CODE),
        diagnosticCodes(result));
  }

  @Test
  void rejectsRootObjectLiteralConstraints() {
    BindingBuildResult result =
        build(
            """
            {
              "type": "object",
              "properties": {},
              "additionalProperties": false,
              "const": {}
            }
            """);

    assertEquals(
        BindingDiagnostic.UNSUPPORTED_LITERAL_CONSTRAINT_CODE,
        result.diagnostics().getFirst().code());
    assertEquals("/const", result.diagnostics().getFirst().pointer().value());
  }

  @Test
  void rejectsNullableArrayItemBindings() {
    BindingBuildResult result =
        build(
            """
            {
              "type": "object",
              "properties": {
                "names": {"type": "array", "items": {"type": ["null", "string"]}}
              },
              "additionalProperties": false
            }
            """);

    assertEquals(
        BindingDiagnostic.UNSUPPORTED_PROPERTY_TYPE_CODE, result.diagnostics().getFirst().code());
    assertEquals("/properties/names/items/type", result.diagnostics().getFirst().pointer().value());
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

  @Test
  void rejectsUnsupportedTaggedOneOfBindings() {
    BindingBuildResult result =
        build(
            """
            {
              "oneOf": [
                {
                  "type": "object",
                  "properties": {
                    "kind": {"type": "string", "const": "event-a"},
                    "value": {"type": "string"}
                  },
                  "required": ["kind"],
                  "additionalProperties": false
                },
                {
                  "type": "object",
                  "properties": {
                    "kind": {"type": "string", "const": "event_a"}
                  },
                  "required": ["kind"],
                  "additionalProperties": false
                }
              ]
            }
            """);

    assertEquals(BindingDiagnostic.NAME_COLLISION_CODE, result.diagnostics().getFirst().code());
    assertEquals("/oneOf/1/properties/kind", result.diagnostics().getFirst().pointer().value());
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

  private static List<Boolean> nullableFlags(BindingModel model) {
    return model.rootObject().fields().stream().map(field -> field.valueType().nullable()).toList();
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

  private static List<String> tagValues(TaggedUnionBinding union) {
    return union.branches().stream().map(TaggedUnionBranch::tagValue).toList();
  }

  private static List<String> branchTypeNames(TaggedUnionBinding union) {
    return union.branches().stream().map(branch -> branch.object().javaTypeName()).toList();
  }

  private static List<String> branchJsonPropertyNames(TaggedUnionBranch branch) {
    return branch.object().fields().stream().map(FieldBinding::jsonPropertyName).toList();
  }

  private static List<Boolean> branchRequiredFlags(TaggedUnionBranch branch) {
    return branch.object().fields().stream().map(FieldBinding::required).toList();
  }
}
