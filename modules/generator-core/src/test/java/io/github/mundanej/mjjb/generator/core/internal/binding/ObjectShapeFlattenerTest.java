package io.github.mundanej.mjjb.generator.core.internal.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjjb.schema.model.SchemaSyntaxParseResult;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxParser;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.ArrayValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.Member;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.ObjectValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.StringValue;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ObjectShapeFlattenerTest {
  @Test
  void flattensCompatiblePropertiesRequiredNamesAndMetadata() {
    ArrayList<BindingDiagnostic> diagnostics = new ArrayList<>();

    ObjectValue flattened =
        ObjectShapeFlattener.flatten(
                parseObject(
                    """
                    {
                      "type": "object",
                      "description": "base",
                      "properties": {"id": {"type": "string"}},
                      "required": ["id"],
                      "allOf": [
                        {
                          "type": "object",
                          "description": "base",
                          "properties": {"count": {"type": "integer"}},
                          "required": ["count"]
                        }
                      ]
                    }
                    """),
                diagnostics)
            .orElseThrow();

    assertTrue(diagnostics.isEmpty());
    assertEquals(List.of("type", "description", "properties", "required"), memberNames(flattened));
    assertEquals(List.of("id", "count"), propertyNames(flattened));
    assertEquals(List.of("id", "count"), requiredNames(flattened));
  }

  @Test
  void preservesDuplicatePropertyWhenSchemasAreEquivalent() {
    ArrayList<BindingDiagnostic> diagnostics = new ArrayList<>();

    ObjectValue flattened =
        ObjectShapeFlattener.flatten(
                parseObject(
                    """
                    {
                      "properties": {"id": {"type": "string", "minLength": 2}},
                      "allOf": [
                        {
                          "properties": {"id": {"minLength": 2, "type": "string"}}
                        }
                      ]
                    }
                    """),
                diagnostics)
            .orElseThrow();

    assertTrue(diagnostics.isEmpty());
    assertEquals(List.of("id"), propertyNames(flattened));
  }

  @Test
  void rejectsConflictingPropertiesWithBranchPropertyPointer() {
    ArrayList<BindingDiagnostic> diagnostics = new ArrayList<>();

    assertFalse(
        ObjectShapeFlattener.flatten(
                parseObject(
                    """
                    {
                      "properties": {"id": {"type": "string"}},
                      "allOf": [
                        {
                          "properties": {"id": {"type": "integer"}}
                        }
                      ]
                    }
                    """),
                diagnostics)
            .isPresent());

    assertEquals(1, diagnostics.size());
    assertEquals(BindingDiagnostic.UNSUPPORTED_ALL_OF_CODE, diagnostics.getFirst().code());
    assertEquals("/allOf/0/properties/id", diagnostics.getFirst().pointer().value());
  }

  @Test
  void rejectsClosedBranchThatDoesNotDeclareMergedProperty() {
    ArrayList<BindingDiagnostic> diagnostics = new ArrayList<>();

    assertFalse(
        ObjectShapeFlattener.flatten(
                parseObject(
                    """
                    {
                      "allOf": [
                        {
                          "properties": {"id": {"type": "string"}},
                          "additionalProperties": false
                        },
                        {
                          "properties": {"count": {"type": "integer"}}
                        }
                      ]
                    }
                    """),
                diagnostics)
            .isPresent());

    assertEquals(1, diagnostics.size());
    assertEquals(BindingDiagnostic.UNSUPPORTED_ALL_OF_CODE, diagnostics.getFirst().code());
    assertEquals("/allOf/0/additionalProperties", diagnostics.getFirst().pointer().value());
  }

  private static ObjectValue parseObject(String schema) {
    SchemaSyntaxParseResult parseResult = SchemaSyntaxParser.parse(schema);
    assertTrue(parseResult.diagnostics().isEmpty());
    assertTrue(parseResult.root() instanceof ObjectValue);
    return (ObjectValue) parseResult.root();
  }

  private static List<String> memberNames(ObjectValue objectValue) {
    return objectValue.members().stream().map(Member::name).toList();
  }

  private static List<String> propertyNames(ObjectValue objectValue) {
    ObjectValue properties =
        (ObjectValue) BindingModelBuilder.member(objectValue, "properties").orElseThrow().value();
    return properties.members().stream().map(Member::name).toList();
  }

  private static List<String> requiredNames(ObjectValue objectValue) {
    ArrayValue required =
        (ArrayValue) BindingModelBuilder.member(objectValue, "required").orElseThrow().value();
    return required.items().stream().map(item -> ((StringValue) item).value()).toList();
  }
}
