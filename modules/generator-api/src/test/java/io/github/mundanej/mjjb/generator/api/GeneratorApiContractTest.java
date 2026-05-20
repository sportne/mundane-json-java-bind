package io.github.mundanej.mjjb.generator.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class GeneratorApiContractTest {
  @Test
  void requestDefaultsProfileAndPackage() {
    GeneratorRequest request =
        new GeneratorRequest(List.of(Path.of("schema.json")), Path.of("out"), null, "", Map.of());

    assertEquals(GeneratorProfile.JSP_DATA_2020_12, request.profile());
    assertEquals(GeneratorRequest.DEFAULT_PACKAGE, request.defaultPackage());
    assertEquals(GeneratorRequest.DEFAULT_ROOT_TYPE_NAME, request.rootTypeName());
    assertFalse(request.generateSchemaMetadataHelpers());
  }

  @Test
  void requestAcceptsCustomRootTypeName() {
    GeneratorRequest request =
        new GeneratorRequest(
            List.of(Path.of("schema.json")),
            Path.of("out"),
            GeneratorProfile.JSP_DATA_2020_12,
            "com.example",
            "CustomRoot",
            Map.of());

    assertEquals("CustomRoot", request.rootTypeName());
    assertFalse(request.generateSchemaMetadataHelpers());
  }

  @Test
  void requestAcceptsSchemaMetadataHelperOptIn() {
    GeneratorRequest request =
        new GeneratorRequest(
            List.of(Path.of("schema.json")),
            Path.of("out"),
            GeneratorProfile.JSP_DATA_2020_12,
            "com.example",
            "CustomRoot",
            Map.of("b", "two", "a", "one"),
            true);

    assertTrue(request.generateSchemaMetadataHelpers());
    assertEquals(List.of(Path.of("schema.json")), request.schemaPaths());
    assertEquals(Map.of("a", "one", "b", "two"), request.packageMappings());
  }

  @Test
  void profileParsesCliToken() {
    assertTrue(GeneratorProfile.fromCliToken("JSP-DATA-2020-12").isPresent());
  }

  @Test
  void resultReportsSuccessFromDiagnostics() {
    Path generated = Path.of("Generated.java");
    GeneratorResult success = GeneratorResult.success(List.of(generated));
    GeneratorDiagnostic diagnostic =
        new GeneratorDiagnostic("MJJBT-001", "Broken schema.", Path.of("schema.json"), "/type");
    GeneratorResult failure = GeneratorResult.failure(List.of(diagnostic));

    assertTrue(success.successful());
    assertEquals(generated, success.generatedSources().getFirst());
    assertFalse(failure.successful());
    assertEquals("MJJBT-001 | schema.json#/type | Broken schema.", diagnostic.toManifestLine());
  }
}
