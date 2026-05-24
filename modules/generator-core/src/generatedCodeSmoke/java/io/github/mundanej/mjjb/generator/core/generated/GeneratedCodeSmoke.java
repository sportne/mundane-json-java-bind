package io.github.mundanej.mjjb.generator.core.generated;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/** Entry point for generated-source smoke verification. */
public final class GeneratedCodeSmoke {
  private GeneratedCodeSmoke() {}

  public static void main(String[] args) throws IOException {
    Path workspace = args.length == 0 ? Path.of("build/generated-code-smoke") : Path.of(args[0]);
    GeneratedSourceVerifier verifier =
        new GeneratedSourceVerifier(compileClasspathFromSystemProperty());
    for (GeneratedSourceFixture fixture : fixtures()) {
      verifier.verifyFixture(fixture, workspace);
    }
  }

  private static List<Path> compileClasspathFromSystemProperty() {
    String classpath = System.getProperty("mjjb.generatedSourceCompileClasspath", "");
    if (classpath.isBlank()) {
      return List.of();
    }
    return Arrays.stream(classpath.split(java.io.File.pathSeparator))
        .filter(entry -> !entry.isBlank())
        .map(Path::of)
        .filter(Files::exists)
        .toList();
  }

  private static List<GeneratedSourceFixture> fixtures() {
    return List.of(
        new GeneratedSourceFixture("empty-object"),
        new GeneratedSourceFixture("mixed-scalar"),
        new GeneratedSourceFixture("array-scalar"),
        new GeneratedSourceFixture("facet-constraints"),
        new GeneratedSourceFixture("literal-constraints"),
        new GeneratedSourceFixture("nullable-fields"),
        new GeneratedSourceFixture("nested-object"),
        new GeneratedSourceFixture("tagged-oneof"),
        new GeneratedSourceFixture("metadata-basic", true),
        new GeneratedSourceFixture("metadata-tagged", true),
        new GeneratedSourceFixture("diagnostics-hardening"),
        new GeneratedSourceFixture("property-order"),
        new GeneratedSourceFixture("validation-fail-fast"));
  }
}
