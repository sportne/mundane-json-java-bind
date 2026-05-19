package io.github.mundanej.mjjb.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SchemaSupportProfileTest {
  @Test
  void declaresSupportedAndUnsupportedKeywords() {
    for (JsonSchemaKeyword keyword : JsonSchemaKeyword.bindingKeywords()) {
      assertTrue(SchemaSupportProfile.supportsKeyword(keyword.keyword()), keyword.keyword());
      assertTrue(SchemaSupportProfile.acceptsKeyword(keyword.keyword()), keyword.keyword());
    }
    for (JsonSchemaKeyword keyword : JsonSchemaKeyword.ignoredAnnotationKeywords()) {
      assertFalse(SchemaSupportProfile.supportsKeyword(keyword.keyword()), keyword.keyword());
      assertTrue(SchemaSupportProfile.acceptsKeyword(keyword.keyword()), keyword.keyword());
    }
    for (JsonSchemaKeyword keyword : JsonSchemaKeyword.unsupportedKeywords()) {
      assertFalse(SchemaSupportProfile.supportsKeyword(keyword.keyword()), keyword.keyword());
      assertFalse(SchemaSupportProfile.acceptsKeyword(keyword.keyword()), keyword.keyword());
    }
  }

  @Test
  void jsonPointerEscapesTokens() {
    assertEquals("/a~1b/c~0d", JsonPointer.ROOT.property("a/b").property("c~d").value());
  }

  @Test
  void unsupportedKeywordDiagnosticReferencesProfile() {
    SchemaSupportDiagnostic diagnostic =
        SchemaSupportProfile.unsupportedKeyword("$ref", JsonPointer.ROOT.property("$ref"));

    assertEquals(SchemaSupportProfile.UNSUPPORTED_KEYWORD_CODE, diagnostic.code());
    assertEquals("/$ref", diagnostic.pointer().value());
  }

  @Test
  void profileAndDialectExposeDraft202012Tokens() {
    assertEquals(
        URI.create("https://json-schema.org/draft/2020-12/schema"),
        JsonSchemaDialect.DRAFT_2020_12.uri());
    assertEquals("JSP-DATA-2020-12", JsonSchemaProfile.JSP_DATA_2020_12.token());
    assertTrue(JsonSchemaProfile.fromToken("JSP-DATA-2020-12").isPresent());
    assertTrue(JsonSchemaProfile.fromToken(null).isEmpty());
    assertTrue(JsonSchemaProfile.fromToken("other").isEmpty());
  }

  @Test
  void keywordLookupIsDeterministic() {
    assertEquals(JsonSchemaKeyword.TYPE, JsonSchemaKeyword.fromKeyword("type").orElseThrow());
    assertTrue(JsonSchemaKeyword.fromKeyword("notAKeyword").isEmpty());
    assertTrue(JsonSchemaKeyword.ONE_OF.supportedInV1());
    assertTrue(JsonSchemaKeyword.DESCRIPTION.acceptedInV1());
    assertFalse(JsonSchemaKeyword.REF.supportedInV1());
  }

  @Test
  void acceptsSupportedKeywordShapes() {
    assertValid(
        """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "title": "Example",
          "description": "Accepted annotation",
          "$comment": "Ignored comment",
          "examples": [],
          "deprecated": false,
          "readOnly": false,
          "writeOnly": false,
          "type": "object",
          "properties": {
            "id": {"type": "string", "minLength": 1, "maxLength": 36, "pattern": "^[a-z]+$", "format": "uuid"},
            "count": {"type": "integer", "minimum": 0, "maximum": 10, "exclusiveMinimum": -1, "exclusiveMaximum": 11},
            "tags": {"type": "array", "items": {"type": "string"}, "minItems": 0, "maxItems": 3},
            "nullable": {"type": ["null", "string"], "default": null}
          },
          "required": ["id"],
          "additionalProperties": false,
          "enum": [{"id": "abc"}],
          "const": {"id": "abc"}
        }
        """);
  }

  @Test
  void acceptsHomogeneousArrayKeywordShapes() {
    assertValid(
        """
        {
          "type": "object",
          "properties": {
            "strings": {"type": "array", "items": {"type": "string"}, "minItems": 1, "maxItems": 3},
            "integers": {"type": "array", "items": {"type": "integer"}},
            "numbers": {"type": "array", "items": {"type": "number"}},
            "booleans": {"type": "array", "items": {"type": "boolean"}}
          },
          "additionalProperties": false
        }
        """);
  }

  @Test
  void ignoresUnknownExtensionKeywordsAsAnnotations() {
    assertValid("{\"type\":\"object\",\"x-extension\":{\"$ref\":\"annotation text\"}}");
  }

  @Test
  void reportsUnsupportedKeywordsWithExactPointersAndDeterministicOrdering() {
    List<SchemaSupportDiagnostic> diagnostics =
        validate("{\"properties\":{\"b\":{\"$ref\":\"x\"},\"a\":{\"allOf\":[]}}}");

    assertEquals(2, diagnostics.size());
    assertEquals("/properties/a/allOf", diagnostics.get(0).pointer().value());
    assertEquals("/properties/b/$ref", diagnostics.get(1).pointer().value());
    assertEquals(SchemaSupportProfile.UNSUPPORTED_KEYWORD_CODE, diagnostics.get(0).code());
  }

  @Test
  void reportsInvalidKeywordValueShapes() {
    assertInvalid("{\"type\": 1}", "/type");
    assertInvalid("{\"type\": \"not-a-type\"}", "/type");
    assertInvalid("{\"type\": [\"string\", \"string\"]}", "/type/1");
    assertInvalid("{\"required\": \"id\"}", "/required");
    assertInvalid("{\"required\": [\"id\", \"id\"]}", "/required/1");
    assertInvalid("{\"properties\": []}", "/properties");
    assertInvalid("{\"properties\": {\"id\": \"bad\"}}", "/properties/id");
    assertInvalid("{\"items\": 1}", "/items");
    assertInvalid("{\"items\": [{\"type\": \"string\"}]}", "/items");
    assertInvalid("{\"minItems\": -1}", "/minItems");
    assertInvalid("{\"maxItems\": 1.5}", "/maxItems");
    assertInvalid("{\"minLength\": \"1\"}", "/minLength");
    assertInvalid("{\"maximum\": \"10\"}", "/maximum");
    assertInvalid("{\"pattern\": 1}", "/pattern");
    assertInvalid("{\"format\": true}", "/format");
    assertInvalid("{\"enum\": \"open\"}", "/enum");
    assertInvalid("{\"oneOf\": []}", "/oneOf");
  }

  @Test
  void reportsUnsupportedKeywordValueShapes() {
    assertUnsupportedValue("{\"additionalProperties\": true}", "/additionalProperties");
    assertUnsupportedValue("{\"additionalProperties\": {}}", "/additionalProperties");
    assertUnsupportedValue("{\"type\": [\"string\", \"number\"]}", "/type");
    assertUnsupportedValue("{\"type\": [\"null\"]}", "/type");
    assertUnsupportedValue("{\"format\": \"email\"}", "/format");
    assertUnsupportedValue("{\"properties\": {\"id\": true}}", "/properties/id");
    assertUnsupportedValue("{\"items\": false}", "/items");
    assertUnsupportedKeyword("{\"prefixItems\": [{\"type\":\"string\"}]}", "/prefixItems");
    assertUnsupportedValue("{\"oneOf\": [{\"type\":\"string\"}, {\"type\":\"number\"}]}", "/oneOf");
  }

  private static void assertValid(String source) {
    assertTrue(validate(source).isEmpty());
  }

  private static void assertInvalid(String source, String pointer) {
    assertDiagnostic(source, pointer, SchemaSupportProfile.INVALID_KEYWORD_VALUE_CODE);
  }

  private static void assertUnsupportedValue(String source, String pointer) {
    assertDiagnostic(source, pointer, SchemaSupportProfile.UNSUPPORTED_KEYWORD_VALUE_CODE);
  }

  private static void assertUnsupportedKeyword(String source, String pointer) {
    assertDiagnostic(source, pointer, SchemaSupportProfile.UNSUPPORTED_KEYWORD_CODE);
  }

  private static void assertDiagnostic(String source, String pointer, String code) {
    List<SchemaSupportDiagnostic> diagnostics = validate(source);

    assertFalse(diagnostics.isEmpty());
    assertEquals(code, diagnostics.getFirst().code());
    assertEquals(pointer, diagnostics.getFirst().pointer().value());
  }

  private static List<SchemaSupportDiagnostic> validate(String source) {
    SchemaSyntaxParseResult parseResult = SchemaSyntaxParser.parse(source);

    assertTrue(parseResult.successful());
    return SchemaSupportProfile.validate(parseResult.root());
  }
}
