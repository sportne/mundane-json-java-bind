package io.github.mundanej.mjjb.generator.core.generated;

import io.github.mundanej.mjjb.generator.api.GeneratorResult;
import io.github.mundanej.mjjb.generator.core.CoreGenerator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/** Reusable verifier for generated Java source fixtures. */
public final class GeneratedSourceVerifier {
  private static final List<String> FORBIDDEN_TOKENS =
      List.of(
          "@",
          "java.lang.reflect",
          "MethodHandles",
          "ServiceLoader",
          "Class.forName",
          "Proxy",
          "io.github.mundanej.mjjb.generator",
          "io.github.mundanej.mjjb.schema",
          "io.github.mundanej.mjjb.parser");
  private final List<Path> compileClasspath;

  public GeneratedSourceVerifier() {
    this(currentProcessClasspath());
  }

  public GeneratedSourceVerifier(List<Path> compileClasspath) {
    this.compileClasspath =
        List.copyOf(Objects.requireNonNull(compileClasspath, "compileClasspath"));
  }

  public void verifyFixture(GeneratedSourceFixture fixture, Path workspace) throws IOException {
    Objects.requireNonNull(fixture, "fixture");
    Objects.requireNonNull(workspace, "workspace");
    Path fixtureDirectory = workspace.resolve(fixture.name());
    Path schema = fixtureDirectory.resolve("schema.json");
    Path outputDirectory = fixtureDirectory.resolve("generated");
    Files.createDirectories(fixtureDirectory);
    Files.write(schema, readResource(fixture.schemaResource()));

    GeneratorResult result =
        new CoreGenerator().generate(fixture.request(List.of(schema), outputDirectory));
    if (!result.successful()) {
      throw failure(
          fixture.name(),
          schema,
          "generation failed: "
              + result.diagnostics().stream()
                  .map(diagnostic -> diagnostic.toManifestLine())
                  .toList());
    }
    if (result.generatedSources().size() != fixture.goldenResources().size()) {
      throw failure(
          fixture.name(),
          outputDirectory,
          "expected "
              + fixture.goldenResources().size()
              + " generated sources but found "
              + result.generatedSources().size());
    }
    for (int index = 0; index < result.generatedSources().size(); index++) {
      Path source = result.generatedSources().get(index);
      verifyGolden(fixture.name(), source, readResource(fixture.goldenResources().get(index)));
      verifyAllowedTokens(fixture.name(), source);
    }
    Path classesDirectory = fixtureDirectory.resolve("classes");
    compileGeneratedSources(fixture.name(), result.generatedSources(), classesDirectory);
    if (fixture.behaviorProbeResource().isPresent()) {
      verifyBehaviorProbe(
          fixture.name(),
          readResource(fixture.behaviorProbeResource().orElseThrow()),
          classesDirectory);
    }
  }

  public void verifyGolden(String fixtureName, Path generatedSource, byte[] expected)
      throws IOException {
    Objects.requireNonNull(fixtureName, "fixtureName");
    Objects.requireNonNull(generatedSource, "generatedSource");
    Objects.requireNonNull(expected, "expected");
    byte[] actual = Files.readAllBytes(generatedSource);
    if (!java.util.Arrays.equals(expected, actual)) {
      throw failure(fixtureName, generatedSource, "golden mismatch for generated source");
    }
  }

  public void verifyAllowedTokens(String fixtureName, Path generatedSource) throws IOException {
    Objects.requireNonNull(fixtureName, "fixtureName");
    Objects.requireNonNull(generatedSource, "generatedSource");
    String content = Files.readString(generatedSource);
    for (String token : FORBIDDEN_TOKENS) {
      if (content.contains(token)) {
        throw failure(
            fixtureName,
            generatedSource,
            "forbidden token '" + token + "' found in generated source");
      }
    }
  }

  public void compileGeneratedSource(
      String fixtureName, Path generatedSource, Path classesDirectory) throws IOException {
    compileGeneratedSources(fixtureName, List.of(generatedSource), classesDirectory);
  }

  public void compileGeneratedSources(
      String fixtureName, List<Path> generatedSources, Path classesDirectory) throws IOException {
    Objects.requireNonNull(fixtureName, "fixtureName");
    Objects.requireNonNull(generatedSources, "generatedSources");
    Objects.requireNonNull(classesDirectory, "classesDirectory");
    if (generatedSources.isEmpty()) {
      throw failure(fixtureName, classesDirectory, "no generated sources were provided");
    }
    for (Path generatedSource : generatedSources) {
      Objects.requireNonNull(generatedSource, "generatedSource");
    }
    compileJava(
        fixtureName,
        generatedSources.getFirst(),
        generatedSources,
        classesDirectory,
        classpathWith(classesDirectory));
  }

