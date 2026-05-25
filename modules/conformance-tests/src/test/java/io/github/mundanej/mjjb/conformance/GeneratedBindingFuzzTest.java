package io.github.mundanej.mjjb.conformance;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.github.mundanej.mjjb.generator.api.GeneratorRequest;
import io.github.mundanej.mjjb.generator.api.GeneratorResult;
import io.github.mundanej.mjjb.generator.core.CoreGenerator;
import io.github.mundanej.mjjb.parser.JsonStreamReader;
import io.github.mundanej.mjjb.parser.JsonStringWriter;
import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.JsonReader;
import io.github.mundanej.mjjb.runtime.JsonWriter;
import io.github.mundanej.mjjb.runtime.ValidationError;
import io.github.mundanej.mjjb.runtime.ValidationResult;
import io.github.mundanej.mjjb.testkit.DeterministicFuzzConfig;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SplittableRandom;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("fuzz")
final class GeneratedBindingFuzzTest {
  private static final DeterministicFuzzConfig FUZZ =
      DeterministicFuzzConfig.fromSystemProperties(0xC0FFEE42L, 24, 5, 24);
  private static final String GENERATED_PACKAGE = "io.github.mundanej.mjjb.generated";
  private static final String GENERATED_ROOT = "GeneratedBindings";

  @TempDir Path tempDir;

  @Test
  void generatedBindingsReadValidateWriteAndRejectFuzzedInstances()
      throws IOException, ReflectiveOperationException {
    for (Scenario scenario : scenarios()) {
      CompiledBinding binding = compileScenario(scenario);
      try {
        for (int iteration = 0; iteration < FUZZ.iterations(); iteration++) {
          SplittableRandom random =
              new SplittableRandom(
                  FUZZ.seed() ^ scenario.name().hashCode() ^ iteration * 0x9E3779B97F4A7C15L);
          String validJson = scenario.validJson(random);
          String validLabel = FUZZ.caseLabel(scenario.name() + "-valid", iteration, validJson);
          assertDoesNotThrow(() -> binding.assertValidRoundTrip(validJson, validLabel), validLabel);

          String invalidJson = scenario.invalidJson(random, iteration);
          String invalidLabel =
              FUZZ.caseLabel(scenario.name() + "-invalid", iteration, invalidJson);
          assertDoesNotThrow(() -> binding.assertInvalid(invalidJson, invalidLabel), invalidLabel);
        }
      } finally {
        binding.close();
      }
    }
  }

