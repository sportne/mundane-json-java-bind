package io.github.mundanej.mjjb.generator.core;

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
        schema, "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"$ref\":\"x\"}");

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schema), tempDir.resolve("out")));

    assertFalse(result.successful());
    assertTrue(result.diagnostics().getFirst().message().contains("$ref"));
  }

  @Test
  void writesInitialGeneratedSourceForSupportedSchema() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(
        schema, "{\"type\":\"object\",\"properties\":{},\"additionalProperties\":false}");

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schema), tempDir.resolve("out")));

    assertTrue(result.successful());
    assertTrue(Files.isRegularFile(result.generatedSources().getFirst()));
  }
}
