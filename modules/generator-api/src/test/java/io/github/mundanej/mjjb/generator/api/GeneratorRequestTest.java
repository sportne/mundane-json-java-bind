package io.github.mundanej.mjjb.generator.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class GeneratorRequestTest {
  @Test
  void staticFactoryUsesPublicDefaults() {
    List<Path> schemaPaths = List.of(Path.of("schema.json"));
    Path outputDirectory = Path.of("generated");

    GeneratorRequest request = GeneratorRequest.of(schemaPaths, outputDirectory);

    assertEquals(schemaPaths, request.schemaPaths());
    assertEquals(outputDirectory, request.outputDirectory());
    assertEquals(GeneratorProfile.JSP_DATA_2020_12, request.profile());
    assertEquals(GeneratorRequest.DEFAULT_PACKAGE, request.defaultPackage());
    assertEquals(GeneratorRequest.DEFAULT_ROOT_TYPE_NAME, request.rootTypeName());
    assertEquals(Map.of(), request.packageMappings());
    assertFalse(request.generateSchemaMetadataHelpers());
  }

  @Test
  void canonicalConstructorKeepsCustomProfilePackageRootMetadataAndMappings() {
    Map<String, String> packageMappings =
        Map.of("https://schemas.example.test/admin", "com.example.admin");

    GeneratorRequest request =
        new GeneratorRequest(
            List.of(Path.of("admin.schema.json")),
            Path.of("src/generated/java"),
            GeneratorProfile.JSP_DATA_2020_12,
            "com.example.generated",
            "AdminBindings",
            packageMappings,
            true);

    assertEquals(List.of(Path.of("admin.schema.json")), request.schemaPaths());
    assertEquals(Path.of("src/generated/java"), request.outputDirectory());
    assertEquals(GeneratorProfile.JSP_DATA_2020_12, request.profile());
    assertEquals("com.example.generated", request.defaultPackage());
    assertEquals("AdminBindings", request.rootTypeName());
    assertEquals(packageMappings, request.packageMappings());
    assertTrue(request.generateSchemaMetadataHelpers());
  }

  @Test
  void requestDefensivelyCopiesInputsAndAccessorsAreImmutable() {
    List<Path> schemaPaths = new ArrayList<>();
    schemaPaths.add(Path.of("first.schema.json"));
    Map<String, String> packageMappings = new LinkedHashMap<>();
    packageMappings.put("https://schemas.example.test/first", "com.example.first");

    GeneratorRequest request =
        new GeneratorRequest(
            schemaPaths,
            Path.of("out"),
            GeneratorProfile.JSP_DATA_2020_12,
            "com.example",
            "ExampleRoot",
            packageMappings,
            false);

    schemaPaths.add(Path.of("second.schema.json"));
    packageMappings.put("https://schemas.example.test/second", "com.example.second");

    assertEquals(List.of(Path.of("first.schema.json")), request.schemaPaths());
    assertEquals(
        Map.of("https://schemas.example.test/first", "com.example.first"),
        request.packageMappings());
    assertThrows(
        UnsupportedOperationException.class,
        () -> request.schemaPaths().add(Path.of("third.schema.json")));
    assertThrows(
        UnsupportedOperationException.class,
        () ->
            request
                .packageMappings()
                .put("https://schemas.example.test/third", "com.example.third"));
  }

  @Test
  void packageMappingsAreReturnedInDeterministicKeyOrder() {
    Map<String, String> packageMappings = new LinkedHashMap<>();
    packageMappings.put("https://schemas.example.test/zeta", "com.example.zeta");
    packageMappings.put("https://schemas.example.test/alpha", "com.example.alpha");
    packageMappings.put("https://schemas.example.test/middle", "com.example.middle");

    GeneratorRequest request =
        new GeneratorRequest(
            List.of(Path.of("schema.json")),
            Path.of("out"),
            GeneratorProfile.JSP_DATA_2020_12,
            "com.example",
            "ExampleRoot",
            packageMappings,
            false);

    assertEquals(
        Map.of(
            "https://schemas.example.test/alpha",
            "com.example.alpha",
            "https://schemas.example.test/middle",
            "com.example.middle",
            "https://schemas.example.test/zeta",
            "com.example.zeta"),
        request.packageMappings());
  }

  @Test
  void defaultsNullProfilePackageAndRootTypeName() {
    List<Path> schemaPaths = List.of(Path.of("schema.json"));
    Path outputDirectory = Path.of("out");
    Map<String, String> packageMappings = Map.of();

    GeneratorRequest request =
        new GeneratorRequest(
            schemaPaths, outputDirectory, null, null, null, packageMappings, false);

    assertEquals(GeneratorProfile.JSP_DATA_2020_12, request.profile());
    assertEquals(GeneratorRequest.DEFAULT_PACKAGE, request.defaultPackage());
    assertEquals(GeneratorRequest.DEFAULT_ROOT_TYPE_NAME, request.rootTypeName());
  }

  @Test
  void defaultsBlankPackageAndRootTypeName() {
    List<Path> schemaPaths = List.of(Path.of("schema.json"));
    Path outputDirectory = Path.of("out");
    GeneratorProfile profile = GeneratorProfile.JSP_DATA_2020_12;
    Map<String, String> packageMappings = Map.of();

    GeneratorRequest request =
        new GeneratorRequest(
            schemaPaths, outputDirectory, profile, " ", "\t", packageMappings, false);

    assertEquals(GeneratorRequest.DEFAULT_PACKAGE, request.defaultPackage());
    assertEquals(GeneratorRequest.DEFAULT_ROOT_TYPE_NAME, request.rootTypeName());
  }

  @Test
  void rejectsNullRequiredCollections() {
    List<Path> schemaPaths = List.of(Path.of("schema.json"));
    Path outputDirectory = Path.of("out");
    GeneratorProfile profile = GeneratorProfile.JSP_DATA_2020_12;
    String defaultPackage = "com.example";
    String rootTypeName = "ExampleRoot";
    Map<String, String> packageMappings = Map.of();

    assertThrows(
        NullPointerException.class,
        () ->
            new GeneratorRequest(
                null,
                outputDirectory,
                profile,
                defaultPackage,
                rootTypeName,
                packageMappings,
                false));
    assertThrows(
        NullPointerException.class,
        () ->
            new GeneratorRequest(
                schemaPaths, outputDirectory, profile, defaultPackage, rootTypeName, null, false));
  }
}
