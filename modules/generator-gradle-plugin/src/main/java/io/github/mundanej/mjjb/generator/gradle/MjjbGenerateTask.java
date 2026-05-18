package io.github.mundanej.mjjb.generator.gradle;

import io.github.mundanej.mjjb.generator.api.GeneratorDiagnostic;
import io.github.mundanej.mjjb.generator.api.GeneratorProfile;
import io.github.mundanej.mjjb.generator.api.GeneratorRequest;
import io.github.mundanej.mjjb.generator.api.GeneratorResult;
import io.github.mundanej.mjjb.generator.core.CoreGenerator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/** Generates Java sources from explicit JSON Schema inputs. */
@CacheableTask
public class MjjbGenerateTask extends DefaultTask {
  private final ConfigurableFileCollection schemas;
  private final DirectoryProperty outputDirectory;
  private final Property<String> profile;
  private final Property<String> defaultPackage;
  private final MapProperty<String, String> packageMappings;

  @Inject
  public MjjbGenerateTask(ObjectFactory objects) {
    schemas = objects.fileCollection();
    outputDirectory = objects.directoryProperty();
    profile = objects.property(String.class).convention("JSP-DATA-2020-12");
    defaultPackage = objects.property(String.class).convention(GeneratorRequest.DEFAULT_PACKAGE);
    packageMappings = objects.mapProperty(String.class, String.class);
  }

  @TaskAction
  public void generate() {
    Path temporaryOutput = getTemporaryDir().toPath().resolve("generated");
    Path finalOutput = outputDirectory.get().getAsFile().toPath();
    try {
      deleteRecursively(temporaryOutput);
    } catch (IOException exception) {
      throw new GradleException(
          "Unable to prepare temporary generated source directory.", exception);
    }

    GeneratorResult result =
        new CoreGenerator()
            .generate(
                new GeneratorRequest(
                    sortedPaths(schemas),
                    temporaryOutput,
                    parsedProfile(),
                    defaultPackage.get(),
                    sortedMap(packageMappings.get())));
    if (!result.successful()) {
      throw new GradleException(formatDiagnostics(result.diagnostics()));
    }

    try {
      deleteRecursively(finalOutput);
      copyDirectory(temporaryOutput, finalOutput);
    } catch (IOException exception) {
      throw new GradleException("Unable to replace generated source directory.", exception);
    }
  }

  @InputFiles
  @PathSensitive(PathSensitivity.RELATIVE)
  public ConfigurableFileCollection getSchemas() {
    return schemas;
  }

  @OutputDirectory
  public DirectoryProperty getOutputDirectory() {
    return outputDirectory;
  }

  @Input
  public Property<String> getProfile() {
    return profile;
  }

  @Input
  public Property<String> getDefaultPackage() {
    return defaultPackage;
  }

  @Input
  public MapProperty<String, String> getPackageMappings() {
    return packageMappings;
  }

  private GeneratorProfile parsedProfile() {
    String token = profile.get();
    return GeneratorProfile.fromCliToken(token)
        .orElseThrow(
            () ->
                new GradleException(
                    "MJJB-GRADLE-001 | profile | Unsupported generator profile " + token + "."));
  }

  private List<Path> sortedPaths(ConfigurableFileCollection files) {
    return files.getFiles().stream()
        .map(file -> file.toPath().toAbsolutePath().normalize())
        .sorted()
        .toList();
  }

  private Map<String, String> sortedMap(Map<String, String> values) {
    return Map.copyOf(new TreeMap<>(values));
  }

  private String formatDiagnostics(List<GeneratorDiagnostic> diagnostics) {
    return String.join(
        System.lineSeparator(),
        diagnostics.stream().map(GeneratorDiagnostic::toManifestLine).toList());
  }

  private void copyDirectory(Path source, Path target) throws IOException {
    Files.createDirectories(target);
    if (!Files.exists(source)) {
      return;
    }
    try (java.util.stream.Stream<Path> paths = Files.walk(source)) {
      for (Path path : paths.sorted().toList()) {
        Path relativePath = source.relativize(path);
        Path targetPath = target.resolve(relativePath);
        if (Files.isDirectory(path)) {
          Files.createDirectories(targetPath);
        } else {
          Path parent = targetPath.getParent();
          if (parent != null) {
            Files.createDirectories(parent);
          }
          Files.copy(path, targetPath);
        }
      }
    }
  }

  private void deleteRecursively(Path path) throws IOException {
    if (!Files.exists(path)) {
      return;
    }
    try (java.util.stream.Stream<Path> paths = Files.walk(path)) {
      for (Path item : paths.sorted(Comparator.reverseOrder()).toList()) {
        Files.delete(item);
      }
    }
  }
}
