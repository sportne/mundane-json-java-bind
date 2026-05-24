package io.github.mundanej.mjjb.conformance;

import io.github.mundanej.mjjb.generator.api.GeneratorProfile;
import io.github.mundanej.mjjb.generator.api.GeneratorRequest;
import io.github.mundanej.mjjb.generator.api.GeneratorResult;
import io.github.mundanej.mjjb.generator.core.CoreGenerator;
import io.github.mundanej.mjjb.parser.JsonStreamReader;
import io.github.mundanej.mjjb.runtime.JsonReadException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/** Writes non-threshold performance evidence for parser, generator, and generated binding paths. */
public final class PerformanceEvidence {
  private static final MeasurementPlan RUNTIME_PLAN = new MeasurementPlan(5, 25, 50);
  private static final MeasurementPlan GENERATOR_PLAN = new MeasurementPlan(2, 10, 2);
  private static final MeasurementPlan COMPILE_PLAN = new MeasurementPlan(1, 5, 1);
  private static final MeasurementPlan QUICK_PLAN = new MeasurementPlan(0, 1, 1);
  private static final String GENERATED_PACKAGE = "io.github.mundanej.mjjb.performance.generated";
  private static final String ROOT_TYPE = "PerformanceBinding";
  private static final AtomicLong SINK = new AtomicLong();
  private static final AtomicLong WORKSPACE_COUNTER = new AtomicLong();

  private PerformanceEvidence() {}

  public static void main(String[] args)
      throws IOException, ClassNotFoundException, NoSuchMethodException, JsonReadException {
    Path reportDirectory =
        args.length > 0 ? Path.of(args[0]) : Path.of("build/reports/performance").toAbsolutePath();
    Path workspace =
        args.length > 1 ? Path.of(args[1]) : Path.of("build/performance-evidence").toAbsolutePath();
    List<Path> classpath = args.length > 2 ? classpathFrom(args[2]) : currentProcessClasspath();
    boolean quick = args.length > 3 && "--quick".equals(args[3]);
    MeasurementPlan runtimePlan = quick ? QUICK_PLAN : RUNTIME_PLAN;
    MeasurementPlan generatorPlan = quick ? QUICK_PLAN : GENERATOR_PLAN;
    MeasurementPlan compilePlan = quick ? QUICK_PLAN : COMPILE_PLAN;

    Files.createDirectories(reportDirectory);
    Files.createDirectories(workspace);

    String parserSmall = parserSmallJson();
    String parserMedium = parserMediumJson(160);
    String bindingSmall = generatedBindingJson(2);
    String bindingMedium = generatedBindingJson(160);
    String bindingRich = generatedRichBindingJson(160, 25);
    String schemaSource = bindingSchema();

    ArrayList<Measurement> measurements = new ArrayList<>();
    measurements.add(
        measure(
            "parser-small-read",
            parserSmall.length(),
            runtimePlan,
            iterations -> parseSmallDocument(parserSmall, iterations)));
    measurements.add(
        measure(
            "parser-medium-read",
            parserMedium.length(),
            runtimePlan,
            iterations -> parseMediumDocument(parserMedium, iterations)));
    measurements.add(
        measure(
            "generator-rich-generate",
            schemaSource.length(),
            generatorPlan,
            iterations ->
                generateBindingSources(
                    workspace.resolve("generation-measure"), schemaSource, iterations)));

    try (GeneratedBindingHarness generated =
        GeneratedBindingHarness.compile(
            workspace, classpath, schemaSource, bindingSmall, bindingMedium, bindingRich)) {
      ArtifactSummary artifactSummary = generated.artifactSummary();
      measurements.add(
          measure(
              "generated-rich-compile",
              artifactSummary.generatedSourceBytes(),
              compilePlan,
              iterations ->
                  compileGeneratedSources(
                      workspace.resolve("compile-measure"),
                      generated.generatedSources(),
                      classpath,
                      iterations)));
      measurements.add(
          measure(
              "generated-small-read-validate-write",
              bindingSmall.length(),
              runtimePlan,
              generated.operation("readValidateWriteSmall")));
      measurements.add(
          measure(
              "generated-medium-read-validate-write",
              bindingMedium.length(),
              runtimePlan,
              generated.operation("readValidateWriteMedium")));
      measurements.add(
          measure(
              "generated-rich-read-validate-write",
              bindingRich.length(),
              runtimePlan,
              generated.operation("readValidateWriteRich")));
      Path report = reportDirectory.resolve("performance-evidence.md");
      Files.writeString(report, report(measurements, artifactSummary), StandardCharsets.UTF_8);
      System.out.println("Performance evidence written to " + report.toAbsolutePath());
    }
  }

