package io.github.mundanej.mjjb.generator.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjjb.generator.api.GeneratorResult;
import io.github.mundanej.mjjb.generator.core.generated.GeneratedSourceVerifier;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

final class GeneratedFixtureAssertions {
  private static final List<String> DEFAULT_SOURCE_FILES =
      List.of(
          "GeneratedBindings.java",
          "GeneratedBindingsJsonWriter.java",
          "GeneratedBindingsJsonReader.java",
          "GeneratedBindingsJsonValidator.java");
  private static final List<String> METADATA_SOURCE_FILES =
      List.of(
          "GeneratedBindings.java",
          "GeneratedBindingsJsonWriter.java",
          "GeneratedBindingsJsonReader.java",
          "GeneratedBindingsJsonValidator.java",
          "GeneratedBindingsJsonSchemaMetadata.java");

  private final GeneratedSourceVerifier verifier = new GeneratedSourceVerifier();

  void verifyDefaultFixture(String fixtureName, GeneratorResult result, Path classesDirectory)
      throws IOException {
    verifyFixture(fixtureName, result, classesDirectory, DEFAULT_SOURCE_FILES);
  }

  void verifyMetadataFixture(String fixtureName, GeneratorResult result, Path classesDirectory)
      throws IOException {
    verifyFixture(fixtureName, result, classesDirectory, METADATA_SOURCE_FILES);
  }

  void verifyFixture(
      String fixtureName,
      GeneratorResult result,
      Path classesDirectory,
      List<String> expectedSourceFiles)
      throws IOException {
    Objects.requireNonNull(fixtureName, "fixtureName");
    Objects.requireNonNull(result, "result");
    Objects.requireNonNull(classesDirectory, "classesDirectory");
    Objects.requireNonNull(expectedSourceFiles, "expectedSourceFiles");

    assertTrue(result.successful());
    assertEquals(expectedSourceFiles.size(), result.generatedSources().size());
    for (String fileName : expectedSourceFiles) {
      Path source = sourceNamed(result, fileName);
      verifier.verifyGolden(fixtureName, source, goldenBytes(fixtureName, fileName));
      verifier.verifyAllowedTokens(fixtureName, source);
    }
    verifier.compileGeneratedSources(fixtureName, result.generatedSources(), classesDirectory);
  }

  static Path sourceNamed(GeneratorResult result, String fileName) {
    return result.generatedSources().stream()
        .filter(source -> fileName.equals(sourceFileName(source)))
        .findFirst()
        .orElseThrow();
  }

  private static String sourceFileName(Path source) {
    Path sourceFileName = source.getFileName();
    return sourceFileName == null ? "" : sourceFileName.toString();
  }

  private static byte[] goldenBytes(String fixtureName, String fileName) throws IOException {
    String resourceName = "/golden/" + fixtureName + "/" + fileName + ".golden";
    try (InputStream stream = GeneratedFixtureAssertions.class.getResourceAsStream(resourceName)) {
      Objects.requireNonNull(stream, "missing test resource " + resourceName);
      return stream.readAllBytes();
    }
  }
}
