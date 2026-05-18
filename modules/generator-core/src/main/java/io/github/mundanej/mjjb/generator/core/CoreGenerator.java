package io.github.mundanej.mjjb.generator.core;

import io.github.mundanej.mjjb.generator.api.Generator;
import io.github.mundanej.mjjb.generator.api.GeneratorDiagnostic;
import io.github.mundanej.mjjb.generator.api.GeneratorRequest;
import io.github.mundanej.mjjb.generator.api.GeneratorResult;
import io.github.mundanej.mjjb.generator.core.internal.binding.BindingBuildResult;
import io.github.mundanej.mjjb.generator.core.internal.binding.BindingDiagnostic;
import io.github.mundanej.mjjb.generator.core.internal.binding.BindingModel;
import io.github.mundanej.mjjb.generator.core.internal.binding.BindingModelBuilder;
import io.github.mundanej.mjjb.generator.core.internal.emitter.ModelSourceEmitter;
import io.github.mundanej.mjjb.generator.core.internal.emitter.ReaderSourceEmitter;
import io.github.mundanej.mjjb.generator.core.internal.emitter.WriterSourceEmitter;
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
import java.util.Optional;

/** Initial deterministic generator entry point. */
public final class CoreGenerator implements Generator {
  private static final String ROOT_TYPE_NAME = "GeneratedBindings";

  @Override
  public GeneratorResult generate(GeneratorRequest request) {
    Objects.requireNonNull(request, "request");
    ArrayList<GeneratorDiagnostic> diagnostics = new ArrayList<>();
    ArrayList<BindingModel> models = new ArrayList<>();
    if (request.schemaPaths().isEmpty()) {
      diagnostics.add(
          new GeneratorDiagnostic(
              "MJJBG-GEN-001", "At least one JSON Schema input is required.", null, ""));
      return GeneratorResult.failure(diagnostics);
    }
    for (Path schemaPath : request.schemaPaths()) {
      ValidatedSchema validatedSchema = validateSchemaPath(schemaPath, request.defaultPackage());
      diagnostics.addAll(validatedSchema.diagnostics());
      validatedSchema.model().ifPresent(models::add);
    }
    if (!diagnostics.isEmpty()) {
      return GeneratorResult.failure(diagnostics);
    }
    try {
      Files.createDirectories(request.outputDirectory());
      return GeneratorResult.success(writeSources(request.outputDirectory(), models.getFirst()));
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

  private ValidatedSchema validateSchemaPath(Path schemaPath, String packageName) {
    Objects.requireNonNull(schemaPath, "schemaPath");
    Objects.requireNonNull(packageName, "packageName");
    ArrayList<GeneratorDiagnostic> diagnostics = new ArrayList<>();
    if (!Files.isRegularFile(schemaPath)) {
      diagnostics.add(
          new GeneratorDiagnostic(
              "MJJBG-GEN-003", "JSON Schema input does not exist.", schemaPath, ""));
      return ValidatedSchema.failure(diagnostics);
    }
    try {
      String source = Files.readString(schemaPath);
      SchemaSyntaxParseResult parseResult = SchemaSyntaxParser.parse(source);
      if (!parseResult.diagnostics().isEmpty()) {
        diagnostics.addAll(toGeneratorDiagnostics(parseResult.diagnostics(), schemaPath));
        return ValidatedSchema.failure(diagnostics);
      }
      diagnostics.addAll(
          toGeneratorSupportDiagnostics(
              SchemaSupportProfile.validate(parseResult.root()), schemaPath));
      if (!diagnostics.isEmpty()) {
        return ValidatedSchema.failure(diagnostics);
      }
      BindingBuildResult bindingResult =
          new BindingModelBuilder().build(parseResult.root(), packageName, ROOT_TYPE_NAME);
      diagnostics.addAll(toGeneratorBindingDiagnostics(bindingResult.diagnostics(), schemaPath));
      if (diagnostics.isEmpty()) {
        return ValidatedSchema.success(bindingResult.model().orElseThrow());
      }
    } catch (IOException exception) {
      diagnostics.add(
          new GeneratorDiagnostic(
              "MJJBG-GEN-004",
              "Unable to read JSON Schema input: " + exception.getMessage(),
              schemaPath,
              ""));
    }
    diagnostics.sort(
        Comparator.comparing(GeneratorDiagnostic::schemaPointer)
            .thenComparing(GeneratorDiagnostic::code)
            .thenComparing(GeneratorDiagnostic::message)
            .thenComparing(GeneratorDiagnostic::toManifestLine));
    return ValidatedSchema.failure(diagnostics);
  }

  private List<GeneratorDiagnostic> toGeneratorBindingDiagnostics(
      List<BindingDiagnostic> bindingDiagnostics, Path schemaPath) {
    ArrayList<GeneratorDiagnostic> diagnostics = new ArrayList<>();
    for (BindingDiagnostic diagnostic : bindingDiagnostics) {
      diagnostics.add(
          new GeneratorDiagnostic(
              diagnostic.code(), diagnostic.message(), schemaPath, diagnostic.pointer().value()));
    }
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

  private List<Path> writeSources(Path outputDirectory, BindingModel model) throws IOException {
    Path packageDirectory = outputDirectory.resolve(model.packageName().replace('.', '/'));
    Files.createDirectories(packageDirectory);
    Path modelSource = packageDirectory.resolve(model.rootTypeName() + ".java");
    Files.writeString(modelSource, new ModelSourceEmitter().emit(model));
    Path writerSource =
        packageDirectory.resolve(WriterSourceEmitter.writerTypeName(model) + ".java");
    Files.writeString(writerSource, new WriterSourceEmitter().emit(model));
    Path readerSource =
        packageDirectory.resolve(ReaderSourceEmitter.readerTypeName(model) + ".java");
    Files.writeString(readerSource, new ReaderSourceEmitter().emit(model));
    return List.of(modelSource, writerSource, readerSource);
  }

  private record ValidatedSchema(
      Optional<BindingModel> model, List<GeneratorDiagnostic> diagnostics) {
    private ValidatedSchema {
      Objects.requireNonNull(model, "model");
      diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    private static ValidatedSchema success(BindingModel model) {
      return new ValidatedSchema(Optional.of(model), List.of());
    }

    private static ValidatedSchema failure(List<GeneratorDiagnostic> diagnostics) {
      return new ValidatedSchema(Optional.empty(), diagnostics);
    }
  }
}