  private static Measurement measure(
      String name, long fixtureBytes, MeasurementPlan plan, MeasuredOperation operation)
      throws IOException, JsonReadException {
    for (int batch = 0; batch < plan.warmupBatches(); batch++) {
      SINK.addAndGet(operation.run(plan.batchIterations()));
    }
    long memoryBefore = usedMemory();
    long[] elapsedNanos = new long[plan.measuredBatches()];
    long checksum = 0L;
    for (int batch = 0; batch < plan.measuredBatches(); batch++) {
      long start = System.nanoTime();
      checksum += operation.run(plan.batchIterations());
      elapsedNanos[batch] = System.nanoTime() - start;
    }
    long memoryAfter = usedMemory();
    SINK.addAndGet(checksum);
    Arrays.sort(elapsedNanos);
    return new Measurement(
        name,
        fixtureBytes,
        plan.warmupBatches(),
        plan.measuredBatches(),
        plan.batchIterations(),
        elapsedNanos[0],
        elapsedNanos[elapsedNanos.length / 2],
        elapsedNanos[elapsedNanos.length - 1],
        memoryAfter - memoryBefore,
        checksum);
  }

  private static long parseSmallDocument(String json, int iterations) throws JsonReadException {
    long checksum = 0L;
    for (int iteration = 0; iteration < iterations; iteration++) {
      JsonStreamReader reader = new JsonStreamReader(json);
      reader.beginObject();
      checksum += readSmallObject(reader);
      reader.endObject();
    }
    return checksum;
  }

  private static long readSmallObject(JsonStreamReader reader) throws JsonReadException {
    long checksum = 0L;
    checksum += reader.nextName().length();
    checksum += reader.nextString().length();
    checksum += reader.nextName().length();
    checksum += Long.parseLong(reader.nextNumberLiteral());
    checksum += reader.nextName().length();
    reader.beginArray();
    while (reader.hasNext()) {
      checksum += reader.nextString().length();
    }
    reader.endArray();
    checksum += reader.nextName().length();
    checksum += reader.nextBoolean() ? 1L : 0L;
    return checksum;
  }

  private static long parseMediumDocument(String json, int iterations) throws JsonReadException {
    long checksum = 0L;
    for (int iteration = 0; iteration < iterations; iteration++) {
      JsonStreamReader reader = new JsonStreamReader(json);
      reader.beginObject();
      checksum += reader.nextName().length();
      reader.beginArray();
      while (reader.hasNext()) {
        reader.beginObject();
        checksum += readSmallObject(reader);
        reader.endObject();
      }
      reader.endArray();
      reader.endObject();
    }
    return checksum;
  }

  private static String parserSmallJson() {
    return "{\"id\":\"id-1\",\"count\":7,\"tags\":[\"red\",\"blue\"],\"active\":true}";
  }

  private static String parserMediumJson(int records) {
    StringBuilder builder = new StringBuilder("{\"records\":[");
    for (int index = 0; index < records; index++) {
      if (index > 0) {
        builder.append(',');
      }
      builder
          .append("{\"id\":\"id-")
          .append(index)
          .append("\",\"count\":")
          .append(index)
          .append(",\"tags\":[\"red\",\"blue\",\"green\"],\"active\":")
          .append(index % 2 == 0)
          .append('}');
    }
    builder.append("]}");
    return builder.toString();
  }

