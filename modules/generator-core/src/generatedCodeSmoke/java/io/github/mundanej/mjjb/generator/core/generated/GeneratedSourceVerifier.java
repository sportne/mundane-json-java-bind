package io.github.mundanej.mjjb.generator.core.generated;

import io.github.mundanej.mjjb.generator.api.GeneratorResult;
import io.github.mundanej.mjjb.generator.core.CoreGenerator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/** Reusable verifier for generated Java source fixtures. */
public final class GeneratedSourceVerifier {
  private static final List<String> FORBIDDEN_TOKENS =
      List.of(
          "@",
          "java.lang.reflect",
          "MethodHandles",
          "ServiceLoader",
          "Class.forName",
          "Proxy",
          "io.github.mundanej.mjjb.generator",
          "io.github.mundanej.mjjb.schema",
          "io.github.mundanej.mjjb.parser");

  public void verifyFixture(GeneratedSourceFixture fixture, Path workspace) throws IOException {
    Objects.requireNonNull(fixture, "fixture");
    Objects.requireNonNull(workspace, "workspace");
    Path fixtureDirectory = workspace.resolve(fixture.name());
    Path schema = fixtureDirectory.resolve("schema.json");
    Path outputDirectory = fixtureDirectory.resolve("generated");
    Files.createDirectories(fixtureDirectory);
    Files.write(schema, readResource(fixture.schemaResource()));

    GeneratorResult result =
        new CoreGenerator().generate(fixture.request(List.of(schema), outputDirectory));
    if (!result.successful()) {
      throw failure(
          fixture.name(),
          schema,
          "generation failed: "
              + result.diagnostics().stream()
                  .map(diagnostic -> diagnostic.toManifestLine())
                  .toList());
    }
    if (result.generatedSources().size() != 1) {
      throw failure(
          fixture.name(),
          outputDirectory,
          "expected exactly one generated source but found " + result.generatedSources().size());
    }
    Path source = result.generatedSources().getFirst();
    verifyGolden(fixture.name(), source, readResource(fixture.goldenResource()));
    verifyAllowedTokens(fixture.name(), source);
    compileGeneratedSource(fixture.name(), source, fixtureDirectory.resolve("classes"));
  }

  public void verifyGolden(String fixtureName, Path generatedSource, byte[] expected)
      throws IOException {
    Objects.requireNonNull(fixtureName, "fixtureName");
    Objects.requireNonNull(generatedSource, "generatedSource");
    Objects.requireNonNull(expected, "expected");
    byte[] actual = Files.readAllBytes(generatedSource);
    if (!java.util.Arrays.equals(expected, actual)) {
      throw failure(fixtureName, generatedSource, "golden mismatch for generated source");
    }
  }

  public void verifyAllowedTokens(String fixtureName, Path generatedSource) throws IOException {
    Objects.requireNonNull(fixtureName, "fixtureName");
    Objects.requireNonNull(generatedSource, "generatedSource");
    String content = Files.readString(generatedSource);
    for (String token : FORBIDDEN_TOKENS) {
      if (content.contains(token)) {
        throw failure(
            fixtureName,
            generatedSource,
            "forbidden token '" + token + "' found in generated source");
      }
    }
  }

  public void compileGeneratedSource(
      String fixtureName, Path generatedSource, Path classesDirectory) throws IOException {
    Objects.requireNonNull(fixtureName, "fixtureName");
    Objects.requireNonNull(generatedSource, "generatedSource");
    Objects.requireNonNull(classesDirectory, "classesDirectory");
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      throw failure(fixtureName, generatedSource, "system Java compiler is not available");
    }
    Files.createDirectories(classesDirectory);
    ByteArrayOutputStream errors = new ByteArrayOutputStream();
    int result;
    try (PrintStream errorStream = new PrintStream(errors, true, StandardCharsets.UTF_8)) {
      result =
          compiler.run(
              null,
              null,
              errorStream,
              "--release",
              "21",
              "-Xlint:all",
              "-Werror",
              "-classpath",
              classesDirectory.toString(),
              "-d",
              classesDirectory.toString(),
              generatedSource.toString());
    }
    if (result != 0) {
      throw failure(
          fixtureName,
          generatedSource,
          "compilation failed: " + errors.toString(StandardCharsets.UTF_8).trim());
    }
  }

  private static byte[] readResource(String resourceName) throws IOException {
    try (InputStream stream =
        GeneratedSourceVerifier.class.getClassLoader().getResourceAsStream(resourceName)) {
      if (stream == null) {
        throw new GeneratedSourceVerificationException("missing resource " + resourceName);
      }
      return stream.readAllBytes();
    }
  }

  private static GeneratedSourceVerificationException failure(
      String fixtureName, Path source, String reason) {
    return new GeneratedSourceVerificationException(
        "Fixture '" + fixtureName + "' failed for " + source + ": " + reason);
  }
}
