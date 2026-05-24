package io.github.mundanej.mjjb.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
  void supportedProfileMatrixDocumentsKnownKeywordStatuses() throws IOException {
    Map<String, String> matrix =
        documentedKeywordStatuses(
            Files.readString(repositoryRoot().resolve("docs/supported-profile.md")));

    for (JsonSchemaKeyword keyword : JsonSchemaKeyword.values()) {
      String status = matrix.get(keyword.keyword());

      assertFalse(status == null || status.isBlank(), keyword.keyword());
      assertTrue(allowedDocumentedStatuses(keyword.support()).contains(status), keyword.keyword());
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
    assertTrue(JsonSchemaKeyword.REF.supportedInV1());
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
            "nullable": {"type": ["null", "string"], "enum": ["x", null], "const": null, "default": null}
          },
          "patternProperties": {
            "^x-": {"type": "string"}
          },
          "required": ["id"],
          "additionalProperties": false,
          "minProperties": 1,
          "maxProperties": 4,
          "propertyNames": {"type": "string", "minLength": 1, "maxLength": 32, "pattern": "^[a-z][a-zA-Z0-9-]*$"},
          "dependentRequired": {"id": ["count"]},
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
  void acceptsNestedClosedObjectKeywordShapes() {
    assertValid(
        """
        {
          "type": "object",
          "properties": {
            "profile": {
              "type": "object",
              "properties": {
                "name": {"type": "string"},
                "address": {
                  "type": "object",
                  "properties": {"city": {"type": "string"}},
                  "required": ["city"],
                  "additionalProperties": false
                }
              },
              "required": ["name"],
              "additionalProperties": false
            }
          },
          "required": ["profile"],
          "additionalProperties": false
        }
        """);
  }

  @Test
  void acceptsAdditionalPropertiesMapKeywordShapes() {
    assertValid(
        """
        {
          "type": "object",
          "properties": {
            "id": {"type": "string"}
          },
          "additionalProperties": {"type": "string", "minLength": 1}
        }
        """);
    assertValid(
        """
        {
          "type": "object",
          "additionalProperties": {"type": "array", "items": {"type": "integer"}}
        }
        """);
    assertValid(
        """
        {
          "type": "object",
          "additionalProperties": {
            "type": "object",
            "properties": {"name": {"type": "string"}},
            "additionalProperties": false
          }
        }
        """);
  }

  @Test
  void acceptsLocalReferenceKeywordShapes() {
    assertValid(
        """
        {
          "type": "object",
          "properties": {
            "id": {"$ref": "#/$defs/id"},
            "escaped": {"$ref": "#/$defs/a~1b/c~0d"}
          },
          "required": ["id"],
          "additionalProperties": false,
          "$defs": {
            "id": {"type": "string"},
            "a/b": {
              "c~d": {"type": "integer"}
            }
          }
        }
        """);
  }

  @Test
  void acceptsNullableTypeArrays() {
    assertValid(
        """
        {
          "type": "object",
          "properties": {
            "left": {"type": ["null", "string"]},
            "right": {"type": ["array", "null"], "items": {"type": "integer"}}
          },
          "additionalProperties": false
        }
        """);
  }

  @Test
  void acceptsSupportedFacetShapes() {
    assertValid(
        """
        {
          "type": "object",
          "properties": {
            "date": {"type": "string", "format": "date"},
            "dateTime": {"type": "string", "format": "date-time"},
            "id": {"type": "string", "format": "uuid"},
            "code": {"type": "string", "minLength": 1, "maxLength": 8, "pattern": "^[A-Z]+$"},
            "count": {"type": "integer", "minimum": 1, "maximum": 10},
            "ratio": {"type": "number", "exclusiveMinimum": 0, "exclusiveMaximum": 1}
          },
          "additionalProperties": false
        }
        """);
  }

  @Test
  void acceptsSupportedLiteralConstraintShapes() {
    assertValid(
        """
        {
          "type": "object",
          "properties": {
            "status": {"type": "string", "enum": ["open", "closed", null], "const": "open", "default": "open"},
            "count": {"type": "integer", "enum": [1, 2, null], "const": 1, "default": 1},
            "ratio": {"type": "number", "enum": [1.5, 2e0, null], "const": 1.5, "default": 1.5},
            "active": {"type": "boolean", "enum": [true, false, null], "const": true, "default": true}
          },
          "additionalProperties": false
        }
        """);
  }

  @Test
  void acceptsTaggedOneOfKeywordShape() {
    assertValid(
        """
        {
          "oneOf": [
            {
              "type": "object",
              "properties": {
                "kind": {"type": "string", "const": "card"},
                "last4": {"type": "string"}
              },
              "required": ["kind", "last4"],
              "additionalProperties": false
            },
            {
              "type": "object",
              "properties": {
                "kind": {"type": "string", "const": "bank-transfer"},
                "iban": {"type": "string"}
              },
              "required": ["kind", "iban"],
              "additionalProperties": false
            }
          ]
        }
        """);
  }

  @Test
  void acceptsConstrainedAllOfKeywordShapes() {
    assertValid(
        """
        {
          "allOf": [
            {
              "type": "object",
              "properties": {"id": {"type": "string"}},
              "required": ["id"],
              "additionalProperties": false
            },
            {
              "type": "object",
              "properties": {"count": {"type": "integer"}},
              "additionalProperties": false
            }
          ]
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
        validate("{\"properties\":{\"b\":{\"$dynamicRef\":\"#x\"},\"a\":{\"anyOf\":[]}}}");

    assertEquals(2, diagnostics.size());
    assertEquals("/properties/a/anyOf", diagnostics.get(0).pointer().value());
    assertEquals("/properties/b/$dynamicRef", diagnostics.get(1).pointer().value());
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
    assertInvalid("{\"patternProperties\": []}", "/patternProperties");
    assertInvalid(
        "{\"patternProperties\": {\"[\": {\"type\": \"string\"}}}", "/patternProperties/[");
    assertInvalid("{\"items\": 1}", "/items");
    assertInvalid("{\"items\": [{\"type\": \"string\"}]}", "/items");
    assertInvalid("{\"minItems\": -1}", "/minItems");
    assertInvalid("{\"maxItems\": 1.5}", "/maxItems");
    assertInvalid("{\"minProperties\": -1}", "/minProperties");
    assertInvalid("{\"maxProperties\": 1.5}", "/maxProperties");
    assertInvalid("{\"minLength\": \"1\"}", "/minLength");
    assertInvalid("{\"maximum\": \"10\"}", "/maximum");
    assertInvalid("{\"pattern\": 1}", "/pattern");
    assertInvalid("{\"pattern\": \"[\"}", "/pattern");
    assertInvalid("{\"format\": true}", "/format");
    assertInvalid("{\"propertyNames\": 1}", "/propertyNames");
    assertInvalid("{\"propertyNames\": {\"type\": 1}}", "/propertyNames/type");
    assertInvalid("{\"propertyNames\": {\"pattern\": \"[\"}}", "/propertyNames/pattern");
    assertInvalid("{\"dependentRequired\": []}", "/dependentRequired");
    assertInvalid("{\"dependentRequired\": {\"a\": true}}", "/dependentRequired/a");
    assertInvalid("{\"dependentRequired\": {\"a\": [1]}}", "/dependentRequired/a/0");
    assertInvalid("{\"dependentRequired\": {\"a\": [\"b\", \"b\"]}}", "/dependentRequired/a/1");
    assertInvalid("{\"enum\": \"open\"}", "/enum");
    assertInvalid("{\"enum\": []}", "/enum");
    assertInvalid("{\"enum\": [1, 1.0]}", "/enum/1");
    assertInvalid("{\"oneOf\": []}", "/oneOf");
  }

  @Test
  void reportsUnsupportedKeywordValueShapes() {
    assertUnsupportedValue("{\"additionalProperties\": true}", "/additionalProperties");
    assertInvalid("{\"additionalProperties\": []}", "/additionalProperties");
    assertUnsupportedValue("{\"type\": [\"string\", \"number\"]}", "/type");
    assertUnsupportedValue("{\"type\": [\"null\"]}", "/type");
    assertUnsupportedValue("{\"format\": \"email\"}", "/format");
    assertUnsupportedValue("{\"properties\": {\"id\": true}}", "/properties/id");
    assertUnsupportedValue("{\"propertyNames\": false}", "/propertyNames");
    assertUnsupportedValue("{\"propertyNames\": {\"type\": \"integer\"}}", "/propertyNames/type");
    assertUnsupportedValue("{\"propertyNames\": {\"minimum\": 1}}", "/propertyNames/minimum");
    assertUnsupportedValue(
        "{\"patternProperties\": {\"^x-\": {\"type\":\"string\"}, \"^y-\": {\"type\":\"string\"}}}",
        "/patternProperties");
    assertUnsupportedValue("{\"patternProperties\": {\"^x-\": false}}", "/patternProperties/^x-");
    assertUnsupportedValue("{\"items\": false}", "/items");
    assertUnsupportedKeyword("{\"prefixItems\": [{\"type\":\"string\"}]}", "/prefixItems");
    assertUnsupportedValue("{\"oneOf\": [{\"type\":\"string\"}, {\"type\":\"number\"}]}", "/oneOf");
    assertUnsupportedValue(
        """
        {
          "oneOf": [
            {
              "type": "object",
              "properties": {"kind": {"type": "string", "const": "same"}},
              "required": ["kind"],
              "additionalProperties": false
            },
            {
              "type": "object",
              "properties": {"kind": {"type": "string", "const": "same"}},
              "required": ["kind"],
              "additionalProperties": false
            }
          ]
        }
        """,
        "/oneOf");
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

  private static Set<String> allowedDocumentedStatuses(JsonSchemaKeywordSupport support) {
    return switch (support) {
      case SUPPORTED_BINDING -> Set.of("Supported", "Supported with profile limits");
      case IGNORED_ANNOTATION -> Set.of("Accepted annotation");
      case UNSUPPORTED -> Set.of("Rejected", "Recommended next", "Deferred - poor tradeoff");
    };
  }

  private static Map<String, String> documentedKeywordStatuses(String markdown) {
    HashMap<String, String> statuses = new HashMap<>();
    for (String line : markdown.lines().toList()) {
      if (!line.startsWith("| `")) {
        continue;
      }
      List<String> cells = markdownTableCells(line);
      if (cells.size() < 4) {
        continue;
      }
      String keywordCell = cells.get(0);
      String status = cells.get(2);
      if (keywordCell.startsWith("`") && keywordCell.endsWith("`")) {
        statuses.put(keywordCell.substring(1, keywordCell.length() - 1), status);
      }
    }
    return Map.copyOf(statuses);
  }

  private static List<String> markdownTableCells(String line) {
    ArrayList<String> cells = new ArrayList<>();
    int cellStart = 1;
    int separator = line.indexOf('|', cellStart);
    while (separator >= 0) {
      cells.add(line.substring(cellStart, separator).trim());
      cellStart = separator + 1;
      separator = line.indexOf('|', cellStart);
    }
    return List.copyOf(cells);
  }

  private static Path repositoryRoot() {
    Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
    while (current != null) {
      if (Files.isRegularFile(current.resolve("docs/supported-profile.md"))) {
        return current;
      }
      current = current.getParent();
    }
    throw new IllegalStateException("Unable to locate repository root");
  }
}
