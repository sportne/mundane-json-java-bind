package io.github.mundanej.mjjb.generator.core;

import io.github.mundanej.mjjb.generator.api.Generator;
import io.github.mundanej.mjjb.generator.api.GeneratorDiagnostic;
import io.github.mundanej.mjjb.generator.api.GeneratorRequest;
import io.github.mundanej.mjjb.generator.api.GeneratorResult;
import io.github.mundanej.mjjb.schema.model.SchemaSupportDiagnostic;
import io.github.mundanej.mjjb.schema.model.SchemaSupportProfile;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxDiagnostic;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxParseResult;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Initial deterministic generator entry point. */
public final class CoreGenerator implements Generator {
  @Override
  public GeneratorResult generate(GeneratorRequest request) {
    Objects.requireNonNull(request, "request");
    ArrayList<GeneratorDiagnostic> diagnostics = new ArrayList<>();
    if (request.schemaPaths().isEmpty()) {
      diagnostics.add(
          new GeneratorDiagnostic(
              "MJJBG-GEN-001", "At least one JSON Schema input is required.", null, ""));
      return GeneratorResult.failure(diagnostics);
    }
    for (Path schemaPath : request.schemaPaths()) {
      diagnostics.addAll(validateSchemaPath(schemaPath));
    }
    if (!diagnostics.isEmpty()) {
      return GeneratorResult.failure(diagnostics);
    }
    try {
      Files.createDirectories(request.outputDirectory());
      Path source = writeScaffoldSource(request);
      return GeneratorResult.success(List.of(source));
    } catch (IOException exception) {
      return GeneratorResult.failure(
          List.of(
              new GeneratorDiagnostic(
                  "MJJBG-GEN-002",
                  "Unable to write generated source: " + exception.getMessage(),
                  null,
                  "")));
    }
  }

  private List<GeneratorDiagnostic> validateSchemaPath(Path schemaPath) {
    Objects.requireNonNull(schemaPath, "schemaPath");
    ArrayList<GeneratorDiagnostic> diagnostics = new ArrayList<>();
    if (!Files.isRegularFile(schemaPath)) {
      diagnostics.add(
          new GeneratorDiagnostic(
              "MJJBG-GEN-003", "JSON Schema input does not exist.", schemaPath, ""));
      return diagnostics;
    }
    try {
      String source = Files.readString(schemaPath);
      SchemaSyntaxParseResult parseResult = SchemaSyntaxParser.parse(source);
      if (!parseResult.diagnostics().isEmpty()) {
        diagnostics.addAll(toGeneratorDiagnostics(parseResult.diagnostics(), schemaPath));
        return diagnostics;
      }
      diagnostics.addAll(
          toGeneratorSupportDiagnostics(
              SchemaSupportProfile.validate(parseResult.root()), schemaPath));
    } catch (IOException exception) {
      diagnostics.add(
          new GeneratorDiagnostic(
              "MJJBG-GEN-004",
              "Unable to read JSON Schema input: " + exception.getMessage(),
              schemaPath,
              ""));
    }
    diagnostics.sort(Comparator.comparing(GeneratorDiagnostic::toManifestLine));
    return diagnostics;
  }

  private List<GeneratorDiagnostic> toGeneratorSupportDiagnostics(
      List<SchemaSupportDiagnostic> schemaDiagnostics, Path schemaPath) {
    ArrayList<GeneratorDiagnostic> diagnostics = new ArrayList<>();
    for (SchemaSupportDiagnostic diagnostic : schemaDiagnostics) {
      diagnostics.add(toGeneratorDiagnostic(diagnostic, schemaPath));
    }
    return diagnostics;
  }

  private List<GeneratorDiagnostic> toGeneratorDiagnostics(
      List<SchemaSyntaxDiagnostic> schemaDiagnostics, Path schemaPath) {
    ArrayList<GeneratorDiagnostic> diagnostics = new ArrayList<>();
    for (SchemaSyntaxDiagnostic diagnostic : schemaDiagnostics) {
      diagnostics.add(
          new GeneratorDiagnostic(
              diagnostic.code(), diagnostic.message(), schemaPath, diagnostic.pointer().value()));
    }
    return diagnostics;
  }

  private GeneratorDiagnostic toGeneratorDiagnostic(
      SchemaSupportDiagnostic diagnostic, Path schemaPath) {
    return new GeneratorDiagnostic(
        diagnostic.code(), diagnostic.message(), schemaPath, diagnostic.pointer().value());
  }

  private Path writeScaffoldSource(GeneratorRequest request) throws IOException {
    String packageName = request.defaultPackage();
    Path packageDirectory = request.outputDirectory().resolve(packageName.replace('.', '/'));
    Files.createDirectories(packageDirectory);
    Path source = packageDirectory.resolve("GeneratedBindings.java");
    String content =
        String.join(
            System.lineSeparator(),
            "package " + packageName + ";",
            "",
            "/** Marker for generated JSON Schema bindings. */",
            "public final class GeneratedBindings {",
            "  private GeneratedBindings() {}",
            "}",
            "");
    Files.writeString(source, content);
    return source;
  }
}
