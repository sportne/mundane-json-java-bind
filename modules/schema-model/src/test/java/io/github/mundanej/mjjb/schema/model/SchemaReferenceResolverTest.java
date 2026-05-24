package io.github.mundanej.mjjb.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.Member;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.ObjectValue;
import org.junit.jupiter.api.Test;

final class SchemaReferenceResolverTest {
  @Test
  void resolvesRootReference() {
    SchemaReferenceResolutionResult result =
        resolve(
            """
            {
              "$ref": "#/$defs/root",
              "$defs": {
                "root": {
                  "type": "object",
                  "additionalProperties": false
                }
              }
            }
            """);

    assertTrue(result.diagnostics().isEmpty());
    ObjectValue root = (ObjectValue) result.root().orElseThrow();
    assertEquals("/$defs/root", root.pointer().value());
    assertEquals("type", root.members().getFirst().name());
  }

  @Test
  void resolvesDefinitionsWithEscapedJsonPointerTokens() {
    SchemaReferenceResolutionResult result =
        resolve(
            """
            {
              "type": "object",
              "properties": {
                "id": {"$ref": "#/$defs/a~1b/c~0d"}
              },
              "additionalProperties": false,
              "$defs": {
                "a/b": {
                  "c~d": {"type": "string"}
                }
              }
            }
            """);

    assertTrue(result.diagnostics().isEmpty());
    ObjectValue root = (ObjectValue) result.root().orElseThrow();
    ObjectValue properties = (ObjectValue) member(root, "properties").value();
    ObjectValue id = (ObjectValue) member(properties, "id").value();

    assertEquals("/$defs/a~1b/c~0d", id.pointer().value());
    assertEquals("type", id.members().getFirst().name());
    assertTrue(root.members().stream().anyMatch(member -> "$defs".equals(member.name())));
  }

  @Test
  void reportsRemoteMissingInvalidAndCyclicReferences() {
    assertDiagnostic(
        "{\"$ref\":\"https://example.com/schema.json\"}",
        SchemaReferenceResolver.REMOTE_REF_CODE,
        "/$ref");
    assertDiagnostic(
        "{\"$ref\":\"#/$defs/missing\",\"$defs\":{}}",
        SchemaReferenceResolver.MISSING_REF_CODE,
        "/$ref");
    assertDiagnostic(
        "{\"$ref\":\"#/bad~2token\"}", SchemaReferenceResolver.INVALID_REF_CODE, "/$ref");
    assertDiagnostic(
        "{\"$ref\":\"#/$defs/a\",\"$defs\":{\"a\":{\"$ref\":\"#/$defs/b\"},\"b\":{\"$ref\":\"#/$defs/a\"}}}",
        SchemaReferenceResolver.CYCLIC_REF_CODE,
        "/$defs/a/$ref");
  }

  @Test
  void reportsReferenceObjectsWithAssertionSiblings() {
    assertDiagnostic(
        """
        {
          "$ref": "#/$defs/id",
          "minLength": 1,
          "$defs": {"id": {"type": "string"}}
        }
        """,
        SchemaReferenceResolver.UNSUPPORTED_REF_SIBLING_CODE,
        "/minLength");
  }

  @Test
  void doesNotResolveAnnotationJsonValues() {
    SchemaReferenceResolutionResult result =
        resolve(
            """
            {
              "type": "object",
              "default": {"$ref": "not-a-schema", "$defs": {"literal": true}},
              "examples": [{"$ref": "also-not-a-schema"}],
              "additionalProperties": false
            }
            """);

    assertTrue(result.diagnostics().isEmpty());
    ObjectValue root = (ObjectValue) result.root().orElseThrow();
    ObjectValue defaultValue = (ObjectValue) member(root, "default").value();
    ObjectValue examplesValue =
        (ObjectValue)
            ((SchemaSyntaxValue.ArrayValue) member(root, "examples").value()).items().getFirst();

    assertTrue(member(defaultValue, "$defs").value() instanceof ObjectValue);
    assertEquals(
        "not-a-schema",
        ((SchemaSyntaxValue.StringValue) member(defaultValue, "$ref").value()).value());
    assertEquals(
        "also-not-a-schema",
        ((SchemaSyntaxValue.StringValue) member(examplesValue, "$ref").value()).value());
  }

  @Test
  void reportsReferencesInsideUnreachableDefinitions() {
    assertDiagnostic(
        """
        {
          "type": "object",
          "additionalProperties": false,
          "$defs": {
            "unused": {"$ref": "https://example.com/remote.json"}
          }
        }
        """,
        SchemaReferenceResolver.REMOTE_REF_CODE,
        "/$defs/unused/$ref");
  }

  private static void assertDiagnostic(String source, String code, String pointer) {
    SchemaReferenceResolutionResult result = resolve(source);

    assertFalse(result.diagnostics().isEmpty());
    assertEquals(code, result.diagnostics().getFirst().code());
    assertEquals(pointer, result.diagnostics().getFirst().pointer().value());
  }

  private static SchemaReferenceResolutionResult resolve(String source) {
    SchemaSyntaxParseResult parseResult = SchemaSyntaxParser.parse(source);
    assertTrue(parseResult.diagnostics().isEmpty());
    return SchemaReferenceResolver.resolve(parseResult.root());
  }

  private static Member member(ObjectValue object, String name) {
    return object.members().stream()
        .filter(member -> name.equals(member.name()))
        .findFirst()
        .orElseThrow();
  }
}
