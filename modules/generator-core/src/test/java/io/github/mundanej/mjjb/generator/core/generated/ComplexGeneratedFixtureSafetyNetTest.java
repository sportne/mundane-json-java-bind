package io.github.mundanej.mjjb.generator.core.generated;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ComplexGeneratedFixtureSafetyNetTest {
  @TempDir Path tempDir;

  private final GeneratedSourceVerifier verifier = new GeneratedSourceVerifier();

  @Test
  void verifiesComplexFixturesThatProtectGeneratorSimplification() {
    List<GeneratedSourceFixture> fixtures =
        List.of(
            new GeneratedSourceFixture("nested-object"),
            new GeneratedSourceFixture("local-ref"),
            new GeneratedSourceFixture("allof-flattened-object"),
            new GeneratedSourceFixture("additional-properties-map"),
            new GeneratedSourceFixture("pattern-properties-map"),
            new GeneratedSourceFixture("literal-constraints"),
            new GeneratedSourceFixture("object-validation-keywords"),
            new GeneratedSourceFixture("multipleof-uniqueitems"),
            new GeneratedSourceFixture("additional-properties-nullable-array-map"),
            new GeneratedSourceFixture("additional-properties-nested-object-map"),
            new GeneratedSourceFixture("tagged-oneof-additional-properties-map"),
            fixture("metadata-basic", true),
            fixture("metadata-tagged", true),
            new GeneratedSourceFixture("diagnostics-hardening"),
            new GeneratedSourceFixture("property-order"),
            new GeneratedSourceFixture("validation-fail-fast"));

    for (GeneratedSourceFixture fixture : fixtures) {
      assertDoesNotThrow(
          () -> verifier.verifyFixture(fixture, tempDir.resolve(fixture.name())), fixture.name());
    }
  }

  private static GeneratedSourceFixture fixture(
      String name, boolean generateSchemaMetadataHelpers) {
    return new GeneratedSourceFixture(name, generateSchemaMetadataHelpers);
  }
}