  private void verifyBehaviorProbe(String fixtureName, byte[] probeSource, Path classesDirectory)
      throws IOException {
    Path probe = classesDirectory.resolve("WriterBehaviorProbe.java");
    Files.write(probe, probeSource);
    compileJava(
        fixtureName, probe, List.of(probe), classesDirectory, classpathWith(classesDirectory));
    runBehaviorProbe(fixtureName, probe, classesDirectory);
  }

  private void runBehaviorProbe(String fixtureName, Path probe, Path classesDirectory)
      throws IOException {
    ArrayList<URL> urls = new ArrayList<>();
    urls.add(classesDirectory.toUri().toURL());
    for (Path path : compileClasspath) {
      urls.add(path.toUri().toURL());
    }
    try (URLClassLoader classLoader =
        new URLClassLoader(
            urls.toArray(URL[]::new), GeneratedSourceVerifier.class.getClassLoader())) {
      Object instance =
          classLoader.loadClass("WriterBehaviorProbe").getDeclaredConstructor().newInstance();
      ((GeneratedSourceBehaviorProbe) instance).run();
    } catch (ReflectiveOperationException | ClassCastException exception) {
      throw failure(fixtureName, probe, "behavior probe failed: " + exception.getMessage());
    } catch (AssertionError error) {
      throw failure(fixtureName, probe, "behavior probe failed: " + error.getMessage());
    } catch (Exception exception) {
      throw failure(fixtureName, probe, "behavior probe failed: " + exception.getMessage());
    }
  }

  private void compileJava(
      String fixtureName,
      Path failureSource,
      List<Path> sources,
      Path classesDirectory,
      List<Path> classpath)
      throws IOException {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      throw failure(fixtureName, failureSource, "system Java compiler is not available");
    }
    Files.createDirectories(classesDirectory);
    ByteArrayOutputStream errors = new ByteArrayOutputStream();
    ArrayList<String> arguments = new ArrayList<>();
    arguments.add("--release");
    arguments.add("21");
    arguments.add("-Xlint:all");
    arguments.add("-Werror");
    arguments.add("-classpath");
    arguments.add(joinClasspath(classpath));
    arguments.add("-d");
    arguments.add(classesDirectory.toString());
    for (Path source : sources) {
      arguments.add(source.toString());
    }
    int result;
    try (PrintStream errorStream = new PrintStream(errors, true, StandardCharsets.UTF_8)) {
      result = compiler.run(null, null, errorStream, arguments.toArray(String[]::new));
    }
    if (result != 0) {
      throw failure(
          fixtureName,
          failureSource,
          "compilation failed: " + errors.toString(StandardCharsets.UTF_8).trim());
    }
  }

  private List<Path> classpathWith(Path classesDirectory) {
    ArrayList<Path> paths = new ArrayList<>();
    paths.add(classesDirectory);
    paths.addAll(compileClasspath);
    return paths;
  }

  private static String joinClasspath(List<Path> classpath) {
    return classpath.stream()
        .map(Path::toString)
        .collect(java.util.stream.Collectors.joining(java.io.File.pathSeparator));
  }

  private static List<Path> currentProcessClasspath() {
    String classpath = System.getProperty("java.class.path", "");
    if (classpath.isBlank()) {
      return List.of();
    }
    return Arrays.stream(classpath.split(java.io.File.pathSeparator))
        .filter(entry -> !entry.isBlank())
        .map(Path::of)
        .filter(Files::exists)
        .toList();
  }

  private static byte[] readResource(String resourceName) throws IOException {
    try (InputStream stream =
        GeneratedSourceVerifier.class.getClassLoader().getResourceAsStream(resourceName)) {
      if (stream == null) {
        throw new GeneratedSourceVerificationException("missing resource " + resourceName);
      }
      return stream.readAllBytes();
    }
  }

  private static GeneratedSourceVerificationException failure(
      String fixtureName, Path source, String reason) {
    return new GeneratedSourceVerificationException(
        "Fixture '" + fixtureName + "' failed for " + source + ": " + reason);
  }
}
