package io.github.mundanej.mjjb.conformance;

import io.github.mundanej.mjjb.generator.api.GeneratorProfile;
import io.github.mundanej.mjjb.generator.api.GeneratorRequest;
import io.github.mundanej.mjjb.generator.api.GeneratorResult;
import io.github.mundanej.mjjb.generator.core.CoreGenerator;
import io.github.mundanej.mjjb.parser.JsonStreamReader;
import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.JsonWriteException;
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

/** Writes non-threshold performance evidence for parser and generated binding paths. */
public final class PerformanceEvidence {
  private static final int WARMUP_BATCHES = 5;
  private static final int MEASURED_BATCHES = 25;
  private static final int BATCH_ITERATIONS = 50;
  private static final String GENERATED_PACKAGE = "io.github.mundanej.mjjb.performance.generated";
  private static final String ROOT_TYPE = "PerformanceBinding";
  private static final AtomicLong SINK = new AtomicLong();

  private PerformanceEvidence() {}

  public static void main(String[] args)
      throws IOException,
          ClassNotFoundException,
          NoSuchMethodException,
          JsonReadException,
          JsonWriteException {
    Path reportDirectory =
        args.length > 0 ? Path.of(args[0]) : Path.of("build/reports/performance").toAbsolutePath();
    Path workspace =
        args.length > 1 ? Path.of(args[1]) : Path.of("build/performance-evidence").toAbsolutePath();
    List<Path> classpath = args.length > 2 ? classpathFrom(args[2]) : currentProcessClasspath();

    Files.createDirectories(reportDirectory);
    Files.createDirectories(workspace);

    String parserSmall = parserSmallJson();
    String parserMedium = parserMediumJson(160);
    String bindingSmall = generatedBindingJson(2);
    String bindingMedium = generatedBindingJson(160);

    ArrayList<Measurement> measurements = new ArrayList<>();
    measurements.add(
        measure(
            "parser-small-read",
            parserSmall.length(),
            iterations -> parseSmallDocument(parserSmall, iterations)));
    measurements.add(
        measure(
            "parser-medium-read",
            parserMedium.length(),
            iterations -> parseMediumDocument(parserMedium, iterations)));

    try (GeneratedBindingHarness generated =
        GeneratedBindingHarness.compile(workspace, classpath, bindingSmall, bindingMedium)) {
      measurements.add(
          measure(
              "generated-small-read-validate-write",
              bindingSmall.length(),
              generated.operation("readValidateWriteSmall")));
      measurements.add(
          measure(
              "generated-medium-read-validate-write",
              bindingMedium.length(),
              generated.operation("readValidateWriteMedium")));
    }

    Path report = reportDirectory.resolve("performance-evidence.md");
    Files.writeString(report, report(measurements), StandardCharsets.UTF_8);
    System.out.println("Performance evidence written to " + report.toAbsolutePath());
  }

  private static Measurement measure(String name, int fixtureBytes, MeasuredOperation operation)
      throws JsonReadException, JsonWriteException {
    for (int batch = 0; batch < WARMUP_BATCHES; batch++) {
      SINK.addAndGet(operation.run(BATCH_ITERATIONS));
    }
    long memoryBefore = usedMemory();
    long[] elapsedNanos = new long[MEASURED_BATCHES];
    long checksum = 0L;
    for (int batch = 0; batch < MEASURED_BATCHES; batch++) {
      long start = System.nanoTime();
      checksum += operation.run(BATCH_ITERATIONS);
      elapsedNanos[batch] = System.nanoTime() - start;
    }
    long memoryAfter = usedMemory();
    SINK.addAndGet(checksum);
    Arrays.sort(elapsedNanos);
    return new Measurement(
        name,
        fixtureBytes,
        WARMUP_BATCHES,
        MEASURED_BATCHES,
        BATCH_ITERATIONS,
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

  private static String report(List<Measurement> measurements) {
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
    long run(int iterations) throws JsonReadException, JsonWriteException;
  }

  private record Measurement(
      String name,
      int fixtureBytes,
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

    private GeneratedBindingHarness(URLClassLoader classLoader, Class<?> harnessClass) {
      this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
      this.harnessClass = Objects.requireNonNull(harnessClass, "harnessClass");
    }

    private static GeneratedBindingHarness compile(
        Path workspace, List<Path> classpath, String smallJson, String mediumJson)
        throws IOException, ClassNotFoundException {
      Path schema = workspace.resolve("schema.json");
      Path generatedDirectory = workspace.resolve("generated");
      Path classesDirectory = workspace.resolve("classes");
      Files.createDirectories(workspace);
      Files.writeString(schema, bindingSchema(), StandardCharsets.UTF_8);
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
          harnessSource, generatedHarnessSource(smallJson, mediumJson), StandardCharsets.UTF_8);
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
          classLoader, classLoader.loadClass(GENERATED_PACKAGE + ".GeneratedBindingEvidence"));
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
          "properties": {
            "id": {"type": "string", "minLength": 1},
            "count": {"type": "integer", "minimum": 0},
            "displayName": {"type": "string"},
            "tags": {
              "type": "array",
              "items": {"type": "string", "minLength": 1},
              "minItems": 1,
              "maxItems": 256
            },
            "scores": {
              "type": "array",
              "items": {"type": "number", "minimum": 0},
              "maxItems": 256
            },
            "active": {"type": "boolean"}
          },
          "required": ["id", "count", "tags"],
          "additionalProperties": false
        }
        """;
  }

  private static String generatedHarnessSource(String smallJson, String mediumJson) {
    return """
        package __PACKAGE_NAME__;

        import io.github.mundanej.mjjb.parser.JsonStreamReader;
        import io.github.mundanej.mjjb.parser.JsonStringWriter;
        import io.github.mundanej.mjjb.runtime.ValidationResult;

        public final class GeneratedBindingEvidence {
          private static final String SMALL_JSON = __SMALL_JSON__;
          private static final String MEDIUM_JSON = __MEDIUM_JSON__;

          private GeneratedBindingEvidence() {}

          public static long readValidateWriteSmall(int iterations) throws Exception {
            return readValidateWrite(SMALL_JSON, iterations);
          }

          public static long readValidateWriteMedium(int iterations) throws Exception {
            return readValidateWrite(MEDIUM_JSON, iterations);
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
        .replace("__MEDIUM_JSON__", javaStringLiteral(mediumJson));
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
