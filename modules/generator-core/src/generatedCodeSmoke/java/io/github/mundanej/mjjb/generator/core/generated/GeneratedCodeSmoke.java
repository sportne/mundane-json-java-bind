package io.github.mundanej.mjjb.generator.core.generated;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Entry point for generated-source smoke verification. */
public final class GeneratedCodeSmoke {
  private GeneratedCodeSmoke() {}

  public static void main(String[] args) throws IOException {
    Path workspace = args.length == 0 ? Path.of("build/generated-code-smoke") : Path.of(args[0]);
    GeneratedSourceVerifier verifier = new GeneratedSourceVerifier();
    for (GeneratedSourceFixture fixture : fixtures()) {
      verifier.verifyFixture(fixture, workspace);
    }
  }

  private static List<GeneratedSourceFixture> fixtures() {
    return List.of(
        new GeneratedSourceFixture("empty-object"),
        new GeneratedSourceFixture("mixed-scalar"),
        new GeneratedSourceFixture("property-order"));
  }
}
