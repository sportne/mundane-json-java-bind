package io.github.mundanej.mjjb.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjjb.generator.api.GeneratorRequest;
import io.github.mundanej.mjjb.generator.api.GeneratorResult;
import io.github.mundanej.mjjb.generator.core.CoreGenerator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ProfileDiagnosticsConformanceTest {
  @TempDir Path tempDir;

  @Test
  void acceptsSupportedAndIgnoredProfileKeywordsThroughGenerator() throws IOException {
    GeneratorResult result =
        generate(
            """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "title": "Accepted",
              "description": "Accepted ignored annotation",
              "type": "object",
              "properties": {"id": {"type": "string", "format": "uuid"}},
              "required": ["id"],
              "additionalProperties": false
            }
            """);

    assertTrue(result.successful());
  }

  @Test
  void acceptsHomogeneousArrayObjectBindingThroughGenerator() throws IOException {
    GeneratorResult result =
        generate(
            """
            {
              "type": "object",
              "properties": {
                "tags": {"type": "array", "items": {"type": "string"}, "minItems": 1, "maxItems": 2}
              },
              "required": ["tags"],
              "additionalProperties": false
            }
            """);

    assertTrue(result.successful());
    assertEquals(4, result.generatedSources().size());
  }

  @Test
  void acceptsSupportedScalarFacetBindingThroughGenerator() throws IOException {
    GeneratorResult result =
        generate(
            """
            {
              "type": "object",
              "properties": {
                "code": {"type": "string", "minLength": 1, "maxLength": 8, "pattern": "^[A-Z]+$"},
                "date": {"type": "string", "format": "date"},
                "count": {"type": "integer", "minimum": 1, "maximum": 10},
                "ratio": {"type": "number", "exclusiveMinimum": 0, "exclusiveMaximum": 1}
              },
              "required": ["code", "count"],
              "additionalProperties": false
            }
            """);

    assertTrue(result.successful());
    assertEquals(4, result.generatedSources().size());
  }

  @Test
  void acceptsSupportedLiteralConstraintBindingThroughGenerator() throws IOException {
    GeneratorResult result =
        generate(
            """
            {
              "type": "object",
              "properties": {
                "status": {"type": "string", "enum": ["open", "closed", null], "default": "open"},
                "count": {"type": "integer", "const": 3, "default": 3},
                "scores": {"type": "array", "items": {"type": "number", "enum": [1.5, 2e0]}}
              },
              "required": ["status"],
              "additionalProperties": false
            }
            """);

    assertTrue(result.successful());
    assertEquals(4, result.generatedSources().size());
  }

  @Test
  void acceptsNullableFieldBindingThroughGenerator() throws IOException {
    GeneratorResult result =
        generate(
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

    assertTrue(result.successful());
    assertEquals(4, result.generatedSources().size());
  }

  @Test
  void rejectsNullableArrayItemBindingThroughGenerator() throws IOException {
    GeneratorResult result =
        generate(
            """
            {
              "type": "object",
              "properties": {
                "names": {"type": "array", "items": {"type": ["null", "string"]}}
              },
              "additionalProperties": false
            }
            """);

    assertFalse(result.successful());
    assertEquals("MJJBG-BINDING-UNSUPPORTED-PROPERTY-TYPE", result.diagnostics().getFirst().code());
    assertEquals("/properties/names/items/type", result.diagnostics().getFirst().schemaPointer());
  }

  @Test
  void rejectsUnsupportedLiteralConstraintBindingThroughGenerator() throws IOException {
    GeneratorResult result =
        generate(
            """
            {
              "type": "object",
              "properties": {
                "tags": {"type": "array", "items": {"type": "string"}, "const": []}
              },
              "additionalProperties": false
            }
            """);

    assertFalse(result.successful());
    assertEquals(
        "MJJBG-BINDING-UNSUPPORTED-LITERAL-CONSTRAINT", result.diagnostics().getFirst().code());
    assertEquals("/properties/tags/const", result.diagnostics().getFirst().schemaPointer());
  }

  @Test
  void rejectsKnownUnsupportedKeywordThroughGenerator() throws IOException {
    GeneratorResult result = generate("{\"$ref\":\"schema.json\"}");

    assertFalse(result.successful());
    assertEquals("MJJBG-SCHEMA-UNSUPPORTED-KEYWORD", result.diagnostics().getFirst().code());
    assertEquals("/$ref", result.diagnostics().getFirst().schemaPointer());
  }

  @Test
  void reportsNestedUnsupportedKeywordsInDeterministicManifestOrder() throws IOException {
    GeneratorResult result =
        generate(
            """
            {
              "type": "object",
              "properties": {
                "z": {"$ref": "other.json"},
                "a": {"allOf": []}
              },
              "additionalProperties": false
            }
            """);

    assertFalse(result.successful());
    assertEquals(2, result.diagnostics().size());
    assertEquals("/properties/a/allOf", result.diagnostics().get(0).schemaPointer());
    assertEquals("/properties/z/$ref", result.diagnostics().get(1).schemaPointer());
    assertTrue(
        result
            .diagnostics()
            .get(0)
            .toManifestLine()
            .contains("MJJBG-SCHEMA-UNSUPPORTED-KEYWORD | "));
    assertTrue(result.diagnostics().get(0).toManifestLine().contains("#/properties/a/allOf | "));
  }

  @Test
  void rejectsUnsupportedProfileKeywordValueThroughGenerator() throws IOException {
    GeneratorResult result = generate("{\"type\":\"object\",\"additionalProperties\":true}");

    assertFalse(result.successful());
    assertEquals("MJJBG-SCHEMA-UNSUPPORTED-KEYWORD-VALUE", result.diagnostics().getFirst().code());
    assertEquals("/additionalProperties", result.diagnostics().getFirst().schemaPointer());
  }

  @Test
  void rejectsTupleArrayFormsThroughGeneratorProfile() throws IOException {
    GeneratorResult prefixItems = generate("{\"prefixItems\":[{\"type\":\"string\"}]}");
    GeneratorResult arrayItems = generate("{\"items\":[{\"type\":\"string\"}]}");

    assertFalse(prefixItems.successful());
    assertEquals("MJJBG-SCHEMA-UNSUPPORTED-KEYWORD", prefixItems.diagnostics().getFirst().code());
    assertEquals("/prefixItems", prefixItems.diagnostics().getFirst().schemaPointer());
    assertFalse(arrayItems.successful());
    assertEquals("MJJBG-SCHEMA-INVALID-KEYWORD-VALUE", arrayItems.diagnostics().getFirst().code());
    assertEquals("/items", arrayItems.diagnostics().getFirst().schemaPointer());
  }

  private GeneratorResult generate(String source) throws IOException {
    Path schema = Files.createTempFile(tempDir, "schema", ".json");
    Files.writeString(schema, source);

    return new CoreGenerator()
        .generate(GeneratorRequest.of(List.of(schema), tempDir.resolve("generated")));
  }
}
