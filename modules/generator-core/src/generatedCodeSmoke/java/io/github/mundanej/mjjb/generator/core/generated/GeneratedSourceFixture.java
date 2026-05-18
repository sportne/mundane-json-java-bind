package io.github.mundanej.mjjb.generator.core.generated;

import io.github.mundanej.mjjb.generator.api.GeneratorRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Generated-source smoke fixture descriptor. */
public record GeneratedSourceFixture(
    String name,
    String schemaResource,
    List<String> goldenResources,
    Optional<String> behaviorProbeResource,
    String packageName) {
  public GeneratedSourceFixture(String name) {
    this(
        name,
        "fixtures/" + name + "/schema.json",
        List.of(
            "fixtures/" + name + "/GeneratedBindings.java.golden",
            "fixtures/" + name + "/GeneratedBindingsJsonWriter.java.golden",
            "fixtures/" + name + "/GeneratedBindingsJsonReader.java.golden"),
        Optional.of("fixtures/" + name + "/BindingBehaviorProbe.java"),
        GeneratorRequest.DEFAULT_PACKAGE);
  }

  public GeneratedSourceFixture {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(schemaResource, "schemaResource");
    goldenResources = List.copyOf(Objects.requireNonNull(goldenResources, "goldenResources"));
    Objects.requireNonNull(behaviorProbeResource, "behaviorProbeResource");
    packageName =
        packageName == null || packageName.isBlank()
            ? GeneratorRequest.DEFAULT_PACKAGE
            : packageName;
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    if (goldenResources.isEmpty()) {
      throw new IllegalArgumentException("goldenResources must not be empty");
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
