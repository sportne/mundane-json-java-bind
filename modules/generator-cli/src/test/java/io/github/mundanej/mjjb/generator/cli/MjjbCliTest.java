package io.github.mundanej.mjjb.generator.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class MjjbCliTest {
  @TempDir Path tempDir;

  @Test
  void helpExitsSuccessfully() {
    RunResult result = runCli("--help");

    assertEquals(0, result.exitCode());
    assertTrue(result.stdout().contains("Usage:"));
  }

  @Test
  void noArgumentsPrintHelpAndExitSuccessfully() {
    RunResult result = runCli();

    assertEquals(0, result.exitCode());
    assertTrue(result.stdout().contains("Usage:"));
    assertEquals("", result.stderr());
  }

  @Test
  void shortHelpExitsSuccessfully() {
    RunResult result = runCli("-h");

    assertEquals(0, result.exitCode());
    assertTrue(result.stdout().contains("Usage:"));
    assertEquals("", result.stderr());
  }

  @Test
  void unsupportedCommandTokenIsArgumentError() {
    RunResult result = runCli("compile");

    assertEquals(2, result.exitCode());
    assertTrue(result.stderr().contains("MJJB-CLI-001"));
    assertTrue(result.stderr().contains("Unsupported argument compile."));
  }

  @Test
  void unknownOptionIsArgumentError() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(schema, "{\"type\":\"object\"}");

    RunResult result =
        runCli(
            "generate",
            "--schema",
            schema.toString(),
            "--out",
            tempDir.resolve("out").toString(),
            "--bogus");

    assertEquals(2, result.exitCode());
    assertTrue(result.stderr().contains("MJJB-CLI-001"));
    assertTrue(result.stderr().contains("Unsupported argument --bogus."));
  }

  @Test
  void missingOptionValueIsArgumentError() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(schema, "{\"type\":\"object\"}");

    RunResult result = runCli("generate", "--schema", schema.toString(), "--out");

    assertEquals(2, result.exitCode());
    assertTrue(result.stderr().contains("MJJB-CLI-003"));
    assertTrue(result.stderr().contains("Missing value for --out."));
  }

  @Test
  void missingSchemaIsArgumentError() {
    RunResult result = runCli("generate", "--out", "build/generated");

    assertEquals(2, result.exitCode());
    assertTrue(result.stderr().contains("MJJB-CLI-002"));
  }

  @Test
  void invalidProfileIsArgumentError() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(schema, "{\"type\":\"object\"}");

    RunResult result =
        runCli(
            "generate",
            "--schema",
            schema.toString(),
            "--profile",
            "NOPE",
            "--out",
            tempDir.toString());

    assertEquals(2, result.exitCode());
    assertTrue(result.stderr().contains("MJJB-CLI-004"));
  }

  @Test
  void generateCommandWritesCustomRootTypeOutputAndCompiledSources() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Path output = tempDir.resolve("out");
    Files.writeString(
        schema, "{\"type\":\"object\",\"properties\":{},\"additionalProperties\":false}");

    RunResult result =
        runCli(
            "generate",
            "--schema",
            schema.toString(),
            "--out",
            output.toString(),
            "--package",
            "com.example.generated",
            "--root-type",
            "AccountBinding");

    Path modelSource = output.resolve("com/example/generated/AccountBinding.java");
    Path writerSource = output.resolve("com/example/generated/AccountBindingJsonWriter.java");
    Path readerSource = output.resolve("com/example/generated/AccountBindingJsonReader.java");
    Path validatorSource = output.resolve("com/example/generated/AccountBindingJsonValidator.java");
    List<Path> sources = List.of(modelSource, writerSource, readerSource, validatorSource);
    assertEquals(0, result.exitCode());
    for (Path source : sources) {
      assertTrue(Files.isRegularFile(source), source.toString());
      assertTrue(result.stdout().contains("Generated " + source));
    }
    compileGeneratedSources(output.resolve("classes"), sources);
  }

  @Test
  void repeatedSchemaArgumentsValidateEverySchemaBeforeGenerating() throws IOException {
    Path firstSchema = tempDir.resolve("first.json");
    Path secondSchema = tempDir.resolve("second.json");
    Files.writeString(firstSchema, "{\"type\":\"object\"}");

    RunResult result =
        runCli(
            "generate",
            "--schema",
            firstSchema.toString(),
            "--schema",
            secondSchema.toString(),
            "--out",
            tempDir.resolve("out").toString());

    assertEquals(1, result.exitCode());
    assertTrue(result.stderr().contains("MJJBG-GEN-003"));
    assertTrue(result.stderr().contains(secondSchema.toString()));
  }

  @Test
  void missingSchemaPathIsGenerationError() {
    RunResult result =
        runCli(
            "generate",
            "--schema",
            tempDir.resolve("missing.json").toString(),
            "--out",
            tempDir.resolve("out").toString());

    assertEquals(1, result.exitCode());
    assertTrue(result.stderr().contains("MJJBG-GEN-003"));
  }

  @Test
  void unsupportedKeywordIsGenerationError() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(schema, "{\"type\":\"object\",\"properties\":{\"id\":{\"$ref\":\"x\"}}}");

    RunResult result =
        runCli(
            "generate", "--schema", schema.toString(), "--out", tempDir.resolve("out").toString());

    assertEquals(1, result.exitCode());
    assertTrue(result.stderr().contains("MJJBG-SCHEMA-UNSUPPORTED-KEYWORD"));
  }

  @Test
  void invalidPackageIsArgumentError() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(schema, "{\"type\":\"object\"}");

    RunResult result =
        runCli(
            "generate",
            "--schema",
            schema.toString(),
            "--out",
            tempDir.resolve("out").toString(),
            "--package",
            "1.bad");

    assertEquals(2, result.exitCode());
    assertTrue(result.stderr().contains("MJJB-CLI-005"));
  }

  @Test
  void packageWithKeywordPartIsArgumentError() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(schema, "{\"type\":\"object\"}");

    RunResult result =
        runCli(
            "generate",
            "--schema",
            schema.toString(),
            "--out",
            tempDir.resolve("out").toString(),
            "--package",
            "com.class");

    assertEquals(2, result.exitCode());
    assertTrue(result.stderr().contains("MJJB-CLI-005"));
  }

  @Test
  void packageWithEmptyPartIsArgumentError() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(schema, "{\"type\":\"object\"}");

    RunResult result =
        runCli(
            "generate",
            "--schema",
            schema.toString(),
            "--out",
            tempDir.resolve("out").toString(),
            "--package",
            "com..example");

    assertEquals(2, result.exitCode());
    assertTrue(result.stderr().contains("MJJB-CLI-005"));
  }

  @Test
  void invalidRootTypeIsArgumentError() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(schema, "{\"type\":\"object\"}");

    RunResult result =
        runCli(
            "generate",
            "--schema",
            schema.toString(),
            "--out",
            tempDir.resolve("out").toString(),
            "--root-type",
            "1Bad");

    assertEquals(2, result.exitCode());
    assertTrue(result.stderr().contains("MJJB-CLI-006"));
  }

  @Test
  void rootTypeKeywordIsArgumentError() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(schema, "{\"type\":\"object\"}");

    RunResult result =
        runCli(
            "generate",
            "--schema",
            schema.toString(),
            "--out",
            tempDir.resolve("out").toString(),
            "--root-type",
            "class");

    assertEquals(2, result.exitCode());
    assertTrue(result.stderr().contains("MJJB-CLI-006"));
  }

  @Test
  void rootTypeWithPunctuationIsArgumentError() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(schema, "{\"type\":\"object\"}");

    RunResult result =
        runCli(
            "generate",
            "--schema",
            schema.toString(),
            "--out",
            tempDir.resolve("out").toString(),
            "--root-type",
            "Bad-Type");

    assertEquals(2, result.exitCode());
    assertTrue(result.stderr().contains("MJJB-CLI-006"));
  }

  private static RunResult runCli(String... args) {
    PrintStream previousOut = System.out;
    PrintStream previousErr = System.err;
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();
    int exitCode;
    try (PrintStream out = new PrintStream(stdout, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(stderr, true, StandardCharsets.UTF_8)) {
      System.setOut(out);
      System.setErr(err);
      exitCode = MjjbCli.run(args);
    } finally {
      System.setOut(previousOut);
      System.setErr(previousErr);
    }
    return new RunResult(
        exitCode, stdout.toString(StandardCharsets.UTF_8), stderr.toString(StandardCharsets.UTF_8));
  }

  private static void compileGeneratedSources(Path classesDirectory, List<Path> sources)
      throws IOException {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    assertNotNull(compiler, "system Java compiler is required");
    Files.createDirectories(classesDirectory);
    ByteArrayOutputStream errors = new ByteArrayOutputStream();
    ArrayList<String> arguments = new ArrayList<>();
    arguments.add("--release");
    arguments.add("21");
    arguments.add("-Xlint:all");
    arguments.add("-Werror");
    arguments.add("-classpath");
    arguments.add(System.getProperty("java.class.path", ""));
    arguments.add("-d");
    arguments.add(classesDirectory.toString());
    sources.stream().map(Path::toString).forEach(arguments::add);
    int result;
    try (PrintStream errorStream = new PrintStream(errors, true, StandardCharsets.UTF_8)) {
      result = compiler.run(null, null, errorStream, arguments.toArray(String[]::new));
    }
    assertEquals(0, result, errors.toString(StandardCharsets.UTF_8));
  }

  private record RunResult(int exitCode, String stdout, String stderr) {}
}
