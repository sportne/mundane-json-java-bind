package io.github.mundanej.mjjb.conformance.schemastore;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.github.mundanej.mjjb.generator.api.GeneratorProfile;
import io.github.mundanej.mjjb.generator.api.GeneratorRequest;
import io.github.mundanej.mjjb.generator.api.GeneratorResult;
import io.github.mundanej.mjjb.generator.core.CoreGenerator;
import io.github.mundanej.mjjb.parser.JsonStreamReader;
import io.github.mundanej.mjjb.parser.JsonStringWriter;
import io.github.mundanej.mjjb.runtime.JsonField;
import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.JsonReader;
import io.github.mundanej.mjjb.runtime.JsonWriteException;
import io.github.mundanej.mjjb.runtime.JsonWriter;
import io.github.mundanej.mjjb.runtime.SchemaBranchMetadata;
import io.github.mundanej.mjjb.runtime.SchemaObjectMetadata;
import io.github.mundanej.mjjb.runtime.SchemaPropertyMetadata;
import io.github.mundanej.mjjb.runtime.SchemaRootMetadata;
import io.github.mundanej.mjjb.runtime.ValidationResult;
import io.github.mundanej.mjjb.schema.model.SchemaSupportDiagnostic;
import io.github.mundanej.mjjb.schema.model.SchemaSupportProfile;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxParseResult;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxParser;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.ArrayValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.BooleanValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.Member;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.NullValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.NumberValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.ObjectValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.StringValue;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