  private CompiledBinding compileScenario(Scenario scenario)
      throws IOException, ReflectiveOperationException {
    Path workspace = tempDir.resolve(scenario.name());
    Path schemaPath = workspace.resolve("schema.json");
    Path generated = workspace.resolve("generated");
    Path classes = workspace.resolve("classes");
    Files.createDirectories(workspace);
    Files.writeString(schemaPath, scenario.schema(), StandardCharsets.UTF_8);

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schemaPath), generated));
    if (!result.successful()) {
      fail("Generation failed for " + scenario.name() + ": " + result.diagnostics());
    }
    compileGeneratedSources(scenario.name(), result.generatedSources(), classes);
    return CompiledBinding.load(classes);
  }

  private static void compileGeneratedSources(
      String scenario, List<Path> generatedSources, Path classes) throws IOException {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      throw new IllegalStateException("System Java compiler is not available for " + scenario);
    }
    Files.createDirectories(classes);
    ArrayList<String> arguments = new ArrayList<>();
    arguments.add("--release");
    arguments.add("21");
    arguments.add("-Xlint:all");
    arguments.add("-Werror");
    arguments.add("-classpath");
    arguments.add(joinClasspath(classpathWith(classes)));
    arguments.add("-d");
    arguments.add(classes.toString());
    for (Path generatedSource : generatedSources) {
      arguments.add(generatedSource.toString());
    }
    int exitCode = compiler.run(null, null, null, arguments.toArray(String[]::new));
    if (exitCode != 0) {
      fail("Generated source compilation failed for " + scenario + " with exit code " + exitCode);
    }
  }

  private static List<Path> classpathWith(Path classes) {
    ArrayList<Path> paths = new ArrayList<>();
    paths.add(classes);
    Pattern pathSeparator = Pattern.compile(Pattern.quote(System.getProperty("path.separator")));
    for (String entry :
        pathSeparator.splitAsStream(System.getProperty("java.class.path", "")).toList()) {
      if (!entry.isBlank()) {
        paths.add(Path.of(entry));
      }
    }
    return paths;
  }

  private static String joinClasspath(List<Path> paths) {
    return String.join(
        System.getProperty("path.separator"), paths.stream().map(Path::toString).toList());
  }

  private static List<Scenario> scenarios() {
    return List.of(richObjectScenario(), localRefAllOfScenario(), taggedOneOfScenario());
  }

  private static Scenario richObjectScenario() {
    return new Scenario(
        "rich-object",
        """
        {
          "type": "object",
          "minProperties": 6,
          "maxProperties": 12,
          "propertyNames": {"pattern": "^[A-Za-z0-9_-]+$"},
          "properties": {
            "id": {"type": "string", "minLength": 2, "maxLength": 12, "pattern": "^[a-z][a-z0-9_-]*$"},
            "count": {"type": "integer", "minimum": 0, "maximum": 100},
            "score": {"type": "number", "minimum": 0, "exclusiveMaximum": 100, "multipleOf": 0.5},
            "active": {"type": "boolean"},
            "note": {"type": ["null", "string"], "maxLength": 16},
            "tags": {
              "type": "array",
              "items": {"type": "string", "minLength": 1, "maxLength": 6},
              "minItems": 1,
              "maxItems": 4,
              "uniqueItems": true
            },
            "profile": {
              "type": "object",
              "properties": {
                "city": {"type": "string", "minLength": 1},
                "zip": {"type": "string", "pattern": "^[0-9]{5}$"}
              },
              "required": ["city"],
              "additionalProperties": false
            }
          },
          "required": ["id", "count", "score", "active", "tags", "profile"],
          "dependentRequired": {"active": ["score"]},
          "patternProperties": {
            "^x_": {"type": "integer", "minimum": 0, "maximum": 20}
          },
          "additionalProperties": {"type": "string", "enum": ["ok", "warn"]}
        }
        """,
        GeneratedBindingFuzzTest::richValidJson,
        GeneratedBindingFuzzTest::richInvalidJson);
  }

  private static Scenario localRefAllOfScenario() {
    return new Scenario(
        "local-ref-allof",
        """
        {
          "type": "object",
          "properties": {
            "id": {"$ref": "#/$defs/id"},
            "name": {"type": "string", "const": "fixed"},
            "level": {"type": "integer", "minimum": 0, "maximum": 3}
          },
          "additionalProperties": false,
          "allOf": [
            {"type": "object", "required": ["id"]},
            {"type": "object", "required": ["name"]}
          ],
          "$defs": {
            "id": {"type": "string", "pattern": "^id-[0-9]+$"}
          }
        }
        """,
        random ->
            "{\"id\":\"id-"
                + random.nextInt(1_000)
                + "\",\"name\":\"fixed\",\"level\":"
                + random.nextInt(4)
                + "}",
        (random, iteration) ->
            switch (iteration % 4) {
              case 0 -> "{\"name\":\"fixed\",\"level\":1}";
              case 1 -> "{\"id\":\"bad\",\"name\":\"fixed\",\"level\":1}";
              case 2 -> "{\"id\":\"id-1\",\"name\":\"other\",\"level\":1}";
              default -> "{\"id\":\"id-1\",\"name\":\"fixed\",\"extra\":true}";
            });
  }

  private static Scenario taggedOneOfScenario() {
    return new Scenario(
        "tagged-oneof",
        """
        {
          "oneOf": [
            {
              "type": "object",
              "properties": {
                "kind": {"type": "string", "const": "card"},
                "last4": {"type": "string", "pattern": "^[0-9]{4}$"},
                "amount": {"type": "number", "minimum": 0, "multipleOf": 0.5}
              },
              "required": ["kind", "last4", "amount"],
              "additionalProperties": false
            },
            {
              "type": "object",
              "properties": {
                "kind": {"type": "string", "const": "bank"},
                "iban": {"type": "string", "minLength": 8},
                "verified": {"type": "boolean"}
              },
              "required": ["kind", "iban"],
              "additionalProperties": false
            }
          ]
        }
        """,
        random -> {
          if (random.nextBoolean()) {
            return "{\"kind\":\"card\",\"last4\":\""
                + fourDigits(random)
                + "\",\"amount\":"
                + halfStep(random, 0, 200)
                + "}";
          }
          return "{\"kind\":\"bank\",\"iban\":\"IBAN"
              + random.nextInt(10_000_000)
              + "\",\"verified\":true}";
        },
        (random, iteration) ->
            switch (iteration % 5) {
              case 0 -> "{\"kind\":\"cash\",\"amount\":1.0}";
              case 1 -> "{\"kind\":\"card\",\"last4\":\"12\",\"amount\":1.0}";
              case 2 -> "{\"kind\":\"card\",\"last4\":\"1234\",\"amount\":-1.0}";
              case 3 -> "{\"kind\":\"bank\",\"iban\":\"short\"}";
              default -> "{\"kind\":\"bank\",\"iban\":\"IBAN12345678\",\"extra\":false}";
            });
  }

  private static String richValidJson(SplittableRandom random) {
    String note =
        switch (random.nextInt(3)) {
          case 0 -> "";
          case 1 -> ",\"note\":null";
          default -> ",\"note\":\"" + asciiWord(random, 1, 12) + "\"";
        };
    String zip = random.nextBoolean() ? ",\"zip\":\"" + fiveDigits(random) + "\"" : "";
    String patternMap =
        random.nextBoolean() ? ",\"x_" + random.nextInt(100) + "\":" + random.nextInt(21) : "";
    String additionalMap =
        random.nextBoolean()
            ? ",\"extra_"
                + random.nextInt(100)
                + "\":\""
                + (random.nextBoolean() ? "ok" : "warn")
                + "\""
            : "";
    return "{\"id\":\""
        + asciiId(random)
        + "\",\"count\":"
        + random.nextInt(101)
        + ",\"score\":"
        + halfStep(random, 0, 199)
        + ",\"active\":"
        + random.nextBoolean()
        + note
        + ",\"tags\":"
        + uniqueTags(random)
        + ",\"profile\":{\"city\":\""
        + asciiWord(random, 1, 10)
        + "\""
        + zip
        + "}"
        + patternMap
        + additionalMap
        + "}";
  }

  private static String richInvalidJson(SplittableRandom random, int iteration) {
    return switch (iteration % 8) {
      case 0 ->
          "{\"count\":1,\"score\":1.0,\"active\":true,\"tags\":[\"a\"],\"profile\":{\"city\":\"x\"}}";
      case 1 ->
          "{\"id\":\"ab\",\"id\":\"cd\",\"count\":1,\"score\":1.0,\"active\":true,\"tags\":[\"a\"],\"profile\":{\"city\":\"x\"}}";
      case 2 ->
          "{\"id\":\"ab\",\"count\":1,\"score\":100.0,\"active\":true,\"tags\":[\"a\"],\"profile\":{\"city\":\"x\"}}";
      case 3 ->
          "{\"id\":\"ab\",\"count\":1,\"score\":1.0,\"active\":true,\"tags\":[\"a\"],\"profile\":{\"city\":\"x\"},\"bad.key\":\"ok\"}";
      case 4 ->
          "{\"id\":\"ab\",\"count\":1,\"score\":1.0,\"active\":true,\"tags\":[\"a\"],\"profile\":{\"city\":\"x\"},\"x_bad\":-1}";
      case 5 ->
          "{\"id\":\"ab\",\"count\":1,\"score\":1.0,\"active\":true,\"tags\":[\"a\"],\"profile\":{\"city\":\"x\"},\"extra_1\":\"bad\"}";
      case 6 ->
          "{\"id\":\"ab\",\"count\":1,\"score\":1.0,\"active\":true,\"tags\":[\"dup\",\"dup\"],\"profile\":{\"city\":\"x\"}}";
      default ->
          "{\"id\":\"ab\",\"count\":1,\"score\":1.0,\"active\":true,\"tags\":[\"a\"],\"profile\":{\"city\":\"x\",\"zip\":\"bad\"}}";
    };
  }

  private static String asciiId(SplittableRandom random) {
    return "a" + asciiWord(random, 1, 8).toLowerCase(Locale.ROOT);
  }

  private static String asciiWord(SplittableRandom random, int minLength, int maxLength) {
    int length = random.nextInt(minLength, maxLength + 1);
    StringBuilder builder = new StringBuilder();
    for (int index = 0; index < length; index++) {
      builder.append((char) ('a' + random.nextInt(26)));
    }
    return builder.toString();
  }

  private static String uniqueTags(SplittableRandom random) {
    int size = random.nextInt(1, 5);
    return "["
        + String.join(
            ",",
            Stream.iterate(0, index -> index + 1)
                .limit(size)
                .map(index -> "\"t" + index + "_" + random.nextInt(100) + "\"")
                .toList())
        + "]";
  }

  private static String halfStep(SplittableRandom random, int minHalfSteps, int maxHalfSteps) {
    int halfSteps = random.nextInt(minHalfSteps, maxHalfSteps + 1);
    return halfSteps / 2 + (halfSteps % 2 == 0 ? ".0" : ".5");
  }

  private static String fourDigits(SplittableRandom random) {
    return "%04d".formatted(random.nextInt(10_000));
  }

  private static String fiveDigits(SplittableRandom random) {
    return "%05d".formatted(random.nextInt(100_000));
  }

  private record Scenario(
      String name, String schema, ValidJson validJson, InvalidJson invalidJson) {
    String validJson(SplittableRandom random) {
      return validJson.create(random);
    }

    String invalidJson(SplittableRandom random, int iteration) {
      return invalidJson.create(random, iteration);
    }
  }

  @FunctionalInterface
  private interface ValidJson {
    String create(SplittableRandom random);
  }

  @FunctionalInterface
  private interface InvalidJson {
    String create(SplittableRandom random, int iteration);
  }

  private static final class CompiledBinding implements AutoCloseable {
    private final URLClassLoader classLoader;
    private final Method read;
    private final Method validate;
    private final Method write;

    private CompiledBinding(
        URLClassLoader classLoader, Method read, Method validate, Method write) {
      this.classLoader = classLoader;
      this.read = read;
      this.validate = validate;
      this.write = write;
    }

    static CompiledBinding load(Path classes) throws IOException, ReflectiveOperationException {
      ArrayList<URL> urls = new ArrayList<>();
      for (Path path : classpathWith(classes)) {
        urls.add(path.toUri().toURL());
      }
      URLClassLoader classLoader =
          new URLClassLoader(
              urls.toArray(URL[]::new), GeneratedBindingFuzzTest.class.getClassLoader());
      Class<?> modelType = classLoader.loadClass(GENERATED_PACKAGE + "." + GENERATED_ROOT);
      Class<?> readerType =
          classLoader.loadClass(GENERATED_PACKAGE + "." + GENERATED_ROOT + "JsonReader");
      Class<?> validatorType =
          classLoader.loadClass(GENERATED_PACKAGE + "." + GENERATED_ROOT + "JsonValidator");
      Class<?> writerType =
          classLoader.loadClass(GENERATED_PACKAGE + "." + GENERATED_ROOT + "JsonWriter");
      return new CompiledBinding(
          classLoader,
          readerType.getMethod("read", JsonReader.class),
          validatorType.getMethod("validate", modelType),
          writerType.getMethod("write", JsonWriter.class, modelType));
    }

    void assertValidRoundTrip(String json, String label) throws ReflectiveOperationException {
      Object value = readValue(json);
      ValidationResult validationResult = (ValidationResult) validate.invoke(null, value);
      assertTrue(validationResult.isValid(), label + ", errors=" + validationResult.errors());

      JsonStringWriter writer = new JsonStringWriter();
      write.invoke(null, writer, value);
      String writtenJson = assertDoesNotThrow(writer::json, label);
      Object reread = readValue(writtenJson);
      ValidationResult rereadValidation = (ValidationResult) validate.invoke(null, reread);
      assertTrue(rereadValidation.isValid(), label + ", written=" + writtenJson);
    }

    void assertInvalid(String json, String label) throws ReflectiveOperationException {
      Object value;
      try {
        value = readValue(json);
      } catch (InvocationTargetException exception) {
        if (exception.getCause() instanceof JsonReadException) {
          return;
        }
        throw exception;
      }

      ValidationResult validationResult = (ValidationResult) validate.invoke(null, value);
      assertFalse(validationResult.isValid(), label);
      for (ValidationError error : validationResult.errors()) {
        assertFalse(error.code().isBlank(), label);
        assertFalse(error.path().value().isBlank(), label);
      }
    }

    private Object readValue(String json) throws ReflectiveOperationException {
      return read.invoke(null, new JsonStreamReader(json));
    }

    @Override
    public void close() throws IOException {
      classLoader.close();
    }
  }
}
