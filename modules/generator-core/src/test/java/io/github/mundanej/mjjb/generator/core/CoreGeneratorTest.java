package io.github.mundanej.mjjb.generator.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjjb.generator.api.GeneratorRequest;
import io.github.mundanej.mjjb.generator.api.GeneratorResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class CoreGeneratorTest {
  @TempDir Path tempDir;

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
  void writesInitialGeneratedSourceForSupportedSchema() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(
        schema,
        "{\"type\":\"object\",\"description\":\"mentions $ref and allOf as text\",\"properties\":{},\"additionalProperties\":false}");

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schema), tempDir.resolve("out")));

    assertTrue(result.successful());
    assertTrue(Files.isRegularFile(result.generatedSources().getFirst()));
  }

  @Test
  void acceptsSupportedSchemaWithArraysLiteralsAndNumbers() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(
        schema,
        """
        {
          "type": "object",
          "properties": {
            "status": {"enum": ["open", "closed"]},
            "count": {"type": "integer", "minimum": 0, "maximum": 10},
            "flags": {"type": "array", "items": {"type": "boolean"}, "default": [true, false, null]}
          },
          "required": ["status"],
          "additionalProperties": false
        }
        """);

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schema), tempDir.resolve("out")));

    assertTrue(result.successful());
  }

  @Test
  void reportsUnsupportedKeywordInsideArrayWithPointer() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(schema, "{\"oneOf\":[{\"allOf\":[]}]}");

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schema), tempDir.resolve("out")));

    assertFalse(result.successful());
    assertEquals("/oneOf/0/allOf", result.diagnostics().getFirst().schemaPointer());
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
  void reportsInvalidJsonSchemaInput() throws IOException {
    assertInvalidSchema("{\"type\":\"object\"", "");
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
}
