package io.github.mundanej.mjjb.generator.core.generated;

import io.github.mundanej.mjjb.generator.api.GeneratorRequest;
import java.util.Map;
import java.util.Objects;

/** Generated-source smoke fixture descriptor. */
public record GeneratedSourceFixture(
    String name, String schemaResource, String goldenResource, String packageName) {
  public GeneratedSourceFixture(String name) {
    this(
        name,
        "fixtures/" + name + "/schema.json",
        "fixtures/" + name + "/GeneratedBindings.java.golden",
        GeneratorRequest.DEFAULT_PACKAGE);
  }

  public GeneratedSourceFixture {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(schemaResource, "schemaResource");
    Objects.requireNonNull(goldenResource, "goldenResource");
    packageName =
        packageName == null || packageName.isBlank()
            ? GeneratorRequest.DEFAULT_PACKAGE
            : packageName;
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
  }

  public GeneratorRequest request(
      java.util.List<java.nio.file.Path> schemaPaths, java.nio.file.Path outputDirectory) {
    return new GeneratorRequest(
        schemaPaths,
        outputDirectory,
        io.github.mundanej.mjjb.generator.api.GeneratorProfile.JSP_DATA_2020_12,
        packageName,
        Map.of());
  }
}