  private static String generatedBindingJson(int itemCount) {
    StringBuilder builder = new StringBuilder();
    builder.append("{\"id\":\"binding-1\",\"count\":").append(itemCount).append(',');
    builder.append("\"displayName\":\"Performance\",\"tags\":[");
    for (int index = 0; index < itemCount; index++) {
      if (index > 0) {
        builder.append(',');
      }
      builder.append("\"tag-").append(index).append('"');
    }
    builder.append("],\"scores\":[");
    for (int index = 0; index < itemCount; index++) {
      if (index > 0) {
        builder.append(',');
      }
      builder.append(index).append(".25");
    }
    builder.append("],\"active\":true}");
    return builder.toString();
  }

  private static String generatedRichBindingJson(int itemCount, int mapEntries) {
    StringBuilder builder = new StringBuilder();
    builder.append("{\"id\":\"binding-rich\",\"count\":").append(itemCount).append(',');
    builder.append("\"displayName\":\"Performance Rich\",");
    builder.append("\"tags\":[");
    for (int index = 0; index < itemCount; index++) {
      if (index > 0) {
        builder.append(',');
      }
      builder.append("\"tag-").append(index).append('"');
    }
    builder.append("],\"scores\":[");
    for (int index = 0; index < itemCount; index++) {
      if (index > 0) {
        builder.append(',');
      }
      builder.append(index).append(".25");
    }
    builder.append("],\"active\":true,");
    builder.append("\"profile\":{\"level\":7,\"label\":\"primary\"}");
    for (int index = 0; index < mapEntries; index++) {
      builder.append(",\"x-flag-").append(index).append("\":").append(index % 2 == 0);
    }
    for (int index = 0; index < mapEntries; index++) {
      builder.append(",\"attr").append(index).append("\":\"value-").append(index).append('"');
    }
    builder.append('}');
    return builder.toString();
  }

  private static String report(List<Measurement> measurements, ArtifactSummary artifactSummary) {
    String lineSeparator = System.lineSeparator();
    StringBuilder builder = new StringBuilder();
    builder.append("# Performance Evidence").append(lineSeparator).append(lineSeparator);
    builder.append("Generated at: `").append(Instant.now()).append("`").append(lineSeparator);
    builder
        .append("Java runtime: `")
        .append(System.getProperty("java.runtime.version"))
        .append("`")
        .append(lineSeparator);
    builder
        .append("Java VM: `")
        .append(System.getProperty("java.vm.name"))
        .append("`")
        .append(lineSeparator);
    builder
        .append("Operating system: `")
        .append(System.getProperty("os.name"))
        .append(" ")
        .append(System.getProperty("os.version"))
        .append("`")
        .append(lineSeparator);
    builder
        .append("Available processors: `")
        .append(Runtime.getRuntime().availableProcessors())
        .append("`")
        .append(lineSeparator)
        .append(lineSeparator);
    builder
        .append("| Case | Fixture bytes | Warmup batches | Measured batches | Batch iterations ")
        .append("| Min ms | Median ms | Max ms | Rough memory delta bytes | Checksum |")
        .append(lineSeparator);
    builder.append("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|").append(lineSeparator);
    for (Measurement measurement : measurements) {
      builder.append(measurement.toMarkdownRow()).append(lineSeparator);
    }
    builder.append(lineSeparator).append("## Generated Binding Artifact Summary");
    builder.append(lineSeparator).append(lineSeparator);
    builder
        .append("| Schema bytes | Generated source files | Generated source bytes |")
        .append(lineSeparator);
    builder.append("|---:|---:|---:|").append(lineSeparator);
    builder
        .append("| ")
        .append(artifactSummary.schemaBytes())
        .append(" | ")
        .append(artifactSummary.generatedSourceCount())
        .append(" | ")
        .append(artifactSummary.generatedSourceBytes())
        .append(" |")
        .append(lineSeparator);
    builder.append(lineSeparator).append("Checksum sink: `").append(SINK.get()).append("`");
    builder.append(lineSeparator).append(lineSeparator);
    builder
        .append("Timing values are diagnostic evidence only. They are not pass/fail thresholds.")
        .append(lineSeparator);
    builder
        .append(
            "Memory deltas are rough heap observations around measured batches, not allocation ")
        .append("profiles.")
        .append(lineSeparator);
    return builder.toString();
  }

