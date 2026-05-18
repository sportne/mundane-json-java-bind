package io.github.mundanej.mjjb.generator.api;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Immutable public generator request. */
public record GeneratorRequest(
    List<Path> schemaPaths,
    Path outputDirectory,
    GeneratorProfile profile,
    String defaultPackage,
    Map<String, String> packageMappings) {
  public static final String DEFAULT_PACKAGE = "io.github.mundanej.mjjb.generated";

  public GeneratorRequest {
    Objects.requireNonNull(schemaPaths, "schemaPaths");
    Objects.requireNonNull(packageMappings, "packageMappings");
    schemaPaths = List.copyOf(schemaPaths);
    profile = profile == null ? GeneratorProfile.JSP_DATA_2020_12 : profile;
    defaultPackage =
        defaultPackage == null || defaultPackage.isBlank() ? DEFAULT_PACKAGE : defaultPackage;
    packageMappings = Map.copyOf(new TreeMap<>(packageMappings));
  }

  public static GeneratorRequest of(List<Path> schemaPaths, Path outputDirectory) {
    return new GeneratorRequest(
        schemaPaths, outputDirectory, GeneratorProfile.JSP_DATA_2020_12, DEFAULT_PACKAGE, Map.of());
  }

  @Override
  public List<Path> schemaPaths() {
    return List.copyOf(schemaPaths);
  }

  @Override
  public Map<String, String> packageMappings() {
    return Map.copyOf(packageMappings);
  }
}
