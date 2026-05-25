package io.github.mundanej.mjjb.generator.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjjb.generator.api.GeneratorProfile;
import io.github.mundanej.mjjb.generator.api.GeneratorRequest;
import io.github.mundanej.mjjb.generator.api.GeneratorResult;
import io.github.mundanej.mjjb.generator.core.generated.GeneratedSourceVerifier;
import io.github.mundanej.mjjb.schema.model.SchemaReferenceResolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class CoreGeneratorTest {
  @TempDir Path tempDir;
  private final GeneratedSourceVerifier generatedSourceVerifier = new GeneratedSourceVerifier();
  private final GeneratedFixtureAssertions fixtureAssertions = new GeneratedFixtureAssertions();

  @Test
  void rejectsUnsupportedDraft202012KeywordWithDeterministicDiagnostic() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(
        schema,
        "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"properties\":{\"id\":{\"$ref\":\"x\"}}}");

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schema), tempDir.resolve("out")));

    assertFalse(result.successful());
    assertTrue(result.diagnostics().getFirst().message().contains("$ref"));
    assertTrue(result.diagnostics().getFirst().schemaPointer().contains("/properties/id/$ref"));
  }

  @Test
  void writesEmptyObjectRecordMatchingGoldenSource() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(
        schema,
        "{\"type\":\"object\",\"description\":\"mentions $ref and allOf as text\",\"properties\":{},\"additionalProperties\":false}");

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schema), tempDir.resolve("out")));

    fixtureAssertions.verifyDefaultFixture(
        "empty-object", result, tempDir.resolve("empty-object-classes"));
  }

  @Test
  void writesScalarObjectRecordMatchingGoldenSource() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(
        schema,
        """
        {
          "type": "object",
          "properties": {
            "id": {"type": "string"},
            "count": {"type": "integer"},
            "displayName": {"type": "string"},
            "score": {"type": "number"},
            "active": {"type": "boolean"}
          },
          "required": ["id", "count"],
          "additionalProperties": false
        }
        """);

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schema), tempDir.resolve("out")));

    fixtureAssertions.verifyDefaultFixture(
        "mixed-scalar", result, tempDir.resolve("mixed-scalar-classes"));
  }

  @Test
  void writesScalarArrayObjectRecordMatchingGoldenSource() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(
        schema,
        """
        {
          "type": "object",
          "properties": {
            "tags": {"type": "array", "items": {"type": "string"}, "minItems": 1, "maxItems": 3},
            "counts": {"type": "array", "items": {"type": "integer"}},
            "scores": {"type": "array", "items": {"type": "number"}, "maxItems": 2},
            "flags": {"type": "array", "items": {"type": "boolean"}}
          },
          "required": ["tags", "counts"],
          "additionalProperties": false
        }
        """);

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schema), tempDir.resolve("out")));

    fixtureAssertions.verifyDefaultFixture(
        "array-scalar", result, tempDir.resolve("array-scalar-classes"));
  }

  @Test
  void writesFacetConstrainedObjectRecordMatchingGoldenSource() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(
        schema,
        """
        {
          "type": "object",
          "properties": {
            "code": {"type": "string", "minLength": 2, "maxLength": 4},
            "symbol": {"type": "string", "pattern": "^[A-Z]+$"},
            "label": {"type": "string", "maxLength": 4},
            "eventDate": {"type": "string", "format": "date"},
            "createdAt": {"type": "string", "format": "date-time"},
            "identifier": {"type": "string", "format": "uuid"},
            "count": {"type": "integer", "minimum": 1, "maximum": 10},
            "ratio": {"type": "number", "exclusiveMinimum": 0, "exclusiveMaximum": 1},
            "names": {"type": "array", "items": {"type": "string", "minLength": 2, "pattern": "^[a-z]+$"}},
            "scores": {"type": "array", "items": {"type": "number", "minimum": 0, "maximum": 100}}
          },
          "required": ["code", "count", "names"],
          "additionalProperties": false
        }
        """);

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schema), tempDir.resolve("out")));

    fixtureAssertions.verifyDefaultFixture(
        "facet-constraints", result, tempDir.resolve("facet-constraints-classes"));
  }

  @Test
  void writesLiteralConstrainedObjectRecordMatchingGoldenSource() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(
        schema,
        """
        {
          "type": "object",
          "properties": {
            "status": {"type": "string", "enum": ["open", "closed", null], "default": "open"},
            "kind": {"type": "string", "const": "record"},
            "priority": {"type": "integer", "enum": [1, 2, null], "default": 1},
            "score": {"type": "number", "enum": [1.5, 2e0], "const": 1.5, "default": 1.5},
            "active": {"type": "boolean", "const": true, "default": true},
            "voided": {"type": "string", "const": null, "default": null},
            "tags": {"type": "array", "items": {"type": "string", "enum": ["red", "blue", null]}},
            "flags": {"type": "array", "items": {"type": "boolean", "const": true}}
          },
          "required": ["status", "kind", "tags"],
          "additionalProperties": false
        }
        """);

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schema), tempDir.resolve("out")));

    fixtureAssertions.verifyDefaultFixture(
        "literal-constraints", result, tempDir.resolve("literal-constraints-classes"));
  }

  @Test
  void writesNullableObjectRecordMatchingGoldenSource() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(
        schema,
        """
        {
          "type": "object",
          "properties": {
            "nickname": {"type": ["null", "string"], "minLength": 2, "enum": ["Ada", null], "default": null},
            "status": {"type": ["string", "null"], "const": null},
            "code": {"type": ["null", "string"], "const": "OK", "default": "OK"},
            "score": {"type": ["null", "number"], "maximum": 10},
            "tags": {"type": ["null", "array"], "items": {"type": "string", "minLength": 3, "enum": ["red", "blue"]}, "minItems": 1},
            "flags": {"type": ["array", "null"], "items": {"type": "boolean", "const": true}}
          },
          "required": ["nickname", "tags"],
          "additionalProperties": false
        }
        """);

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schema), tempDir.resolve("out")));

    fixtureAssertions.verifyDefaultFixture(
        "nullable-fields", result, tempDir.resolve("nullable-fields-classes"));
  }

  @Test
  void writesTaggedOneOfSourcesMatchingGoldenSource() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(
        schema,
        """
        {
          "oneOf": [
            {
              "type": "object",
              "properties": {
                "kind": {"type": "string", "const": "card"},
                "last4": {"type": "string", "minLength": 4, "maxLength": 4},
                "amount": {"type": "number", "minimum": 0},
                "labels": {"type": "array", "items": {"type": "string", "minLength": 2}, "minItems": 1}
              },
              "required": ["kind", "last4", "amount"],
              "additionalProperties": false
            },
            {
              "type": "object",
              "properties": {
                "kind": {"type": "string", "const": "bank-transfer"},
                "iban": {"type": "string", "minLength": 8},
                "urgent": {"type": "boolean"},
                "memo": {"type": ["null", "string"], "enum": ["payroll", null]}
              },
              "required": ["kind", "iban"],
              "additionalProperties": false
            }
          ]
        }
        """);

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schema), tempDir.resolve("out")));

    fixtureAssertions.verifyDefaultFixture(
        "tagged-oneof", result, tempDir.resolve("tagged-oneof-classes"));
  }

  @Test
  void writesBasicObjectMetadataHelperWhenEnabled() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(
        schema,
        """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "title": "Metadata Root",
          "description": "Root description",
          "$comment": "Root comment",
          "examples": [{"id": "abc", "count": 3}],
          "deprecated": false,
          "readOnly": true,
          "writeOnly": false,
          "type": "object",
          "properties": {
            "id": {
              "type": "string",
              "title": "Identifier",
              "description": "Stable identifier",
              "examples": ["abc", "def"],
              "default": "abc"
            },
            "count": {
              "type": "integer",
              "default": 3
            },
            "tags": {
              "type": ["null", "array"],
              "items": {"type": "string"},
              "examples": [["red", "blue"]]
            }
          },
          "required": ["id"],
          "additionalProperties": false
        }
        """);

    GeneratorResult result = generateWithMetadata(schema);

    fixtureAssertions.verifyMetadataFixture(
        "metadata-basic", result, tempDir.resolve("metadata-basic-classes"));
  }

  @Test
  void writesTaggedOneOfMetadataHelperWhenEnabled() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(
        schema,
        """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "title": "Payment",
          "oneOf": [
            {
              "title": "Card branch",
              "type": "object",
              "properties": {
                "kind": {"type": "string", "const": "card"},
                "last4": {"type": "string", "title": "Last four", "default": "0000"},
                "amount": {"type": "number"}
              },
              "required": ["kind", "last4", "amount"],
              "additionalProperties": false
            },
            {
              "title": "Bank branch",
              "type": "object",
              "properties": {
                "kind": {"type": "string", "const": "bank-transfer"},
                "iban": {"type": "string"},
                "urgent": {"type": "boolean", "default": false}
              },
              "required": ["kind", "iban"],
              "additionalProperties": false
            }
          ]
        }
        """);

    GeneratorResult result = generateWithMetadata(schema);

    fixtureAssertions.verifyMetadataFixture(
        "metadata-tagged", result, tempDir.resolve("metadata-tagged-classes"));
  }

  @Test
  void writesCustomRootTypeSources() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(
        schema, "{\"type\":\"object\",\"properties\":{},\"additionalProperties\":false}");

    GeneratorResult result =
        new CoreGenerator()
            .generate(
                new GeneratorRequest(
                    List.of(schema),
                    tempDir.resolve("out"),
                    null,
                    "com.example.generated",
                    "CustomRoot",
                    Map.of()));

    assertTrue(result.successful());
    assertEquals(4, result.generatedSources().size());
    Path modelSource = GeneratedFixtureAssertions.sourceNamed(result, "CustomRoot.java");
    Path writerSource = GeneratedFixtureAssertions.sourceNamed(result, "CustomRootJsonWriter.java");
    Path readerSource = GeneratedFixtureAssertions.sourceNamed(result, "CustomRootJsonReader.java");
    Path validatorSource =
        GeneratedFixtureAssertions.sourceNamed(result, "CustomRootJsonValidator.java");
    assertTrue(Files.readString(modelSource).contains("public record CustomRoot()"));
    assertTrue(
        Files.readString(writerSource)
            .contains("public static void write(JsonWriter writer, CustomRoot value)"));
    assertTrue(
        Files.readString(readerSource)
            .contains("public static CustomRoot read(JsonReader reader)"));
    assertTrue(
        Files.readString(validatorSource)
            .contains("public static ValidationResult validate(CustomRoot value)"));
    generatedSourceVerifier.compileGeneratedSources(
        "custom-root-type", result.generatedSources(), tempDir.resolve("custom-root-classes"));
  }

  @Test
  void reportsUnsupportedBindingShapesBeforeEmission() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Path output = tempDir.resolve("out");
    Files.writeString(
        schema,
        """
        {
          "type": "object",
          "properties": {
            "status": {"enum": ["open", "closed"]},
            "count": {"type": "integer", "minimum": 0, "maximum": 10},
            "flags": {"type": "array", "items": {"type": "array"}}
          },
          "required": ["status"],
          "additionalProperties": false
        }
        """);

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schema), output));

    assertFalse(result.successful());
    assertTrue(result.generatedSources().isEmpty());
    assertFalse(
        Files.exists(output.resolve("io/github/mundanej/mjjb/generated/GeneratedBindings.java")));
    assertFalse(
        Files.exists(
            output.resolve("io/github/mundanej/mjjb/generated/GeneratedBindingsJsonWriter.java")));
    assertFalse(
        Files.exists(
            output.resolve("io/github/mundanej/mjjb/generated/GeneratedBindingsJsonReader.java")));
    assertFalse(
        Files.exists(
            output.resolve(
                "io/github/mundanej/mjjb/generated/GeneratedBindingsJsonValidator.java")));
    assertEquals(
        List.of("MJJBG-BINDING-UNSUPPORTED-PROPERTY-TYPE", "MJJBG-BINDING-MISSING-PROPERTY-TYPE"),
        result.diagnostics().stream().map(diagnostic -> diagnostic.code()).toList());
    assertEquals(
        List.of("/properties/flags/items/type", "/properties/status"),
        result.diagnostics().stream().map(diagnostic -> diagnostic.schemaPointer()).toList());
  }

  @Test
  void reportsBindingDiagnosticsInDeterministicOrder() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(
        schema,
        """
        {
          "type": "object",
          "properties": {
            "z": {"type": "array"},
            "a": {}
          },
          "required": ["missing"],
          "additionalProperties": false
        }
        """);

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schema), tempDir.resolve("out")));

    assertFalse(result.successful());
    assertEquals(
        List.of("/properties/a", "/properties/z", "/required/0"),
        result.diagnostics().stream().map(diagnostic -> diagnostic.schemaPointer()).toList());
    assertEquals(
        List.of(
            "MJJBG-BINDING-MISSING-PROPERTY-TYPE",
            "MJJBG-BINDING-MISSING-ARRAY-ITEMS",
            "MJJBG-BINDING-UNKNOWN-REQUIRED"),
        result.diagnostics().stream().map(diagnostic -> diagnostic.code()).toList());
  }

  @Test
  void reportsUnsupportedKeywordInsideArrayWithPointer() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(schema, "{\"items\":{\"anyOf\":[]}}");

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schema), tempDir.resolve("out")));

    assertFalse(result.successful());
    assertEquals("/items/anyOf", result.diagnostics().getFirst().schemaPointer());
    assertEquals("MJJBG-SCHEMA-UNSUPPORTED-KEYWORD", result.diagnostics().getFirst().code());
  }

  @Test
  void reportsUnsupportedKeywordWithEscapedPointer() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(schema, "{\"properties\":{\"a/b\":{\"not\":{}}}}");

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schema), tempDir.resolve("out")));

    assertFalse(result.successful());
    assertEquals("/properties/a~1b/not", result.diagnostics().getFirst().schemaPointer());
  }

  @Test
  void reportsUnsupportedKeywordValuesBeforeEmission() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Path output = tempDir.resolve("out");
    Files.writeString(schema, "{\"type\":\"object\",\"additionalProperties\":true}");

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schema), output));

    assertFalse(result.successful());
    assertTrue(result.generatedSources().isEmpty());
    assertFalse(
        Files.exists(output.resolve("io/github/mundanej/mjjb/generated/GeneratedBindings.java")));
    assertFalse(
        Files.exists(
            output.resolve("io/github/mundanej/mjjb/generated/GeneratedBindingsJsonWriter.java")));
    assertFalse(
        Files.exists(
            output.resolve("io/github/mundanej/mjjb/generated/GeneratedBindingsJsonReader.java")));
    assertFalse(
        Files.exists(
            output.resolve(
                "io/github/mundanej/mjjb/generated/GeneratedBindingsJsonValidator.java")));
    assertEquals("MJJBG-SCHEMA-UNSUPPORTED-KEYWORD-VALUE", result.diagnostics().getFirst().code());
    assertEquals("/additionalProperties", result.diagnostics().getFirst().schemaPointer());
  }

  @Test
  void reportsInvalidKeywordValuesBeforeEmission() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(schema, "{\"type\":\"object\",\"required\":\"id\"}");

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schema), tempDir.resolve("out")));

    assertFalse(result.successful());
    assertEquals("MJJBG-SCHEMA-INVALID-KEYWORD-VALUE", result.diagnostics().getFirst().code());
    assertEquals("/required", result.diagnostics().getFirst().schemaPointer());
  }

  @Test
  void generatesEquivalentBindingsForLocalReferences() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(
        schema,
        """
        {
          "type": "object",
          "properties": {
            "id": {"$ref": "#/$defs/id"},
            "profile": {"$ref": "#/$defs/profile"}
          },
          "required": ["id", "profile"],
          "additionalProperties": false,
          "$defs": {
            "id": {"type": "string", "minLength": 1},
            "profile": {
              "type": "object",
              "properties": {
                "name": {"$ref": "#/$defs/name"}
              },
              "required": ["name"],
              "additionalProperties": false
            },
            "name": {"type": "string"}
          }
        }
        """);

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schema), tempDir.resolve("out")));

    assertTrue(result.successful());
    String modelSource =
        Files.readString(GeneratedFixtureAssertions.sourceNamed(result, "GeneratedBindings.java"));
    assertTrue(modelSource.contains("GeneratedBindingsProfile profile"));
    assertTrue(modelSource.contains("record GeneratedBindingsProfile("));
  }

  @Test
  void resolvesTaggedOneOfBranchesBeforeProfileValidation() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(
        schema,
        """
        {
          "oneOf": [
            {"$ref": "#/$defs/card"},
            {"$ref": "#/$defs/bank"}
          ],
          "$defs": {
            "card": {
              "type": "object",
              "properties": {
                "kind": {"type": "string", "const": "card"},
                "last4": {"type": "string"}
              },
              "required": ["kind", "last4"],
              "additionalProperties": false
            },
            "bank": {
              "type": "object",
              "properties": {
                "kind": {"type": "string", "const": "bank"},
                "iban": {"type": "string"}
              },
              "required": ["kind", "iban"],
              "additionalProperties": false
            }
          }
        }
        """);

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schema), tempDir.resolve("out")));

    assertTrue(result.successful());
    String modelSource =
        Files.readString(GeneratedFixtureAssertions.sourceNamed(result, "GeneratedBindings.java"));
    assertTrue(modelSource.contains("sealed interface GeneratedBindings"));
    assertTrue(modelSource.contains("record Card("));
    assertTrue(modelSource.contains("record Bank("));
  }

  @Test
  void reportsReferenceResolutionDiagnosticsBeforeEmission() throws IOException {
    Path remote = tempDir.resolve("remote.json");
    Files.writeString(remote, "{\"$ref\":\"https://example.com/schema.json\"}");
    Path missing = tempDir.resolve("missing.json");
    Files.writeString(missing, "{\"$ref\":\"#/$defs/missing\",\"$defs\":{}}");
    Path cycle = tempDir.resolve("cycle.json");
    Files.writeString(
        cycle,
        "{\"$ref\":\"#/$defs/a\",\"$defs\":{\"a\":{\"$ref\":\"#/$defs/b\"},\"b\":{\"$ref\":\"#/$defs/a\"}}}");

    GeneratorResult remoteResult =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(remote), tempDir.resolve("out1")));
    GeneratorResult missingResult =
        new CoreGenerator()
            .generate(GeneratorRequest.of(List.of(missing), tempDir.resolve("out2")));
    GeneratorResult cycleResult =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(cycle), tempDir.resolve("out3")));

    assertEquals(
        SchemaReferenceResolver.REMOTE_REF_CODE, remoteResult.diagnostics().getFirst().code());
    assertEquals("/$ref", remoteResult.diagnostics().getFirst().schemaPointer());
    assertEquals(
        SchemaReferenceResolver.MISSING_REF_CODE, missingResult.diagnostics().getFirst().code());
    assertEquals("/$ref", missingResult.diagnostics().getFirst().schemaPointer());
    assertEquals(
        SchemaReferenceResolver.CYCLIC_REF_CODE, cycleResult.diagnostics().getFirst().code());
    assertEquals("/$defs/a/$ref", cycleResult.diagnostics().getFirst().schemaPointer());
  }

  @Test
  void reportsProfileDiagnosticsInDeterministicOrder() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(
        schema, "{\"properties\":{\"b\":{\"$dynamicRef\":\"#x\"},\"a\":{\"anyOf\":[]}}}");

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schema), tempDir.resolve("out")));

    assertFalse(result.successful());
    assertEquals("/properties/a/anyOf", result.diagnostics().get(0).schemaPointer());
    assertEquals("/properties/b/$dynamicRef", result.diagnostics().get(1).schemaPointer());
  }

  @Test
  void reportsInvalidJsonSchemaInput() throws IOException {
    assertInvalidSchema("{\"type\":\"object\"", "");
  }

  @Test
  void generatedProductionSourcesKeepRuntimeOnlyDependencyBoundary() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(
        schema,
        """
        {
          "type": "object",
          "properties": {
            "id": {"type": "string", "minLength": 1},
            "tags": {"type": "array", "items": {"type": "string"}, "minItems": 1},
            "score": {"type": ["null", "number"], "maximum": 10},
            "active": {"type": "boolean", "default": true}
          },
          "required": ["id", "tags"],
          "additionalProperties": false
        }
        """);

    GeneratorResult result = generateWithMetadata(schema);

    assertTrue(result.successful());
    assertEquals(5, result.generatedSources().size());
    for (Path source : result.generatedSources()) {
      String content = Files.readString(source);
      generatedSourceVerifier.verifyAllowedTokens("generated-dependency-boundary", source);
      assertTrue(
          content
              .lines()
              .filter(line -> line.startsWith("import "))
              .allMatch(
                  line ->
                      line.startsWith("import java.")
                          || line.startsWith("import io.github.mundanej.mjjb.runtime.")),
          source + " must import only JDK and runtime-core types");
    }
  }

  @Test
  void generatedPatternChecksUseConstantsOutsideHotPaths() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(
        schema,
        """
        {
          "type": "object",
          "properties": {
            "id": {"type": "string", "pattern": "^[A-Z]+$"}
          },
          "patternProperties": {
            "^x-": {"type": "string", "pattern": "^[a-z]+$"}
          },
          "propertyNames": {"pattern": "^[A-Za-z0-9_-]+$"},
          "required": ["id"],
          "additionalProperties": {"type": "string"}
        }
        """);

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schema), tempDir.resolve("out")));

    assertTrue(result.successful());
    String modelSource =
        Files.readString(GeneratedFixtureAssertions.sourceNamed(result, "GeneratedBindings.java"));
    String readerSource =
        Files.readString(
            GeneratedFixtureAssertions.sourceNamed(result, "GeneratedBindingsJsonReader.java"));
    String validatorSource =
        Files.readString(
            GeneratedFixtureAssertions.sourceNamed(result, "GeneratedBindingsJsonValidator.java"));
    assertTrue(modelSource.contains("private static final Pattern PATTERN_1"));
    assertTrue(readerSource.contains("private static final Pattern PATTERN_1"));
    assertTrue(validatorSource.contains("private static final Pattern PATTERN_1"));
    assertTrue(validatorSource.contains("private static Pattern compiledPattern(String pattern)"));
    assertFalse(modelSource.contains("key -> Pattern.compile("));
    assertFalse(readerSource.contains("if (Pattern.compile("));
    assertFalse(validatorSource.contains("Pattern.compile(pattern)"));
    generatedSourceVerifier.compileGeneratedSources(
        "pattern-constants", result.generatedSources(), tempDir.resolve("pattern-classes"));
  }

  @Test
  void reportsInvalidJsonNumbersInSchemaInput() throws IOException {
    assertInvalidSchema("{\"minimum\":1.}", "/minimum");
    assertInvalidSchema("{\"minimum\":1e}", "/minimum");
    assertInvalidSchema("{\"minimum\":-}", "/minimum");
    assertInvalidSchema("{\"minimum\":01}", "/minimum");
  }

  @Test
  void reportsInvalidJsonStringsInSchemaInput() throws IOException {
    assertInvalidSchema("{\"description\":\"bad\\q\"}", "/description");
    assertInvalidSchema("{\"description\":\"bad\nstring\"}", "/description");
  }

  @Test
  void reportsTrailingJsonSchemaInputWithRootPointer() throws IOException {
    assertInvalidSchema("{\"type\":\"object\"} []", "");
  }

  @Test
  void reportsMissingAndEmptyInputs() {
    GeneratorResult empty =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(), tempDir.resolve("out")));
    GeneratorResult missing =
        new CoreGenerator()
            .generate(
                GeneratorRequest.of(
                    List.of(tempDir.resolve("missing.json")), tempDir.resolve("out")));

    assertEquals("MJJBG-GEN-001", empty.diagnostics().getFirst().code());
    assertEquals("MJJBG-GEN-003", missing.diagnostics().getFirst().code());
  }

  private void assertInvalidSchema(String source, String pointer) throws IOException {
    Path schema = Files.createTempFile(tempDir, "schema", ".json");
    Files.writeString(schema, source);

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schema), tempDir.resolve("out")));

    assertFalse(result.successful());
    assertEquals("MJJBG-SCHEMA-INVALID-JSON", result.diagnostics().getFirst().code());
    assertEquals(pointer, result.diagnostics().getFirst().schemaPointer());
  }

  private GeneratorResult generateWithMetadata(Path schema) {
    return new CoreGenerator()
        .generate(
            new GeneratorRequest(
                List.of(schema),
                tempDir.resolve("out"),
                GeneratorProfile.JSP_DATA_2020_12,
                GeneratorRequest.DEFAULT_PACKAGE,
                GeneratorRequest.DEFAULT_ROOT_TYPE_NAME,
                Map.of(),
                true));
  }
}
