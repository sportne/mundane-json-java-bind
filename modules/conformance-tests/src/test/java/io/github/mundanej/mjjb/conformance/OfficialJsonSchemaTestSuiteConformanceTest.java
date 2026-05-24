package io.github.mundanej.mjjb.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.github.mundanej.mjjb.generator.api.GeneratorRequest;
import io.github.mundanej.mjjb.generator.api.GeneratorResult;
import io.github.mundanej.mjjb.generator.core.CoreGenerator;
import io.github.mundanej.mjjb.parser.JsonStreamReader;
import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.JsonReader;
import io.github.mundanej.mjjb.runtime.ValidationResult;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class OfficialJsonSchemaTestSuiteConformanceTest {
  private static final String UPSTREAM_COMMIT = "ba30ec795b67fb0bb636fedda3210b95d8cf558b";
  private static final String GENERATED_PACKAGE = GeneratorRequest.DEFAULT_PACKAGE;
  private static final String GENERATED_ROOT = GeneratorRequest.DEFAULT_ROOT_TYPE_NAME;
  private static final List<String> REQUIRED_CATEGORIES =
      List.of(
          "type",
          "minLength",
          "maxLength",
          "minimum",
          "maximum",
          "exclusiveMinimum",
          "exclusiveMaximum",
          "minItems",
          "maxItems",
          "properties",
          "additionalProperties",
          "allOf",
          "ref",
          "enum",
          "const");
  private static final List<AllowedCase> ALLOWED_CASES =
      List.copyOf(
          List.of(
              allowed(
                  "type",
                  "tests/draft2020-12/type.json",
                  "string type matches strings",
                  "a string is a string",
                  "{\"type\":\"string\"}",
                  "\"foo\"",
                  true),
              allowed(
                  "type",
                  "tests/draft2020-12/type.json",
                  "string type matches strings",
                  "an integer is not a string",
                  "{\"type\":\"string\"}",
                  "1",
                  false),
              allowed(
                  "type",
                  "tests/draft2020-12/type.json",
                  "boolean type matches booleans",
                  "a boolean is a boolean",
                  "{\"type\":\"boolean\"}",
                  "true",
                  true),
              allowed(
                  "type",
                  "tests/draft2020-12/type.json",
                  "boolean type matches booleans",
                  "an integer is not a boolean",
                  "{\"type\":\"boolean\"}",
                  "1",
                  false),
              allowed(
                  "properties",
                  "tests/draft2020-12/properties.json",
                  "properties validation",
                  "object property validates nested property",
                  """
                  {
                    "type": "object",
                    "properties": {
                      "name": {"type": "string", "minLength": 2},
                      "address": {
                        "type": "object",
                        "properties": {
                          "city": {"type": "string", "minLength": 2}
                        },
                        "required": ["city"],
                        "additionalProperties": false
                      }
                    },
                    "required": ["name", "address"],
                    "additionalProperties": false
                  }
                  """,
                  "{\"name\":\"Ada\",\"address\":{\"city\":\"Paris\"}}",
                  true),
              allowed(
                  "properties",
                  "tests/draft2020-12/properties.json",
                  "properties validation",
                  "nested property validation failure is invalid",
                  """
                  {
                    "type": "object",
                    "properties": {
                      "name": {"type": "string", "minLength": 2},
                      "address": {
                        "type": "object",
                        "properties": {
                          "city": {"type": "string", "minLength": 2}
                        },
                        "required": ["city"],
                        "additionalProperties": false
                      }
                    },
                    "required": ["name", "address"],
                    "additionalProperties": false
                  }
                  """,
                  "{\"name\":\"Ada\",\"address\":{\"city\":\"A\"}}",
                  false),
              allowedProjected(
                  "additionalProperties",
                  "tests/draft2020-12/additionalProperties.json",
                  "additionalProperties with schema",
                  "additionalProperties schema validates additional property",
                  """
                  {
                    "$schema": "https://json-schema.org/draft/2020-12/schema",
                    "$comment": "Projected JSON Schema Test Suite case pinned at __COMMIT__.",
                    "type": "object",
                    "properties": {
                      "known": {"type": "string"}
                    },
                    "required": ["known"],
                    "additionalProperties": {"type": "string", "minLength": 2}
                  }
                  """,
                  "{\"known\":\"ok\",\"extra\":\"ab\"}",
                  true),
              allowedProjected(
                  "additionalProperties",
                  "tests/draft2020-12/additionalProperties.json",
                  "additionalProperties with schema",
                  "additionalProperties schema rejects invalid additional property",
                  """
                  {
                    "$schema": "https://json-schema.org/draft/2020-12/schema",
                    "$comment": "Projected JSON Schema Test Suite case pinned at __COMMIT__.",
                    "type": "object",
                    "properties": {
                      "known": {"type": "string"}
                    },
                    "required": ["known"],
                    "additionalProperties": {"type": "string", "minLength": 2}
                  }
                  """,
                  "{\"known\":\"ok\",\"extra\":\"a\"}",
                  false),
              allowedProjected(
                  "allOf",
                  "tests/draft2020-12/allOf.json",
                  "allOf",
                  "allOf with two object schemas is valid",
                  """
                  {
                    "$schema": "https://json-schema.org/draft/2020-12/schema",
                    "$comment": "Projected JSON Schema Test Suite case pinned at __COMMIT__.",
                    "type": "object",
                    "properties": {
                      "id": {"type": "string", "minLength": 2},
                      "count": {"type": "integer"}
                    },
                    "additionalProperties": false,
                    "allOf": [
                      {
                        "type": "object",
                        "required": ["id"]
                      },
                      {
                        "type": "object",
                        "required": ["count"]
                      }
                    ]
                  }
                  """,
                  "{\"id\":\"ok\",\"count\":1}",
                  true),
              allowedProjected(
                  "allOf",
                  "tests/draft2020-12/allOf.json",
                  "allOf",
                  "allOf branch validation failure is invalid",
                  """
                  {
                    "$schema": "https://json-schema.org/draft/2020-12/schema",
                    "$comment": "Projected JSON Schema Test Suite case pinned at __COMMIT__.",
                    "type": "object",
                    "properties": {
                      "id": {"type": "string", "minLength": 2},
                      "count": {"type": "integer"}
                    },
                    "additionalProperties": false,
                    "allOf": [
                      {
                        "type": "object",
                        "required": ["id"]
                      },
                      {
                        "type": "object",
                        "required": ["count"]
                      }
                    ]
                  }
                  """,
                  "{\"id\":\"x\",\"count\":1}",
                  false),
              allowed(
                  "ref",
                  "tests/draft2020-12/ref.json",
                  "root pointer ref",
                  "local ref valid",
                  """
                  {
                    "$ref": "#/$defs/value",
                    "$defs": {
                      "value": {"type": "string", "minLength": 2}
                    }
                  }
                  """,
                  "\"ok\"",
                  true),
              allowed(
                  "ref",
                  "tests/draft2020-12/ref.json",
                  "root pointer ref",
                  "local ref invalid",
                  """
                  {
                    "$ref": "#/$defs/value",
                    "$defs": {
                      "value": {"type": "string", "minLength": 2}
                    }
                  }
                  """,
                  "\"x\"",
                  false),
              allowed(
                  "minLength",
                  "tests/draft2020-12/minLength.json",
                  "minLength validation",
                  "exact length is valid",
                  "{\"type\":\"string\",\"minLength\":2}",
                  "\"fo\"",
                  true),
              allowed(
                  "minLength",
                  "tests/draft2020-12/minLength.json",
                  "minLength validation",
                  "too short is invalid",
                  "{\"type\":\"string\",\"minLength\":2}",
                  "\"f\"",
                  false),
              allowed(
                  "maxLength",
                  "tests/draft2020-12/maxLength.json",
                  "maxLength validation",
                  "exact length is valid",
                  "{\"type\":\"string\",\"maxLength\":2}",
                  "\"fo\"",
                  true),
              allowed(
                  "maxLength",
                  "tests/draft2020-12/maxLength.json",
                  "maxLength validation",
                  "too long is invalid",
                  "{\"type\":\"string\",\"maxLength\":2}",
                  "\"foo\"",
                  false),
              allowed(
                  "minimum",
                  "tests/draft2020-12/minimum.json",
                  "minimum validation",
                  "boundary point is valid",
                  "{\"type\":\"number\",\"minimum\":1.1}",
                  "1.1",
                  true),
              allowed(
                  "minimum",
                  "tests/draft2020-12/minimum.json",
                  "minimum validation",
                  "below the minimum is invalid",
                  "{\"type\":\"number\",\"minimum\":1.1}",
                  "0.6",
                  false),
              allowed(
                  "maximum",
                  "tests/draft2020-12/maximum.json",
                  "maximum validation",
                  "boundary point is valid",
                  "{\"type\":\"number\",\"maximum\":3.0}",
                  "3.0",
                  true),
              allowed(
                  "maximum",
                  "tests/draft2020-12/maximum.json",
                  "maximum validation",
                  "above the maximum is invalid",
                  "{\"type\":\"number\",\"maximum\":3.0}",
                  "3.5",
                  false),
              allowed(
                  "exclusiveMinimum",
                  "tests/draft2020-12/exclusiveMinimum.json",
                  "exclusiveMinimum validation",
                  "above the exclusiveMinimum is valid",
                  "{\"type\":\"number\",\"exclusiveMinimum\":1.1}",
                  "1.2",
                  true),
              allowed(
                  "exclusiveMinimum",
                  "tests/draft2020-12/exclusiveMinimum.json",
                  "exclusiveMinimum validation",
                  "boundary point is invalid",
                  "{\"type\":\"number\",\"exclusiveMinimum\":1.1}",
                  "1.1",
                  false),
              allowed(
                  "exclusiveMaximum",
                  "tests/draft2020-12/exclusiveMaximum.json",
                  "exclusiveMaximum validation",
                  "below the exclusiveMaximum is valid",
                  "{\"type\":\"number\",\"exclusiveMaximum\":3.0}",
                  "2.2",
                  true),
              allowed(
                  "exclusiveMaximum",
                  "tests/draft2020-12/exclusiveMaximum.json",
                  "exclusiveMaximum validation",
                  "boundary point is invalid",
                  "{\"type\":\"number\",\"exclusiveMaximum\":3.0}",
                  "3.0",
                  false),
              allowed(
                  "minItems",
                  "tests/draft2020-12/minItems.json",
                  "minItems validation",
                  "exact length is valid",
                  "{\"type\":\"array\",\"items\":{\"type\":\"integer\"},\"minItems\":1}",
                  "[1]",
                  true),
              allowed(
                  "minItems",
                  "tests/draft2020-12/minItems.json",
                  "minItems validation",
                  "too short is invalid",
                  "{\"type\":\"array\",\"items\":{\"type\":\"integer\"},\"minItems\":1}",
                  "[]",
                  false),
              allowed(
                  "maxItems",
                  "tests/draft2020-12/maxItems.json",
                  "maxItems validation",
                  "exact length is valid",
                  "{\"type\":\"array\",\"items\":{\"type\":\"integer\"},\"maxItems\":2}",
                  "[1,2]",
                  true),
              allowed(
                  "maxItems",
                  "tests/draft2020-12/maxItems.json",
                  "maxItems validation",
                  "too long is invalid",
                  "{\"type\":\"array\",\"items\":{\"type\":\"integer\"},\"maxItems\":2}",
                  "[1,2,3]",
                  false),
              allowed(
                  "enum",
                  "tests/draft2020-12/enum.json",
                  "simple enum validation",
                  "one of the enum is valid",
                  "{\"type\":\"integer\",\"enum\":[1,2,3]}",
                  "1",
                  true),
              allowed(
                  "enum",
                  "tests/draft2020-12/enum.json",
                  "simple enum validation",
                  "something else is invalid",
                  "{\"type\":\"integer\",\"enum\":[1,2,3]}",
                  "4",
                  false),
              allowed(
                  "const",
                  "tests/draft2020-12/const.json",
                  "const validation",
                  "same value is valid",
                  "{\"type\":\"integer\",\"const\":2}",
                  "2",
                  true),
              allowed(
                  "const",
                  "tests/draft2020-12/const.json",
                  "const validation",
                  "another value is invalid",
                  "{\"type\":\"integer\",\"const\":2}",
                  "5",
                  false),
              allowed(
                  "const",
                  "tests/draft2020-12/const.json",
                  "const validation",
                  "another type is invalid",
                  "{\"type\":\"integer\",\"const\":2}",
                  "\"two\"",
                  false)));
  private static final List<SkippedCase> SKIPPED_CASES =
      List.copyOf(
          List.of(
              skipped(
                  "tests/draft2020-12/type.json",
                  "integer type matches integers",
                  "a float with zero fractional part is an integer",
                  SkipReason.NUMERIC_SEMANTICS_DEFERRED),
              skipped(
                  "tests/draft2020-12/type.json",
                  "string type matches strings",
                  "root string instance",
                  SkipReason.ROOT_NON_OBJECT_BINDING),
              skipped(
                  "tests/draft2020-12/required.json",
                  "required validation",
                  "required property is absent",
                  SkipReason.MISSING_REQUIRED_BINDING_SHAPE),
              skipped(
                  "tests/draft2020-12/properties.json",
                  "properties validation",
                  "untyped property schema",
                  SkipReason.UNTYPED_PROPERTY_SCHEMA),
              skipped(
                  "tests/draft2020-12/anyOf.json",
                  "anyOf",
                  "anyOf with one schema",
                  SkipReason.UNSUPPORTED_KEYWORD),
              skipped(
                  "tests/draft2020-12/additionalProperties.json",
                  "additionalProperties with boolean schema true",
                  "any additional property is valid",
                  SkipReason.UNSUPPORTED_KEYWORD_VALUE),
              skipped(
                  "tests/draft2020-12/ref.json",
                  "remote ref",
                  "remote ref valid",
                  SkipReason.REMOTE_REFERENCE),
              skipped(
                  "tests/draft2020-12/format.json",
                  "validation of e-mail addresses",
                  "an invalid e-mail address",
                  SkipReason.OPTIONAL_FORMAT_SCOPE)));

  @TempDir Path tempDir;

  @Test
  void allowedCasesHaveProvenanceAndCategoryCoverage() {
    assertFalse(ALLOWED_CASES.isEmpty());
    for (AllowedCase allowedCase : ALLOWED_CASES) {
      assertFalse(allowedCase.upstreamPath().isBlank(), allowedCase.toString());
      assertFalse(allowedCase.caseDescription().isBlank(), allowedCase.toString());
      assertFalse(allowedCase.testDescription().isBlank(), allowedCase.toString());
      assertFalse(allowedCase.projectedBindingSchema().isBlank(), allowedCase.toString());
      assertFalse(allowedCase.jsonInstance().isBlank(), allowedCase.toString());
    }

    Set<String> categories =
        ALLOWED_CASES.stream().map(AllowedCase::category).collect(Collectors.toUnmodifiableSet());
    assertTrue(categories.containsAll(REQUIRED_CATEGORIES), categories.toString());
  }

  @Test
  void skippedCasesHaveValidReasonsAndProvenance() {
    assertFalse(SKIPPED_CASES.isEmpty());
    EnumSet<SkipReason> reasons = EnumSet.noneOf(SkipReason.class);
    for (SkippedCase skippedCase : SKIPPED_CASES) {
      assertFalse(skippedCase.upstreamPath().isBlank(), skippedCase.toString());
      assertFalse(skippedCase.caseDescription().isBlank(), skippedCase.toString());
      assertFalse(skippedCase.testDescription().isBlank(), skippedCase.toString());
      reasons.add(skippedCase.reason());
    }

    assertEquals(EnumSet.allOf(SkipReason.class), reasons);
  }

  @Test
  void allowedCasesExerciseGeneratedBindings() throws IOException, ReflectiveOperationException {
    for (int index = 0; index < ALLOWED_CASES.size(); index++) {
      AllowedCase allowedCase = ALLOWED_CASES.get(index);
      boolean actualValid = runAllowedCase(index, allowedCase);
      assertEquals(allowedCase.expectedValid(), actualValid, allowedCase.toString());
    }
  }

  private boolean runAllowedCase(int index, AllowedCase allowedCase)
      throws IOException, ReflectiveOperationException {
    Path workspace = tempDir.resolve("%02d-%s".formatted(index, sanitize(allowedCase.category())));
    Path schema = workspace.resolve("schema.json");
    Path generated = workspace.resolve("generated");
    Path classes = workspace.resolve("classes");
    Files.createDirectories(workspace);
    Files.writeString(schema, allowedCase.projectedBindingSchema(), StandardCharsets.UTF_8);

    GeneratorResult result =
        new CoreGenerator().generate(GeneratorRequest.of(List.of(schema), generated));
    if (!result.successful()) {
      fail(
          "Generation failed for "
              + allowedCase
              + ": "
              + result.diagnostics().stream()
                  .map(diagnostic -> diagnostic.toManifestLine())
                  .toList());
    }
    assertEquals(4, result.generatedSources().size(), allowedCase.toString());
    compileGeneratedSources(allowedCase, result.generatedSources(), classes);

    return readAndValidate(allowedCase, classes);
  }

  private boolean readAndValidate(AllowedCase allowedCase, Path classes)
      throws IOException, ReflectiveOperationException {
    ArrayList<URL> urls = new ArrayList<>();
    urls.add(classes.toUri().toURL());
    for (Path path : currentProcessClasspath()) {
      urls.add(path.toUri().toURL());
    }

    try (URLClassLoader classLoader =
        new URLClassLoader(
            urls.toArray(URL[]::new),
            OfficialJsonSchemaTestSuiteConformanceTest.class.getClassLoader())) {
      Class<?> readerType =
          classLoader.loadClass(GENERATED_PACKAGE + "." + GENERATED_ROOT + "JsonReader");
      Method read = readerType.getMethod("read", JsonReader.class);
      Object generatedValue;
      try {
        generatedValue = read.invoke(null, new JsonStreamReader(allowedCase.jsonInstance()));
      } catch (InvocationTargetException exception) {
        if (exception.getCause() instanceof JsonReadException) {
          return false;
        }
        throw exception;
      }

      Class<?> validatorType =
          classLoader.loadClass(GENERATED_PACKAGE + "." + GENERATED_ROOT + "JsonValidator");
      Method validate = validatorType.getMethod("validate", generatedValue.getClass());
      ValidationResult validationResult = (ValidationResult) validate.invoke(null, generatedValue);
      return validationResult.isValid();
    }
  }

  private void compileGeneratedSources(
      AllowedCase allowedCase, List<Path> generatedSources, Path classes) throws IOException {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      fail("System Java compiler is not available for " + allowedCase);
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

    ByteArrayOutputStream errors = new ByteArrayOutputStream();
    int result;
    try (PrintStream errorStream = new PrintStream(errors, true, StandardCharsets.UTF_8)) {
      result = compiler.run(null, null, errorStream, arguments.toArray(String[]::new));
    }
    if (result != 0) {
      fail(
          "Compilation failed for "
              + allowedCase
              + ": "
              + errors.toString(StandardCharsets.UTF_8).trim());
    }
  }

  private static AllowedCase allowed(
      String category,
      String upstreamPath,
      String caseDescription,
      String testDescription,
      String valueSchema,
      String valueJson,
      boolean expectedValid) {
    return new AllowedCase(
        category,
        upstreamPath,
        caseDescription,
        testDescription,
        objectSchema(valueSchema),
        objectInstance(valueJson),
        expectedValid);
  }

  private static AllowedCase allowedProjected(
      String category,
      String upstreamPath,
      String caseDescription,
      String testDescription,
      String schema,
      String json,
      boolean expectedValid) {
    return new AllowedCase(
        category,
        upstreamPath,
        caseDescription,
        testDescription,
        schema.replace("__COMMIT__", UPSTREAM_COMMIT),
        json,
        expectedValid);
  }

  private static SkippedCase skipped(
      String upstreamPath, String caseDescription, String testDescription, SkipReason reason) {
    return new SkippedCase(upstreamPath, caseDescription, testDescription, reason);
  }

  private static String objectSchema(String valueSchema) {
    return """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "$comment": "Projected JSON Schema Test Suite case pinned at __COMMIT__.",
          "$defs": {
            "value": {"type": "string", "minLength": 2}
          },
          "type": "object",
          "properties": {
            "value": __VALUE_SCHEMA__
          },
          "required": ["value"],
          "additionalProperties": false
        }
        """
        .replace("__COMMIT__", UPSTREAM_COMMIT)
        .replace("__VALUE_SCHEMA__", valueSchema);
  }

  private static String objectInstance(String valueJson) {
    return "{\"value\":" + valueJson + "}";
  }

  private static List<Path> classpathWith(Path classes) {
    ArrayList<Path> paths = new ArrayList<>();
    paths.add(classes);
    paths.addAll(currentProcessClasspath());
    return paths;
  }

  private static String joinClasspath(List<Path> classpath) {
    return classpath.stream()
        .map(Path::toString)
        .collect(Collectors.joining(java.io.File.pathSeparator));
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

  private static String sanitize(String value) {
    return value.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
  }

  private enum SkipReason {
    ROOT_NON_OBJECT_BINDING,
    MISSING_REQUIRED_BINDING_SHAPE,
    UNTYPED_PROPERTY_SCHEMA,
    UNSUPPORTED_KEYWORD,
    UNSUPPORTED_KEYWORD_VALUE,
    REMOTE_REFERENCE,
    OPTIONAL_FORMAT_SCOPE,
    NUMERIC_SEMANTICS_DEFERRED
  }

  private record AllowedCase(
      String category,
      String upstreamPath,
      String caseDescription,
      String testDescription,
      String projectedBindingSchema,
      String jsonInstance,
      boolean expectedValid) {
    AllowedCase {
      Objects.requireNonNull(category, "category");
      Objects.requireNonNull(upstreamPath, "upstreamPath");
      Objects.requireNonNull(caseDescription, "caseDescription");
      Objects.requireNonNull(testDescription, "testDescription");
      Objects.requireNonNull(projectedBindingSchema, "projectedBindingSchema");
      Objects.requireNonNull(jsonInstance, "jsonInstance");
    }
  }

  private record SkippedCase(
      String upstreamPath, String caseDescription, String testDescription, SkipReason reason) {
    SkippedCase {
      Objects.requireNonNull(upstreamPath, "upstreamPath");
      Objects.requireNonNull(caseDescription, "caseDescription");
      Objects.requireNonNull(testDescription, "testDescription");
      Objects.requireNonNull(reason, "reason");
    }
  }
}