  private static long usedMemory() {
    Runtime runtime = Runtime.getRuntime();
    return runtime.totalMemory() - runtime.freeMemory();
  }

  private static List<Path> classpathFrom(String classpath) {
    if (classpath.isBlank()) {
      return List.of();
    }
    return Arrays.stream(classpath.split(java.io.File.pathSeparator))
        .filter(entry -> !entry.isBlank())
        .map(Path::of)
        .filter(Files::exists)
        .toList();
  }

  private static List<Path> currentProcessClasspath() {
    return classpathFrom(System.getProperty("java.class.path", ""));
  }

  @FunctionalInterface
  private interface MeasuredOperation {
    long run(int iterations) throws IOException, JsonReadException;
  }

  private record MeasurementPlan(int warmupBatches, int measuredBatches, int batchIterations) {}

  private record Measurement(
      String name,
      long fixtureBytes,
      int warmupBatches,
      int measuredBatches,
      int batchIterations,
      long minNanos,
      long medianNanos,
      long maxNanos,
      long memoryDeltaBytes,
      long checksum) {
    private String toMarkdownRow() {
      return "| "
          + name
          + " | "
          + fixtureBytes
          + " | "
          + warmupBatches
          + " | "
          + measuredBatches
          + " | "
          + batchIterations
          + " | "
          + millis(minNanos)
          + " | "
          + millis(medianNanos)
          + " | "
          + millis(maxNanos)
          + " | "
          + memoryDeltaBytes
          + " | "
          + checksum
          + " |";
    }
  }

  private static String millis(long nanos) {
    return String.format(Locale.ROOT, "%.3f", nanos / 1_000_000.0D);
  }

  private static final class GeneratedBindingHarness implements AutoCloseable {
    private final URLClassLoader classLoader;
    private final Class<?> harnessClass;
    private final List<Path> generatedSources;
    private final ArtifactSummary artifactSummary;

    private GeneratedBindingHarness(
        URLClassLoader classLoader,
        Class<?> harnessClass,
        List<Path> generatedSources,
        ArtifactSummary artifactSummary) {
      this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
      this.harnessClass = Objects.requireNonNull(harnessClass, "harnessClass");
      this.generatedSources =
          List.copyOf(Objects.requireNonNull(generatedSources, "generatedSources"));
      this.artifactSummary = Objects.requireNonNull(artifactSummary, "artifactSummary");
    }

    private static GeneratedBindingHarness compile(
        Path workspace,
        List<Path> classpath,
        String schemaSource,
        String smallJson,
        String mediumJson,
        String richJson)
        throws IOException, ClassNotFoundException {
      Path schema = workspace.resolve("schema.json");
      Path generatedDirectory = workspace.resolve("generated");
      Path classesDirectory = workspace.resolve("classes");
      Files.createDirectories(workspace);
      Files.writeString(schema, schemaSource, StandardCharsets.UTF_8);
      GeneratorResult result =
          new CoreGenerator()
              .generate(
                  new GeneratorRequest(
                      List.of(schema),
                      generatedDirectory,
                      GeneratorProfile.JSP_DATA_2020_12,
                      GENERATED_PACKAGE,
                      ROOT_TYPE,
                      Map.of()));
      if (!result.successful()) {
        throw new IOException(
            "generation failed: "
                + result.diagnostics().stream()
                    .map(diagnostic -> diagnostic.toManifestLine())
                    .toList());
      }
      Path harnessSource =
          generatedDirectory
              .resolve(GENERATED_PACKAGE.replace('.', '/'))
              .resolve("GeneratedBindingEvidence.java");
      Files.writeString(
          harnessSource,
          generatedHarnessSource(smallJson, mediumJson, richJson),
          StandardCharsets.UTF_8);
      ArrayList<Path> sources = new ArrayList<>(result.generatedSources());
      sources.add(harnessSource);
      compileJava(sources, classesDirectory, classpath);

      ArrayList<URL> urls = new ArrayList<>();
      urls.add(classesDirectory.toUri().toURL());
      for (Path path : classpath) {
        urls.add(path.toUri().toURL());
      }
      URLClassLoader classLoader =
          new URLClassLoader(urls.toArray(URL[]::new), PerformanceEvidence.class.getClassLoader());
      return new GeneratedBindingHarness(
          classLoader,
          classLoader.loadClass(GENERATED_PACKAGE + ".GeneratedBindingEvidence"),
          result.generatedSources(),
          new ArtifactSummary(
              schemaSource.length(),
              result.generatedSources().size(),
              sourceBytes(result.generatedSources())));
    }

