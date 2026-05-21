package io.github.mundanej.mjjb.generator.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class GeneratorCliNativeSmokeTest {
  @Test
  void runsHelpGenerationAndDiagnostics() throws IOException {
    Path workspace = Files.createTempDirectory("mjjb-cli-native-smoke");
    Path schema = workspace.resolve("schema.json");
    Path output = workspace.resolve("generated");
    Path unsupportedSchema = workspace.resolve("unsupported.json");
    Files.writeString(
        schema,
        """
        {
          "type": "object",
          "properties": {"id": {"type": "string"}},
          "required": ["id"],
          "additionalProperties": false
        }
        """);
    Files.writeString(
        unsupportedSchema, "{\"type\":\"object\",\"properties\":{\"id\":{\"$ref\":\"x\"}}}");

    assertEquals(0, MjjbCli.run(new String[] {"--help"}));
    assertEquals(
        0,
        MjjbCli.run(
            new String[] {
              "generate",
              "--schema",
              schema.toString(),
              "--out",
              output.toString(),
              "--package",
              "com.example.nativeimage",
              "--root-type",
              "NativeSmoke"
            }));
    assertTrue(Files.isRegularFile(output.resolve("com/example/nativeimage/NativeSmoke.java")));
    assertTrue(
        Files.isRegularFile(output.resolve("com/example/nativeimage/NativeSmokeJsonReader.java")));
    assertEquals(
        1,
        MjjbCli.run(
            new String[] {
              "generate",
              "--schema",
              unsupportedSchema.toString(),
              "--out",
              output.resolve("bad").toString()
            }));
  }
}
