package io.github.mundanej.mjjb.generator.core.generated;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GeneratedSourceVerifierTest {
  @TempDir Path tempDir;
  private final GeneratedSourceVerifier verifier = new GeneratedSourceVerifier();

  @Test
  void reportsGoldenMismatchWithFixtureAndSourcePath() throws IOException {
    Path source = tempDir.resolve("GeneratedBindings.java");
    Files.writeString(source, "public record GeneratedBindings() {}\n");

    GeneratedSourceVerificationException exception =
        assertThrows(
            GeneratedSourceVerificationException.class,
            () ->
                verifier.verifyGolden(
                    "broken-golden",
                    source,
                    "public final class GeneratedBindings {}\n".getBytes(StandardCharsets.UTF_8)));

    assertTrue(exception.getMessage().contains("broken-golden"));
    assertTrue(exception.getMessage().contains(source.toString()));
    assertTrue(exception.getMessage().contains("golden mismatch"));
  }

  @Test
  void reportsForbiddenTokenWithFixtureAndSourcePath() throws IOException {
    Path source = tempDir.resolve("GeneratedBindings.java");
    Files.writeString(source, "@Deprecated\npublic record GeneratedBindings() {}\n");

    GeneratedSourceVerificationException exception =
        assertThrows(
            GeneratedSourceVerificationException.class,
            () -> verifier.verifyAllowedTokens("forbidden-token", source));

    assertTrue(exception.getMessage().contains("forbidden-token"));
    assertTrue(exception.getMessage().contains(source.toString()));
    assertTrue(exception.getMessage().contains("forbidden token '@'"));
  }

  @Test
  void reportsCompilerFailureWithFixtureAndSourcePath() throws IOException {
    Path source = tempDir.resolve("GeneratedBindings.java");
    Files.writeString(source, "public record GeneratedBindings( {}\n");

    GeneratedSourceVerificationException exception =
        assertThrows(
            GeneratedSourceVerificationException.class,
            () ->
                verifier.compileGeneratedSource(
                    "compile-failure", source, tempDir.resolve("classes")));

    assertTrue(exception.getMessage().contains("compile-failure"));
    assertTrue(exception.getMessage().contains(source.toString()));
    assertTrue(exception.getMessage().contains("compilation failed"));
  }
}