    private List<Path> generatedSources() {
      return generatedSources;
    }

    private ArtifactSummary artifactSummary() {
      return artifactSummary;
    }

    private MeasuredOperation operation(String methodName) throws NoSuchMethodException {
      Method method = harnessClass.getMethod(methodName, int.class);
      return iterations -> invoke(method, iterations);
    }

    private static long invoke(Method method, int iterations) {
      try {
        return ((Number) method.invoke(null, iterations)).longValue();
      } catch (IllegalAccessException exception) {
        throw new PerformanceEvidenceException(exception);
      } catch (InvocationTargetException exception) {
        Throwable cause = exception.getCause();
        if (cause instanceof Error error) {
          throw error;
        }
        throw new PerformanceEvidenceException(cause);
      }
    }

    @Override
    public void close() throws IOException {
      classLoader.close();
    }
  }

  private record ArtifactSummary(
      int schemaBytes, int generatedSourceCount, long generatedSourceBytes) {}

  private static long generateBindingSources(Path workspace, String schemaSource, int iterations)
      throws IOException {
    Files.createDirectories(workspace);
    Path schema = workspace.resolve("schema.json");
    Files.writeString(schema, schemaSource, StandardCharsets.UTF_8);
    long checksum = 0L;
    for (int iteration = 0; iteration < iterations; iteration++) {
      Path outputDirectory =
          workspace.resolve("generated-" + WORKSPACE_COUNTER.incrementAndGet() + "-" + iteration);
      GeneratorResult result =
          new CoreGenerator()
              .generate(
                  new GeneratorRequest(
                      List.of(schema),
                      outputDirectory,
                      GeneratorProfile.JSP_DATA_2020_12,
                      GENERATED_PACKAGE,
                      ROOT_TYPE,
                      Map.of()));
      if (!result.successful()) {
        throw new IOException(
            "generation failed: "
                + result.diagnostics().stream()
                    .map(diagnostic -> diagnostic.toManifestLine())
                    .toList());
      }
      checksum += result.generatedSources().size();
      checksum += sourceBytes(result.generatedSources());
    }
    return checksum;
  }

  private static long compileGeneratedSources(
      Path workspace, List<Path> sources, List<Path> classpath, int iterations) throws IOException {
    Files.createDirectories(workspace);
    long checksum = 0L;
    for (int iteration = 0; iteration < iterations; iteration++) {
      Path classesDirectory =
          workspace.resolve("classes-" + WORKSPACE_COUNTER.incrementAndGet() + "-" + iteration);
      compileJava(sources, classesDirectory, classpath);
      checksum += sourceBytes(sources);
    }
    return checksum;
  }

  private static long sourceBytes(List<Path> sources) throws IOException {
    long size = 0L;
    for (Path source : sources) {
      size += Files.size(source);
    }
    return size;
  }

  private static void compileJava(List<Path> sources, Path classesDirectory, List<Path> classpath)
      throws IOException {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      throw new IOException("system Java compiler is not available");
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
    sources.stream().map(Path::toString).forEach(arguments::add);
    int result;
    try (PrintStream errorStream = new PrintStream(errors, true, StandardCharsets.UTF_8)) {
      result = compiler.run(null, null, errorStream, arguments.toArray(String[]::new));
    }
    if (result != 0) {
      throw new IOException(
          "generated performance harness compilation failed: "
              + errors.toString(StandardCharsets.UTF_8).trim());
    }
  }

  private static String joinClasspath(List<Path> classpath) {
    return classpath.stream()
        .map(Path::toString)
        .collect(Collectors.joining(java.io.File.pathSeparator));
  }

