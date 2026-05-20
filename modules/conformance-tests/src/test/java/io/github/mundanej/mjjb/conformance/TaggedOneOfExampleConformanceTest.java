package io.github.mundanej.mjjb.conformance;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjjb.generator.api.GeneratorProfile;
import io.github.mundanej.mjjb.generator.api.GeneratorRequest;
import io.github.mundanej.mjjb.generator.api.GeneratorResult;
import io.github.mundanej.mjjb.generator.core.CoreGenerator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class TaggedOneOfExampleConformanceTest {
  private static final String EXAMPLE_PACKAGE =
      "io.github.mundanej.mjjb.examples.taggedoneof.generated";
  private static final String ROOT_TYPE = "Payment";
  private static final List<String> GENERATED_FILES =
      List.of(
          "Payment.java",
          "PaymentJsonWriter.java",
          "PaymentJsonReader.java",
          "PaymentJsonValidator.java");

  @TempDir Path tempDir;

  @Test
  void checkedInExampleGeneratedSourcesMatchGeneratorOutput() throws IOException {
    Path repositoryRoot = repositoryRoot();
    Path schema =
        repositoryRoot.resolve("examples/tagged-oneof/src/main/schema/tagged-oneof.schema.json");
    Path output = tempDir.resolve("generated");

    GeneratorResult result =
        new CoreGenerator()
            .generate(
                new GeneratorRequest(
                    List.of(schema),
                    output,
                    GeneratorProfile.JSP_DATA_2020_12,
                    EXAMPLE_PACKAGE,
                    ROOT_TYPE,
                    Map.of()));

    assertTrue(result.successful());
    assertEquals(GENERATED_FILES.size(), result.generatedSources().size());
    for (String fileName : GENERATED_FILES) {
      assertArrayEquals(
          Files.readAllBytes(checkedInGeneratedSource(repositoryRoot, fileName)),
          Files.readAllBytes(generatedSource(output, fileName)),
          fileName);
    }
  }

  private static Path checkedInGeneratedSource(Path repositoryRoot, String fileName) {
    return repositoryRoot
        .resolve("examples/tagged-oneof/generated-src/main/java")
        .resolve(EXAMPLE_PACKAGE.replace('.', '/'))
        .resolve(fileName);
  }

  private static Path generatedSource(Path output, String fileName) {
    return output.resolve(EXAMPLE_PACKAGE.replace('.', '/')).resolve(fileName);
  }

  private static Path repositoryRoot() {
    Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
    while (current != null) {
      if (Files.isRegularFile(current.resolve("settings.gradle"))) {
        return current;
      }
      current = current.getParent();
    }
    throw new IllegalStateException(
        "Unable to locate repository root from test working directory.");
  }
}
