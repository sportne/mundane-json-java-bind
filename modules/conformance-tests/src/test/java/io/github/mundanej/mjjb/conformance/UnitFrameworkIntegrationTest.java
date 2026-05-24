package io.github.mundanej.mjjb.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjjb.generator.api.GeneratorProfile;
import io.github.mundanej.mjjb.generator.api.GeneratorRequest;
import io.github.mundanej.mjjb.generator.api.GeneratorResult;
import io.github.mundanej.mjjb.generator.core.CoreGenerator;
import io.github.mundanej.mjjb.parser.JsonStreamReader;
import io.github.mundanej.mjjb.parser.JsonStringWriter;
import io.github.mundanej.mjjb.runtime.JsonField;
import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.JsonReader;
import io.github.mundanej.mjjb.runtime.JsonToken;
import io.github.mundanej.mjjb.runtime.JsonWriteException;
import io.github.mundanej.mjjb.runtime.JsonWriter;
import io.github.mundanej.mjjb.schema.model.SchemaSupportDiagnostic;
import io.github.mundanej.mjjb.schema.model.SchemaSupportProfile;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxParseResult;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class UnitFrameworkIntegrationTest {
  @TempDir Path tempDir;

  @Test
  void parserAndWriterInteropThroughRuntimeInterfaces()
      throws JsonReadException, JsonWriteException {
    JsonReader reader =
        new JsonStreamReader(
            "{\"id\":\"u-1\",\"memo\":null,\"values\":[\"a\",2,true],\"nested\":{\"ok\":false}}");
    JsonStringWriter stringWriter = new JsonStringWriter();
    JsonWriter writer = stringWriter;

    reader.beginObject();
    writer.beginObject();
    assertEquals("id", reader.nextName());
    writer.name("id");
    writer.value(reader.nextString());
    assertEquals("memo", reader.nextName());
    JsonField<String> memo = reader.nextNullableString();
    writer.name("memo");
    if (memo.isExplicitNull()) {
      writer.nullValue();
    }
    assertEquals("values", reader.nextName());
    writer.name("values");
    reader.beginArray();
    writer.beginArray();
    writer.value(reader.nextString());
    writer.number(reader.nextNumberLiteral());
    writer.value(reader.nextBoolean());
    assertFalse(reader.hasNext());
    reader.endArray();
    writer.endArray();
    assertEquals("nested", reader.nextName());
    reader.skipValue();
    assertFalse(reader.hasNext());
    reader.endObject();
    writer.endObject();

    assertEquals(JsonToken.END_DOCUMENT, reader.peek());
    assertEquals("{\"id\":\"u-1\",\"memo\":null,\"values\":[\"a\",2,true]}", stringWriter.json());
  }

  @Test
  void schemaParserSupportProfileAndGeneratorMetadataWorkTogether() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(
        schema,
        """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "title": "Example",
          "type": "object",
          "properties": {
            "id": {"type": "string", "description": "Identifier"},
            "count": {"type": "integer", "minimum": 0}
          },
          "required": ["id"],
          "additionalProperties": false
        }
        """);

    String source = Files.readString(schema);
    SchemaSyntaxParseResult parseResult = SchemaSyntaxParser.parse(source);
    assertTrue(parseResult.successful());
    List<SchemaSupportDiagnostic> supportDiagnostics =
        SchemaSupportProfile.validate(parseResult.root());
    assertEquals(List.of(), supportDiagnostics);

    Path output = tempDir.resolve("generated");
    GeneratorResult result =
        new CoreGenerator()
            .generate(
                new GeneratorRequest(
                    List.of(schema),
                    output,
                    GeneratorProfile.JSP_DATA_2020_12,
                    "com.example.generated",
                    "ExampleBinding",
                    Map.of("https://schemas.example.test/example", "com.example.generated"),
                    true));

    assertTrue(result.successful());
    assertEquals(5, result.generatedSources().size());
    assertTrue(
        Files.readString(sourceNamed(result, "ExampleBindingJsonSchemaMetadata.java"))
            .contains("SchemaRootMetadata"));
  }

  @Test
  void schemaParserSupportProfilePropagatesEscapedUnsupportedPointers() {
    SchemaSyntaxParseResult parseResult =
        SchemaSyntaxParser.parse(
            "{\"properties\":{\"a/b\":{\"$dynamicRef\":\"#x\"},\"c~d\":{\"allOf\":[]}}}");

    List<String> pointers =
        SchemaSupportProfile.validate(parseResult.root()).stream()
            .map(diagnostic -> diagnostic.pointer().value())
            .toList();

    assertEquals(List.of("/properties/a~1b/$dynamicRef", "/properties/c~0d/allOf"), pointers);
  }

  @Test
  void performanceEvidenceWritesV2ReportShape()
      throws IOException, ClassNotFoundException, NoSuchMethodException, JsonReadException {
    Path reportDirectory = tempDir.resolve("performance-report");
    Path workspace = tempDir.resolve("performance-workspace");

    PerformanceEvidence.main(
        new String[] {
          reportDirectory.toString(),
          workspace.toString(),
          System.getProperty("java.class.path", ""),
          "--quick"
        });

    String report = Files.readString(reportDirectory.resolve("performance-evidence.md"));

    assertTrue(report.contains("generator-rich-generate"));
    assertTrue(report.contains("generated-rich-compile"));
    assertTrue(report.contains("generated-rich-read-validate-write"));
    assertTrue(report.contains("## Generated Binding Artifact Summary"));
    assertTrue(
        report.contains("| Schema bytes | Generated source files | Generated source bytes |"));
  }

  private static Path sourceNamed(GeneratorResult result, String fileName) {
    return result.generatedSources().stream()
        .filter(path -> fileName.equals(Objects.requireNonNull(path.getFileName()).toString()))
        .findFirst()
        .orElseThrow();
  }
}