  private static String bindingSchema() {
    return """
        {
          "type": "object",
          "minProperties": 3,
          "properties": {
            "id": {"type": "string", "minLength": 1},
            "count": {"type": "integer", "minimum": 0, "maximum": 10000, "multipleOf": 1},
            "displayName": {"type": "string", "maxLength": 128},
            "tags": {
              "type": "array",
              "items": {"type": "string", "minLength": 1},
              "minItems": 1,
              "maxItems": 256,
              "uniqueItems": true
            },
            "scores": {
              "type": "array",
              "items": {"type": "number", "minimum": 0, "maximum": 10000, "multipleOf": 0.25},
              "maxItems": 256,
              "uniqueItems": true
            },
            "profile": {
              "type": "object",
              "properties": {
                "level": {"type": "integer", "minimum": 0},
                "label": {"type": "string", "pattern": "^[a-z]+$"}
              },
              "required": ["level"],
              "additionalProperties": false
            },
            "active": {"type": "boolean"}
          },
          "patternProperties": {
            "^x-": {"type": "boolean"}
          },
          "required": ["id", "count", "tags"],
          "additionalProperties": {"type": "string"}
        }
        """;
  }

  private static String generatedHarnessSource(
      String smallJson, String mediumJson, String richJson) {
    return """
        package __PACKAGE_NAME__;

        import io.github.mundanej.mjjb.parser.JsonStreamReader;
        import io.github.mundanej.mjjb.parser.JsonStringWriter;
        import io.github.mundanej.mjjb.runtime.ValidationResult;

        public final class GeneratedBindingEvidence {
          private static final String SMALL_JSON = __SMALL_JSON__;
          private static final String MEDIUM_JSON = __MEDIUM_JSON__;
          private static final String RICH_JSON = __RICH_JSON__;

          private GeneratedBindingEvidence() {}

          public static long readValidateWriteSmall(int iterations) throws Exception {
            return readValidateWrite(SMALL_JSON, iterations);
          }

          public static long readValidateWriteMedium(int iterations) throws Exception {
            return readValidateWrite(MEDIUM_JSON, iterations);
          }

          public static long readValidateWriteRich(int iterations) throws Exception {
            return readValidateWrite(RICH_JSON, iterations);
          }

          private static long readValidateWrite(String json, int iterations) throws Exception {
            long checksum = 0L;
            for (int index = 0; index < iterations; index++) {
              PerformanceBinding value =
                  PerformanceBindingJsonReader.read(new JsonStreamReader(json));
              ValidationResult result = PerformanceBindingJsonValidator.validate(value);
              if (!result.isValid()) {
                throw new AssertionError("expected valid generated binding: " + result.errors());
              }
              JsonStringWriter writer = new JsonStringWriter();
              PerformanceBindingJsonWriter.write(writer, value);
              checksum += writer.json().length();
              checksum += value.id().length();
              checksum += value.count();
              checksum += value.tags().size();
              checksum += value.scores().map(java.util.List::size).orElse(0);
            }
            return checksum;
          }
        }
        """
        .replace("__PACKAGE_NAME__", GENERATED_PACKAGE)
        .replace("__SMALL_JSON__", javaStringLiteral(smallJson))
        .replace("__MEDIUM_JSON__", javaStringLiteral(mediumJson))
        .replace("__RICH_JSON__", javaStringLiteral(richJson));
  }

  private static String javaStringLiteral(String value) {
    StringBuilder builder = new StringBuilder();
    builder.append('"');
    for (int index = 0; index < value.length(); index++) {
      char current = value.charAt(index);
      switch (current) {
        case '"' -> builder.append("\\\"");
        case '\\' -> builder.append("\\\\");
        case '\b' -> builder.append("\\b");
        case '\f' -> builder.append("\\f");
        case '\n' -> builder.append("\\n");
        case '\r' -> builder.append("\\r");
        case '\t' -> builder.append("\\t");
        default -> {
          if (current < 0x20) {
            builder.append(String.format("\\u%04x", (int) current));
          } else {
            builder.append(current);
          }
        }
      }
    }
    builder.append('"');
    return builder.toString();
  }

  private static final class PerformanceEvidenceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private PerformanceEvidenceException(Throwable cause) {
      super(cause);
    }
  }
}