final class SchemaStoreCorpusTest {
  private static final String MANIFEST_RESOURCE = "schemastore-corpus/manifest.tsv";
  private static final String GENERATED_ROOT = "SchemaStoreBinding";
  private static final int MINIMUM_CORPUS_SIZE = 90;
  private static final Set<String> SUPPORTED_FORMAT_SAMPLES = Set.of("date", "date-time", "uuid");
  private static final Pattern SLASH_PATTERN = Pattern.compile("/");
  private static final HttpClient HTTP_CLIENT =
      HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(20))
          .followRedirects(HttpClient.Redirect.NORMAL)
          .build();

  @Test
  void manifestEntriesMatchExpectedOutcomes()
      throws IOException, InterruptedException, JsonWriteException, ReflectiveOperationException {
    List<ManifestEntry> entries = readManifest();
    assertTrue(entries.size() >= MINIMUM_CORPUS_SIZE, "SchemaStore corpus is too small");
    assertTrue(
        entries.stream().anyMatch(entry -> entry.expectedOutcome() == ExpectedOutcome.GENERATES),
        "Corpus must include schemas expected to generate");
    assertTrue(
        entries.stream().anyMatch(entry -> entry.expectedOutcome() == ExpectedOutcome.REJECTS),
        "Corpus must include schemas expected to reject");

    Path workspace = Path.of(System.getProperty("mjjb.schemaStoreWorkspaceDir"));
    Path reportDirectory = Path.of(System.getProperty("mjjb.schemaStoreReportDir"));
    boolean refreshMode = Boolean.parseBoolean(System.getProperty("mjjb.schemaStoreRefresh"));
    Files.createDirectories(workspace);
    Files.createDirectories(reportDirectory);

    ArrayList<CorpusResult> results = new ArrayList<>();
    ArrayList<String> failures = new ArrayList<>();
    for (int index = 0; index < entries.size(); index++) {
      ManifestEntry entry = entries.get(index);
      CorpusResult result =
          runEntry(index, entry, workspace.resolve("%03d-%s".formatted(index, entry.slug())));
      results.add(result);
      if (!result.accepted(refreshMode)) {
        failures.add(result.failureMessage());
      }
    }

    writeReports(reportDirectory, results);
    if (!failures.isEmpty()) {
      fail(
          failures.stream()
                  .limit(20)
                  .collect(Collectors.joining(System.lineSeparator(), "", System.lineSeparator()))
              + "See "
              + reportDirectory.resolve("schemastore-corpus-results.md"));
    }
    if (!refreshMode) {
      assertDocumentationAggregateMatches(results);
    }
  }

  private static CorpusResult runEntry(int index, ManifestEntry entry, Path workspace)
      throws IOException, InterruptedException, JsonWriteException, ReflectiveOperationException {
    Files.createDirectories(workspace);
    DownloadedSchema downloaded = download(entry);
    Files.writeString(
        workspace.resolve("schema.json"), downloaded.selectedSource(), StandardCharsets.UTF_8);
    if (!entry.sha256().equals(downloaded.sha256())) {
      return CorpusResult.digestMismatch(entry, downloaded.sha256());
    }

    SchemaSyntaxParseResult parseResult = SchemaSyntaxParser.parse(downloaded.selectedSource());
    if (!parseResult.successful()) {
      return CorpusResult.diagnostic(
          entry, ActualOutcome.REJECTED_PARSE, parseResult.diagnostics().getFirst().code());
    }

    List<SchemaSupportDiagnostic> supportDiagnostics =
        SchemaSupportProfile.validate(parseResult.root());
    if (!supportDiagnostics.isEmpty()) {
      return CorpusResult.diagnostic(
          entry, ActualOutcome.REJECTED_PROFILE, supportDiagnostics.getFirst().code());
    }

    String packageName = "io.github.mundanej.mjjb.schemastore.case" + index;
    Path generatedDirectory = workspace.resolve("generated");
    GeneratorResult generatorResult =
        new CoreGenerator()
            .generate(
                new GeneratorRequest(
                    List.of(workspace.resolve("schema.json")),
                    generatedDirectory,
                    GeneratorProfile.JSP_DATA_2020_12,
                    packageName,
                    GENERATED_ROOT,
                    Map.of(),
                    true));
    if (!generatorResult.successful()) {
      return CorpusResult.diagnostic(
          entry, ActualOutcome.REJECTED_GENERATOR, generatorResult.diagnostics().getFirst().code());
    }

    Path classes = workspace.resolve("classes");
    compileGeneratedSources(entry, generatorResult.generatedSources(), classes);
    verifyGeneratedBinding(entry, parseResult.root(), packageName, classes);
    return CorpusResult.generated(entry);
  }

  private static void verifyGeneratedBinding(
      ManifestEntry entry, SchemaSyntaxValue schemaRoot, String packageName, Path classes)
      throws IOException, JsonWriteException, ReflectiveOperationException {
    try (URLClassLoader classLoader = classLoaderWith(classes)) {
      Class<?> rootType = classLoader.loadClass(packageName + "." + GENERATED_ROOT);
      Class<?> metadataType =
          classLoader.loadClass(packageName + "." + GENERATED_ROOT + "JsonSchemaMetadata");
      Class<?> readerType =
          classLoader.loadClass(packageName + "." + GENERATED_ROOT + "JsonReader");
      Class<?> writerType =
          classLoader.loadClass(packageName + "." + GENERATED_ROOT + "JsonWriter");
      Class<?> validatorType =
          classLoader.loadClass(packageName + "." + GENERATED_ROOT + "JsonValidator");
      SchemaRootMetadata rootMetadata =
          (SchemaRootMetadata) metadataType.getMethod("root").invoke(null);

      GeneratedSample sample = buildSample(entry, schemaRoot, rootType, rootMetadata, classLoader);
      JsonStringWriter writer = new JsonStringWriter();
      writerType
          .getMethod("write", JsonWriter.class, rootType)
          .invoke(null, writer, sample.value());
      String json = writer.json();

      Object roundTripped =
          readerType.getMethod("read", JsonReader.class).invoke(null, new JsonStreamReader(json));
      assertEquals(sample.value(), roundTripped, entry.name());
      ValidationResult validationResult =
          (ValidationResult)
              validatorType.getMethod("validate", rootType).invoke(null, sample.value());
      assertTrue(validationResult.isValid(), entry.name() + " " + validationResult.errors());

      verifyInvalidJsonIsRejected(entry, readerType, rootMetadata, sample);
    }
  }

  private static GeneratedSample buildSample(
      ManifestEntry entry,
      SchemaSyntaxValue schemaRoot,
      Class<?> rootType,
      SchemaRootMetadata rootMetadata,
      ClassLoader classLoader)
      throws ReflectiveOperationException {
    if (rootType.isSealed()) {
      SchemaBranchMetadata branch = rootMetadata.branches().getFirst();
      Class<?> branchType = classLoader.loadClass(rootType.getName() + "$" + branch.javaTypeName());
      ConstructedRecord branchValue =
          constructRecord(entry, schemaRoot, branchType, branch.object());
      return new GeneratedSample(
          branchValue.value(), branch.object(), branch.tagValue(), branchValue.jsonValues());
    }
    ConstructedRecord rootValue =
        constructRecord(entry, schemaRoot, rootType, rootMetadata.rootObject());
    return new GeneratedSample(
        rootValue.value(), rootMetadata.rootObject(), null, rootValue.jsonValues());
  }

  private static ConstructedRecord constructRecord(
      ManifestEntry entry,
      SchemaSyntaxValue schemaRoot,
      Class<?> recordType,
      SchemaObjectMetadata objectMetadata)
      throws ReflectiveOperationException {
    assertTrue(recordType.isRecord(), entry.name() + " generated type must be a record");
    RecordComponent[] components = recordType.getRecordComponents();
    ArrayList<Object> values = new ArrayList<>();
    LinkedHashMap<String, String> jsonValues = new LinkedHashMap<>();
    for (RecordComponent component : components) {
      SchemaPropertyMetadata property =
          objectMetadata.properties().stream()
              .filter(candidate -> component.getName().equals(candidate.javaFieldName()))
              .findFirst()
              .orElseThrow(
                  () ->
                      new AssertionError(
                          entry.name() + " missing metadata for " + component.getName()));
      SchemaSyntaxValue propertySchema = schemaAt(schemaRoot, property.schemaPointer());
      values.add(sampleJavaValue(component.getType(), property.javaType(), propertySchema));
      jsonValues.put(
          property.jsonName(), jsonValueForJavaType(property.javaType(), propertySchema));
    }
    Constructor<?> constructor =
        recordType.getDeclaredConstructor(
            Arrays.stream(components).map(RecordComponent::getType).toArray(Class[]::new));
    Object value = constructor.newInstance(values.toArray());
    return new ConstructedRecord(value, jsonValues);
  }

  private static Object sampleJavaValue(
      Class<?> rawType, String javaType, SchemaSyntaxValue schema) {
    if (rawType == Optional.class) {
      return Optional.of(sampleJavaValueForJavaType(innerType(javaType), schema));
    }
    if (rawType == JsonField.class) {
      return JsonField.value(sampleJavaValueForJavaType(innerType(javaType), schema));
    }
    if (rawType == List.class) {
      return listSample(innerType(javaType), schema);
    }
    if (rawType == String.class) {
      return stringSample(schema);
    }
    if (rawType == long.class || rawType == Long.class) {
      return longSample(schema);
    }
    if (rawType == double.class || rawType == Double.class) {
      return doubleSample(schema);
    }
    if (rawType == boolean.class || rawType == Boolean.class) {
      return booleanSample(schema);
    }
    throw new IllegalArgumentException("Unsupported generated Java type " + rawType.getName());
  }

  private static Object sampleJavaValueForJavaType(String javaType, SchemaSyntaxValue schema) {
    if (javaType.startsWith("List<")) {
      return listSample(innerType(javaType), schema);
    }
    return switch (javaType) {
      case "String" -> stringSample(schema);
      case "long", "Long" -> longSample(schema);
      case "double", "Double" -> doubleSample(schema);
      case "boolean", "Boolean" -> booleanSample(schema);
      default -> throw new IllegalArgumentException("Unsupported generated Java type " + javaType);
    };
  }

  private static void verifyInvalidJsonIsRejected(
      ManifestEntry entry,
      Class<?> readerType,
      SchemaRootMetadata rootMetadata,
      GeneratedSample sample)
      throws NoSuchMethodException {
    Method read = readerType.getMethod("read", JsonReader.class);
    assertJsonReadFailure(entry, read, "[]");

    String unknownJson = objectJson(rootMetadata, sample, Map.of("__mjjb_unknown", "true"));
    assertJsonReadFailure(entry, read, unknownJson);

    Optional<SchemaPropertyMetadata> firstProperty = sample.firstProperty();
    if (firstProperty.isPresent()) {
      String duplicatePropertyJson =
          objectJson(rootMetadata, sample, duplicateMap(firstProperty.orElseThrow(), sample));
      assertJsonReadFailure(entry, read, duplicatePropertyJson);
    }

    Optional<SchemaPropertyMetadata> requiredProperty = sample.firstRequiredProperty();
    if (requiredProperty.isPresent()) {
      String missingRequiredJson =
          objectJson(rootMetadata, sample, Map.of(), Set.of(requiredProperty.orElseThrow()));
      assertJsonReadFailure(entry, read, missingRequiredJson);
    }

    Optional<SchemaPropertyMetadata> scalarProperty = sample.firstScalarProperty();
    if (scalarProperty.isPresent()) {
      String wrongTypeJson =
          objectJson(
              rootMetadata,
              sample,
              Map.of(
                  scalarProperty.orElseThrow().jsonName(),
                  wrongScalarJson(scalarProperty.orElseThrow())));
      assertJsonReadFailure(entry, read, wrongTypeJson);
    }
  }

  private static Map<String, String> duplicateMap(
      SchemaPropertyMetadata property, GeneratedSample sample) {
    LinkedHashMap<String, String> values = new LinkedHashMap<>();
    String value = sample.jsonValue(property);
    values.put(
        property.jsonName(), value + ",\"" + escapeJson(property.jsonName()) + "\":" + value);
    return values;
  }

  private static void assertJsonReadFailure(ManifestEntry entry, Method read, String json) {
    InvocationTargetException failure =
        assertInstanceOf(
            InvocationTargetException.class,
            assertDoesNotThrow(
                () ->
                    assertThrowsInvocation(
                        () -> read.invoke(null, new JsonStreamReader(json)), entry.name())));
    assertTrue(
        failure.getCause() instanceof JsonReadException,
        entry.name() + " expected JsonReadException for " + json);
  }

  private static InvocationTargetException assertThrowsInvocation(
      ThrowingRunnable runnable, String name) {
    try {
      runnable.run();
    } catch (InvocationTargetException exception) {
      return exception;
    } catch (ReflectiveOperationException exception) {
      throw new LinkageError(name + " unexpected reflection failure", exception);
    }
    throw new AssertionError(name + " expected generated reader to reject invalid JSON");
  }

  private static String objectJson(
      SchemaRootMetadata rootMetadata, GeneratedSample sample, Map<String, String> overrides) {
    return objectJson(rootMetadata, sample, overrides, Set.of());
  }

  private static String objectJson(
      SchemaRootMetadata rootMetadata,
      GeneratedSample sample,
      Map<String, String> overrides,
      Set<SchemaPropertyMetadata> omitted) {
    ArrayList<String> fields = new ArrayList<>();
    rootMetadata
        .tagPropertyName()
        .ifPresent(
            tag -> fields.add("\"" + escapeJson(tag) + "\":\"" + escapeJson(sample.tag()) + "\""));
    for (Map.Entry<String, String> override : overrides.entrySet()) {
      fields.add("\"" + escapeJson(override.getKey()) + "\":" + override.getValue());
    }
    for (SchemaPropertyMetadata property : sample.objectMetadata().properties()) {
      if (omitted.contains(property) || overrides.containsKey(property.jsonName())) {
        continue;
      }
      fields.add("\"" + escapeJson(property.jsonName()) + "\":" + sample.jsonValue(property));
    }
    return "{" + String.join(",", fields) + "}";
  }

  private static String wrongScalarJson(SchemaPropertyMetadata property) {
    if (property.javaType().contains("String")) {
      return "42";
    }
    if (property.javaType().contains("Boolean") || property.javaType().equals("boolean")) {
      return "\"not-boolean\"";
    }
    return "\"not-number\"";
  }

  private static void compileGeneratedSources(
      ManifestEntry entry, List<Path> generatedSources, Path classes) throws IOException {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      fail("System Java compiler is not available for " + entry.name());
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
              + entry.name()
              + ": "
              + errors.toString(StandardCharsets.UTF_8).trim());
    }
  }

  private static DownloadedSchema download(ManifestEntry entry)
      throws IOException, InterruptedException {
    URI entryUri = URI.create(entry.url());
    URI fetchUri = withoutFragment(entryUri);
    HttpRequest request =
        HttpRequest.newBuilder(fetchUri)
            .timeout(Duration.ofSeconds(30))
            .header("User-Agent", "mundane-json-java-bind-schemastore-corpus")
            .GET()
            .build();
    HttpResponse<byte[]> response =
        HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IOException(entry.name() + " returned HTTP " + response.statusCode());
    }
    String source = new String(response.body(), StandardCharsets.UTF_8);
    return new DownloadedSchema(
        selectSchemaSource(source, entryUri.getFragment()), sha256(response.body()));
  }

  private static URI withoutFragment(URI uri) throws IOException {
    try {
      return new URI(
          uri.getScheme(),
          uri.getUserInfo(),
          uri.getHost(),
          uri.getPort(),
          uri.getPath(),
          uri.getQuery(),
          null);
    } catch (java.net.URISyntaxException exception) {
      throw new IOException("Invalid SchemaStore URL " + uri, exception);
    }
  }

  private static String selectSchemaSource(String source, String fragment) throws IOException {
    if (fragment == null || fragment.isBlank()) {
      return source;
    }
    if (!fragment.startsWith("/")) {
      throw new IOException("Only JSON Pointer URL fragments are supported: #" + fragment);
    }
    SchemaSyntaxParseResult parseResult = SchemaSyntaxParser.parse(source);
    if (!parseResult.successful()) {
      throw new IOException("Cannot parse downloaded schema for fragment selection.");
    }
    return toJson(schemaAt(parseResult.root(), fragment));
  }

  private static String toJson(SchemaSyntaxValue value) {
    return switch (value) {
      case ObjectValue object ->
          object.members().stream()
              .map(member -> "\"" + escapeJson(member.name()) + "\":" + toJson(member.value()))
              .collect(Collectors.joining(",", "{", "}"));
      case ArrayValue array ->
          array.items().stream()
              .map(SchemaStoreCorpusTest::toJson)
              .collect(Collectors.joining(",", "[", "]"));
      case StringValue stringValue -> "\"" + escapeJson(stringValue.value()) + "\"";
      case NumberValue numberValue -> numberValue.literal();
      case BooleanValue booleanValue -> Boolean.toString(booleanValue.value());
      case NullValue ignored -> "null";
    };
  }

  private static List<ManifestEntry> readManifest() throws IOException {
    String manifestPath = System.getProperty("mjjb.schemaStoreManifestPath");
    if (manifestPath != null && !manifestPath.isBlank()) {
      return parseManifest(Files.readString(Path.of(manifestPath), StandardCharsets.UTF_8));
    }
    try (InputStream stream =
        SchemaStoreCorpusTest.class.getClassLoader().getResourceAsStream(MANIFEST_RESOURCE)) {
      if (stream == null) {
        throw new IOException("Missing resource " + MANIFEST_RESOURCE);
      }
      return parseManifest(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
    }
  }

  private static List<ManifestEntry> parseManifest(String source) throws IOException {
    ArrayList<ManifestEntry> entries = new ArrayList<>();
    for (String line : source.lines().toList()) {
      if (line.isBlank() || line.startsWith("#")) {
        continue;
      }
      String[] parts = line.split("\\t", -1);
      if (parts.length != 5) {
        throw new IOException("Invalid manifest line: " + line);
      }
      entries.add(
          new ManifestEntry(
              parts[0],
              parts[1],
              parts[2],
              ExpectedOutcome.valueOf(parts[3]),
              ExpectedDiagnostic.valueOf(parts[4])));
    }
    return List.copyOf(entries);
  }

  private static void writeReports(Path reportDirectory, List<CorpusResult> results)
      throws IOException {
    Files.writeString(
        reportDirectory.resolve("schemastore-corpus-results.md"), markdownReport(results));
    Files.writeString(
        reportDirectory.resolve("schemastore-corpus-results.tsv"), tsvReport(results));
  }

  private static String markdownReport(List<CorpusResult> results) {
    AggregateCounts counts = aggregateCounts(results);
    StringBuilder builder = new StringBuilder();
    builder.append("# SchemaStore Corpus Results\n\n");
    builder.append("| Result | Count |\n");
    builder.append("|---|---:|\n");
    appendAggregateRow(builder, "Total manifest entries", counts.totalManifestEntries());
    appendAggregateRow(builder, "Generated, compiled, and round-tripped", counts.generated());
    appendAggregateRow(builder, "Expected profile rejections", counts.expectedProfileRejections());
    appendAggregateRow(
        builder, "Expected generator rejections", counts.expectedGeneratorRejections());
    appendAggregateRow(builder, "Digest drift failures", counts.digestDriftFailures());
    appendAggregateRow(builder, "Unexpected failures", counts.unexpectedFailures());
    builder.append('\n');
    builder.append("| Name | Expected | Actual | Diagnostic |\n");
    builder.append("|---|---|---|---|\n");
    for (CorpusResult result : results) {
      builder
          .append("| ")
          .append(escapeMarkdown(result.entry().name()))
          .append(" | ")
          .append(result.entry().expectedOutcome())
          .append(" | ")
          .append(result.actual())
          .append(" | ")
          .append(result.diagnostic())
          .append(" |\n");
    }
    return builder.toString();
  }

  private static void appendAggregateRow(StringBuilder builder, String label, long count) {
    builder.append("| ").append(label).append(" | ").append(count).append(" |\n");
  }

  private static String tsvReport(List<CorpusResult> results) {
    StringBuilder builder = new StringBuilder();
    builder.append(
        "name\turl\texpected\tactual\texpectedDiagnostic\tactualDiagnostic\tactualSha256\n");
    for (CorpusResult result : results) {
      ManifestEntry entry = result.entry();
      builder
          .append(entry.name())
          .append('\t')
          .append(entry.url())
          .append('\t')
          .append(entry.expectedOutcome())
          .append('\t')
          .append(result.actual())
          .append('\t')
          .append(entry.expectedDiagnostic())
          .append('\t')
          .append(result.diagnostic())
          .append('\t')
          .append(result.actualSha256())
          .append('\n');
    }
    return builder.toString();
  }

  private static AggregateCounts aggregateCounts(List<CorpusResult> results) {
    long generated =
        results.stream().filter(result -> result.actual() == ActualOutcome.GENERATED).count();
    long expectedProfileRejections =
        results.stream()
            .filter(result -> result.entry().expectedOutcome() == ExpectedOutcome.REJECTS)
            .filter(
                result ->
                    result.entry().expectedDiagnostic() == ExpectedDiagnostic.PROFILE_DIAGNOSTIC)
            .count();
    long expectedGeneratorRejections =
        results.stream()
            .filter(result -> result.entry().expectedOutcome() == ExpectedOutcome.REJECTS)
            .filter(
                result ->
                    result.entry().expectedDiagnostic() == ExpectedDiagnostic.GENERATOR_DIAGNOSTIC)
            .count();
    long digestDriftFailures =
        results.stream().filter(result -> result.actual() == ActualOutcome.DIGEST_MISMATCH).count();
    long unexpectedFailures =
        results.stream()
            .filter(result -> !result.accepted(false))
            .filter(result -> result.actual() != ActualOutcome.DIGEST_MISMATCH)
            .count();
    return new AggregateCounts(
        results.size(),
        generated,
        expectedProfileRejections,
        expectedGeneratorRejections,
        digestDriftFailures,
        unexpectedFailures);
  }

  private static void assertDocumentationAggregateMatches(List<CorpusResult> results)
      throws IOException {
    String documentation = Files.readString(corpusDocumentation());
    AggregateCounts counts = aggregateCounts(results);
    assertEquals(
        counts.totalManifestEntries(),
        documentedAggregateCount(documentation, "Total manifest entries"));
    assertEquals(
        counts.generated(),
        documentedAggregateCount(documentation, "Generated, compiled, and round-tripped"));
    assertEquals(
        counts.expectedProfileRejections(),
        documentedAggregateCount(documentation, "Expected profile rejections"));
    assertEquals(
        counts.expectedGeneratorRejections(),
        documentedAggregateCount(documentation, "Expected generator rejections"));
    assertEquals(
        counts.digestDriftFailures(),
        documentedAggregateCount(documentation, "Digest drift failures"));
    assertEquals(
        counts.unexpectedFailures(),
        documentedAggregateCount(documentation, "Unexpected failures"));
  }

  private static Path corpusDocumentation() {
    List<Path> candidates =
        List.of(
            Path.of("docs/verification/schemastore-corpus.md"),
            Path.of("../../docs/verification/schemastore-corpus.md"));
    return candidates.stream()
        .filter(Files::exists)
        .findFirst()
        .orElseThrow(
            () -> new IllegalStateException("Missing docs/verification/schemastore-corpus.md"));
  }

  private static long documentedAggregateCount(String documentation, String label) {
    String prefix = "| " + label + " |";
    return documentation
        .lines()
        .filter(line -> line.startsWith(prefix))
        .map(line -> line.split("\\|", -1))
        .map(parts -> parts[2].trim())
        .mapToLong(Long::parseLong)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Missing documented count for " + label));
  }

  private static URLClassLoader classLoaderWith(Path classes) throws IOException {
    ArrayList<URL> urls = new ArrayList<>();
    urls.add(classes.toUri().toURL());
    for (Path path : currentProcessClasspath()) {
      urls.add(path.toUri().toURL());
    }
    return new URLClassLoader(
        urls.toArray(URL[]::new), SchemaStoreCorpusTest.class.getClassLoader());
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

  private static SchemaSyntaxValue schemaAt(SchemaSyntaxValue root, String pointer) {
    if (pointer.isEmpty()) {
      return root;
    }
    SchemaSyntaxValue current = root;
    for (String rawToken : SLASH_PATTERN.split(pointer.substring(1), -1)) {
      String token = rawToken.replace("~1", "/").replace("~0", "~");
      if (current instanceof ObjectValue object) {
        current =
            object.members().stream()
                .filter(member -> token.equals(member.name()))
                .map(Member::value)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Missing pointer " + pointer));
      } else if (current instanceof ArrayValue array) {
        current = array.items().get(Integer.parseInt(token));
      } else {
        throw new IllegalArgumentException("Cannot dereference " + pointer);
      }
    }
    return current;
  }

  private static SchemaSyntaxValue itemsSchema(SchemaSyntaxValue schema) {
    return member(schema, "items").orElseThrow();
  }

  private static List<Object> listSample(String itemJavaType, SchemaSyntaxValue schema) {
    SchemaSyntaxValue itemSchema = itemsSchema(schema);
    ArrayList<Object> values = new ArrayList<>();
    for (int index = 0; index < arraySampleSize(schema); index++) {
      values.add(sampleJavaValueForJavaType(itemJavaType, itemSchema));
    }
    return List.copyOf(values);
  }

  private static int arraySampleSize(SchemaSyntaxValue schema) {
    Optional<Integer> maxItems = integerMember(schema, "maxItems");
    int minItems = integerMember(schema, "minItems").orElse(maxItems.orElse(1) == 0 ? 0 : 1);
    int preferred = Math.max(minItems, 0);
    int upper = maxItems.orElse(Math.max(preferred, 1));
    return Math.max(0, Math.min(preferred, upper));
  }

  private static Optional<SchemaSyntaxValue> member(SchemaSyntaxValue schema, String name) {
    if (!(schema instanceof ObjectValue object)) {
      return Optional.empty();
    }
    return object.members().stream()
        .filter(member -> name.equals(member.name()))
        .map(Member::value)
        .findFirst();
  }

  private static String stringSample(SchemaSyntaxValue schema) {
    Optional<Object> literal = preferredLiteral(schema, "string");
    if (literal.isPresent()) {
      return literal.orElseThrow().toString();
    }
    String format = stringMember(schema, "format").orElse("");
    if (SUPPORTED_FORMAT_SAMPLES.contains(format)) {
      return switch (format) {
        case "date" -> "2026-05-23";
        case "date-time" -> "2026-05-23T12:00:00Z";
        case "uuid" -> "123e4567-e89b-12d3-a456-426614174000";
        default -> throw new IllegalStateException(format);
      };
    }
    Optional<Integer> maxLengthMember = integerMember(schema, "maxLength");
    int minLength =
        integerMember(schema, "minLength").orElse(maxLengthMember.orElse(1) == 0 ? 0 : 1);
    int maxLength = maxLengthMember.orElse(Math.max(minLength, 16));
    int length = Math.max(0, Math.min(Math.max(minLength, 0), maxLength));
    return "a".repeat(length);
  }

  private static long longSample(SchemaSyntaxValue schema) {
    Optional<Object> literal = preferredLiteral(schema, "integer");
    if (literal.isPresent()) {
      return ((Number) literal.orElseThrow()).longValue();
    }
    long minimum = numericMember(schema, "minimum").map(Math::ceil).orElse(1.0).longValue();
    long exclusiveMinimum =
        numericMember(schema, "exclusiveMinimum")
            .map(value -> (long) Math.floor(value) + 1L)
            .orElse(minimum);
    long lower = Math.max(minimum, exclusiveMinimum);
    long maximum =
        numericMember(schema, "maximum").map(Math::floor).orElse((double) lower + 10).longValue();
    long exclusiveMaximum =
        numericMember(schema, "exclusiveMaximum")
            .map(value -> (long) Math.ceil(value) - 1L)
            .orElse(maximum);
    long upper = Math.min(maximum, exclusiveMaximum);
    return Math.min(Math.max(lower, 1L), upper);
  }

  private static double doubleSample(SchemaSyntaxValue schema) {
    Optional<Object> literal = preferredLiteral(schema, "number");
    if (literal.isPresent()) {
      return ((Number) literal.orElseThrow()).doubleValue();
    }
    double lower =
        Math.max(
            numericMember(schema, "minimum").orElse(1.0),
            numericMember(schema, "exclusiveMinimum").map(value -> value + 1.0).orElse(1.0));
    double upper =
        Math.min(
            numericMember(schema, "maximum").orElse(lower + 10.0),
            numericMember(schema, "exclusiveMaximum")
                .map(value -> value - 1.0)
                .orElse(lower + 10.0));
    return Math.min(Math.max(lower, 1.0), upper);
  }

  private static boolean booleanSample(SchemaSyntaxValue schema) {
    Optional<Object> literal = preferredLiteral(schema, "boolean");
    return literal.map(Boolean.class::cast).orElse(true);
  }

  private static Optional<Object> preferredLiteral(SchemaSyntaxValue schema, String type) {
    Optional<SchemaSyntaxValue> constValue = member(schema, "const");
    if (constValue.isPresent()) {
      return javaLiteral(constValue.orElseThrow(), type);
    }
    Optional<SchemaSyntaxValue> defaultValue = member(schema, "default");
    if (defaultValue.isPresent()) {
      Optional<Object> javaDefault = javaLiteral(defaultValue.orElseThrow(), type);
      if (javaDefault.isPresent()) {
        return javaDefault;
      }
    }
    Optional<SchemaSyntaxValue> enumValue = member(schema, "enum");
    if (enumValue.orElse(null) instanceof ArrayValue array) {
      for (SchemaSyntaxValue item : array.items()) {
        Optional<Object> javaValue = javaLiteral(item, type);
        if (javaValue.isPresent()) {
          return javaValue;
        }
      }
    }
    return Optional.empty();
  }

  private static Optional<Object> javaLiteral(SchemaSyntaxValue value, String type) {
    return switch (type) {
      case "string" ->
          value instanceof StringValue stringValue
              ? Optional.of(stringValue.value())
              : Optional.empty();
      case "integer" ->
          value instanceof NumberValue numberValue && numberValue.literal().matches("-?[0-9]+")
              ? Optional.of(Long.parseLong(numberValue.literal()))
              : Optional.empty();
      case "number" ->
          value instanceof NumberValue numberValue
              ? Optional.of(Double.parseDouble(numberValue.literal()))
              : Optional.empty();
      case "boolean" ->
          value instanceof BooleanValue booleanValue
              ? Optional.of(booleanValue.value())
              : Optional.empty();
      default -> Optional.empty();
    };
  }

  private static Optional<String> stringMember(SchemaSyntaxValue schema, String name) {
    return member(schema, name)
        .filter(StringValue.class::isInstance)
        .map(StringValue.class::cast)
        .map(StringValue::value);
  }

  private static Optional<Integer> integerMember(SchemaSyntaxValue schema, String name) {
    return member(schema, name)
        .filter(NumberValue.class::isInstance)
        .map(NumberValue.class::cast)
        .map(NumberValue::literal)
        .filter(literal -> literal.matches("[0-9]+"))
        .map(Integer::parseInt);
  }

  private static Optional<Double> numericMember(SchemaSyntaxValue schema, String name) {
    return member(schema, name)
        .filter(NumberValue.class::isInstance)
        .map(NumberValue.class::cast)
        .map(NumberValue::literal)
        .map(Double::parseDouble);
  }

  private static String innerType(String javaType) {
    int start = javaType.indexOf('<');
    int end = javaType.lastIndexOf('>');
    if (start < 0 || end < start) {
      return javaType;
    }
    return javaType.substring(start + 1, end);
  }

  private static String jsonValueForJavaType(String javaType, SchemaSyntaxValue schema) {
    if (javaType.startsWith("Optional<") || javaType.startsWith("JsonField<")) {
      return jsonValueForJavaType(innerType(javaType), schema);
    }
    if (javaType.startsWith("List<")) {
      ArrayList<String> values = new ArrayList<>();
      SchemaSyntaxValue itemSchema = itemsSchema(schema);
      for (int index = 0; index < arraySampleSize(schema); index++) {
        values.add(jsonValueForJavaType(innerType(javaType), itemSchema));
      }
      return "[" + String.join(",", values) + "]";
    }
    return switch (javaType) {
      case "String" -> "\"" + escapeJson(stringSample(schema)) + "\"";
      case "long", "Long" -> Long.toString(longSample(schema));
      case "double", "Double" -> Double.toString(doubleSample(schema));
      case "boolean", "Boolean" -> Boolean.toString(booleanSample(schema));
      default -> throw new IllegalArgumentException("Unsupported generated Java type " + javaType);
    };
  }

  private static String sha256(byte[] bytes) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(bytes);
      StringBuilder builder = new StringBuilder();
      for (byte value : hashed) {
        builder.append("%02x".formatted(value));
      }
      return builder.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static String escapeJson(String value) {
    StringBuilder builder = new StringBuilder();
    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
      switch (character) {
        case '"' -> builder.append("\\\"");
        case '\\' -> builder.append("\\\\");
        case '\b' -> builder.append("\\b");
        case '\f' -> builder.append("\\f");
        case '\n' -> builder.append("\\n");
        case '\r' -> builder.append("\\r");
        case '\t' -> builder.append("\\t");
        default -> {
          if (character < 0x20) {
            builder.append("\\u%04x".formatted((int) character));
          } else {
            builder.append(character);
          }
        }
      }
    }
    return builder.toString();
  }

  private static String escapeMarkdown(String value) {
    return value.replace("|", "\\|");
  }

  private enum ExpectedOutcome {
    GENERATES,
    REJECTS
  }

  private enum ExpectedDiagnostic {
    NONE,
    PARSE_DIAGNOSTIC,
    PROFILE_DIAGNOSTIC,
    GENERATOR_DIAGNOSTIC
  }

  private enum ActualOutcome {
    GENERATED,
    REJECTED_PARSE,
    REJECTED_PROFILE,
    REJECTED_GENERATOR,
    DIGEST_MISMATCH
  }

  private record ManifestEntry(
      String name,
      String url,
      String sha256,
      ExpectedOutcome expectedOutcome,
      ExpectedDiagnostic expectedDiagnostic) {
    ManifestEntry {
      Objects.requireNonNull(name, "name");
      Objects.requireNonNull(url, "url");
      Objects.requireNonNull(sha256, "sha256");
      Objects.requireNonNull(expectedOutcome, "expectedOutcome");
      Objects.requireNonNull(expectedDiagnostic, "expectedDiagnostic");
    }

    String slug() {
      String slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
      return slug.replaceAll("(^-+|-+$)", "");
    }
  }

  private record DownloadedSchema(String selectedSource, String sha256) {
    DownloadedSchema {
      Objects.requireNonNull(selectedSource, "selectedSource");
      Objects.requireNonNull(sha256, "sha256");
    }
  }

  private record AggregateCounts(
      long totalManifestEntries,
      long generated,
      long expectedProfileRejections,
      long expectedGeneratorRejections,
      long digestDriftFailures,
      long unexpectedFailures) {}

  private record CorpusResult(
      ManifestEntry entry, ActualOutcome actual, String diagnostic, String actualSha256) {
    CorpusResult {
      Objects.requireNonNull(entry, "entry");
      Objects.requireNonNull(actual, "actual");
      Objects.requireNonNull(diagnostic, "diagnostic");
      Objects.requireNonNull(actualSha256, "actualSha256");
    }

    static CorpusResult generated(ManifestEntry entry) {
      return new CorpusResult(entry, ActualOutcome.GENERATED, "NONE", entry.sha256());
    }

    static CorpusResult diagnostic(ManifestEntry entry, ActualOutcome actual, String diagnostic) {
      return new CorpusResult(entry, actual, diagnostic, entry.sha256());
    }

    static CorpusResult digestMismatch(ManifestEntry entry, String actualSha256) {
      return new CorpusResult(
          entry, ActualOutcome.DIGEST_MISMATCH, "DIGEST_MISMATCH", actualSha256);
    }

    boolean accepted(boolean refreshMode) {
      if (actual == ActualOutcome.DIGEST_MISMATCH) {
        return refreshMode;
      }
      if (entry.expectedOutcome() == ExpectedOutcome.GENERATES) {
        return actual == ActualOutcome.GENERATED;
      }
      return switch (entry.expectedDiagnostic()) {
        case PARSE_DIAGNOSTIC -> actual == ActualOutcome.REJECTED_PARSE;
        case PROFILE_DIAGNOSTIC -> actual == ActualOutcome.REJECTED_PROFILE;
        case GENERATOR_DIAGNOSTIC -> actual == ActualOutcome.REJECTED_GENERATOR;
        case NONE -> false;
      };
    }

    String failureMessage() {
      return entry.name()
          + " expected "
          + entry.expectedOutcome()
          + "/"
          + entry.expectedDiagnostic()
          + " but got "
          + actual
          + "/"
          + diagnostic
          + " at "
          + entry.url();
    }
  }

  private record GeneratedSample(
      Object value,
      SchemaObjectMetadata objectMetadata,
      String tag,
      Map<String, String> jsonValues) {
    GeneratedSample {
      Objects.requireNonNull(value, "value");
      Objects.requireNonNull(objectMetadata, "objectMetadata");
      jsonValues = Map.copyOf(Objects.requireNonNull(jsonValues, "jsonValues"));
    }

    Optional<SchemaPropertyMetadata> firstProperty() {
      return objectMetadata.properties().stream().findFirst();
    }

    Optional<SchemaPropertyMetadata> firstRequiredProperty() {
      return objectMetadata.properties().stream()
          .filter(SchemaPropertyMetadata::required)
          .findFirst();
    }

    Optional<SchemaPropertyMetadata> firstScalarProperty() {
      return objectMetadata.properties().stream()
          .filter(property -> !property.array())
          .filter(
              property ->
                  property.javaType().contains("String")
                      || property.javaType().contains("Boolean")
                      || property.javaType().equals("boolean")
                      || property.javaType().contains("Long")
                      || property.javaType().equals("long")
                      || property.javaType().contains("Double")
                      || property.javaType().equals("double"))
          .findFirst();
    }

    String jsonValue(SchemaPropertyMetadata property) {
      String jsonValue = jsonValues.get(property.jsonName());
      if (jsonValue == null) {
        throw new IllegalArgumentException(
            "Missing generated JSON value for " + property.jsonName());
      }
      return jsonValue;
    }
  }

  private record ConstructedRecord(Object value, Map<String, String> jsonValues) {
    ConstructedRecord {
      Objects.requireNonNull(value, "value");
      jsonValues = Map.copyOf(Objects.requireNonNull(jsonValues, "jsonValues"));
    }
  }

  @FunctionalInterface
  private interface ThrowingRunnable {
    void run() throws InvocationTargetException, ReflectiveOperationException;
  }
}
