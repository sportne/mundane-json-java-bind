package io.github.mundanej.mjjb.generator.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class MjjbCliTest {
  @TempDir Path tempDir;

  @Test
  void helpExitsSuccessfully() {
    assertEquals(0, MjjbCli.run(new String[] {"--help"}));
  }

  @Test
  void missingSchemaIsArgumentError() {
    assertEquals(2, MjjbCli.run(new String[] {"generate", "--out", "build/generated"}));
  }

  @Test
  void invalidProfileIsArgumentError() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(schema, "{\"type\":\"object\"}");

    assertEquals(
        2,
        MjjbCli.run(
            new String[] {
              "generate",
              "--schema",
              schema.toString(),
              "--profile",
              "NOPE",
              "--out",
              tempDir.toString()
            }));
  }

  @Test
  void generateCommandWritesOutput() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Path output = tempDir.resolve("out");
    Files.writeString(
        schema, "{\"type\":\"object\",\"properties\":{},\"additionalProperties\":false}");

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
              "com.example.generated"
            }));
    assertTrue(Files.isRegularFile(output.resolve("com/example/generated/GeneratedBindings.java")));
  }
}
