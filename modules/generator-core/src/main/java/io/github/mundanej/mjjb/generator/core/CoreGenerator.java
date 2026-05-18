package io.github.mundanej.mjjb.generator.core;

import io.github.mundanej.mjjb.generator.api.Generator;
import io.github.mundanej.mjjb.generator.api.GeneratorDiagnostic;
import io.github.mundanej.mjjb.generator.api.GeneratorRequest;
import io.github.mundanej.mjjb.generator.api.GeneratorResult;
import io.github.mundanej.mjjb.schema.model.JsonPointer;
import io.github.mundanej.mjjb.schema.model.JsonSchemaKeyword;
import io.github.mundanej.mjjb.schema.model.SchemaSupportProfile;
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
      SchemaKeywordScan scan = SchemaKeywordScanner.scan(source);
      if (!scan.diagnostics().isEmpty()) {
        for (SchemaScanDiagnostic diagnostic : scan.diagnostics()) {
          diagnostics.add(
              new GeneratorDiagnostic(
                  diagnostic.code(),
                  diagnostic.message(),
                  schemaPath,
                  diagnostic.pointer().value()));
        }
      }
      for (KeywordOccurrence occurrence : scan.keywords()) {
        JsonSchemaKeyword.fromKeyword(occurrence.keyword())
            .filter(keyword -> !keyword.supportedInV1())
            .ifPresent(
                keyword ->
                    diagnostics.add(
                        toGeneratorDiagnostic(
                            SchemaSupportProfile.unsupportedKeyword(
                                keyword.keyword(), occurrence.pointer()),
                            schemaPath)));
      }
      if (!scan.diagnostics().isEmpty()) {
        return diagnostics;
      }
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

  private GeneratorDiagnostic toGeneratorDiagnostic(
      io.github.mundanej.mjjb.schema.model.SchemaSupportDiagnostic diagnostic, Path schemaPath) {
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

  private record KeywordOccurrence(String keyword, JsonPointer pointer) {}

  private record SchemaScanDiagnostic(String code, String message, JsonPointer pointer) {}

  private record SchemaKeywordScan(
      List<KeywordOccurrence> keywords, List<SchemaScanDiagnostic> diagnostics) {}

  private static final class SchemaKeywordScanner {
    private final String source;
    private final ArrayList<KeywordOccurrence> keywords = new ArrayList<>();
    private final ArrayList<SchemaScanDiagnostic> diagnostics = new ArrayList<>();
    private int index;

    private SchemaKeywordScanner(String source) {
      this.source = Objects.requireNonNull(source, "source");
    }

    private static SchemaKeywordScan scan(String source) {
      SchemaKeywordScanner scanner = new SchemaKeywordScanner(source);
      scanner.parse();
      return new SchemaKeywordScan(List.copyOf(scanner.keywords), List.copyOf(scanner.diagnostics));
    }

    private void parse() {
      skipWhitespace();
      parseValue(JsonPointer.ROOT);
      skipWhitespace();
      if (diagnostics.isEmpty() && index < source.length()) {
        addError(
            "MJJBG-SCHEMA-INVALID-JSON",
            "Unexpected content after JSON Schema document.",
            JsonPointer.ROOT);
      }
    }

    private void parseValue(JsonPointer pointer) {
      skipWhitespace();
      if (diagnosticsPresent() || index >= source.length()) {
        addError("MJJBG-SCHEMA-INVALID-JSON", "Unexpected end of JSON Schema document.", pointer);
        return;
      }
      char current = source.charAt(index);
      switch (current) {
        case '{' -> parseObject(pointer);
        case '[' -> parseArray(pointer);
        case '"' -> parseString(pointer);
        case 't' -> parseLiteral("true", pointer);
        case 'f' -> parseLiteral("false", pointer);
        case 'n' -> parseLiteral("null", pointer);
        default -> {
          if (current == '-' || Character.isDigit(current)) {
            parseNumber(pointer);
          } else {
            addError("MJJBG-SCHEMA-INVALID-JSON", "Unexpected JSON token in schema.", pointer);
          }
        }
      }
    }

    private void parseObject(JsonPointer pointer) {
      index++;
      skipWhitespace();
      if (consumeIf('}')) {
        return;
      }
      boolean first = true;
      while (!diagnosticsPresent()) {
        if (!first && !consumeIf(',')) {
          addError(
              "MJJBG-SCHEMA-INVALID-JSON", "Expected comma between object properties.", pointer);
          return;
        }
        skipWhitespace();
        if (index >= source.length() || source.charAt(index) != '"') {
          addError("MJJBG-SCHEMA-INVALID-JSON", "Expected object property name.", pointer);
          return;
        }
        String name = parseString(pointer);
        if (diagnosticsPresent()) {
          return;
        }
        JsonPointer propertyPointer = pointer.property(name);
        keywords.add(new KeywordOccurrence(name, propertyPointer));
        skipWhitespace();
        if (!consumeIf(':')) {
          addError(
              "MJJBG-SCHEMA-INVALID-JSON",
              "Expected colon after object property name.",
              propertyPointer);
          return;
        }
        parseValue(propertyPointer);
        skipWhitespace();
        if (consumeIf('}')) {
          return;
        }
        first = false;
      }
    }

    private void parseArray(JsonPointer pointer) {
      index++;
      skipWhitespace();
      if (consumeIf(']')) {
        return;
      }
      int itemIndex = 0;
      boolean first = true;
      while (!diagnosticsPresent()) {
        if (!first && !consumeIf(',')) {
          addError("MJJBG-SCHEMA-INVALID-JSON", "Expected comma between array items.", pointer);
          return;
        }
        parseValue(pointer.index(itemIndex));
        itemIndex++;
        skipWhitespace();
        if (consumeIf(']')) {
          return;
        }
        first = false;
      }
    }

    private String parseString(JsonPointer pointer) {
      index++;
      StringBuilder builder = new StringBuilder();
      while (index < source.length()) {
        char current = source.charAt(index++);
        if (current == '"') {
          return builder.toString();
        }
        if (current == '\\') {
          builder.append(parseEscapedCharacter(pointer));
          if (diagnosticsPresent()) {
            return "";
          }
        } else if (current <= 0x1f) {
          addError(
              "MJJBG-SCHEMA-INVALID-JSON", "Unescaped control character in JSON string.", pointer);
          return "";
        } else {
          builder.append(current);
        }
      }
      addError("MJJBG-SCHEMA-INVALID-JSON", "Unterminated JSON string.", pointer);
      return "";
    }

    private char parseEscapedCharacter(JsonPointer pointer) {
      if (index >= source.length()) {
        addError("MJJBG-SCHEMA-INVALID-JSON", "Unterminated JSON escape.", pointer);
        return '\0';
      }
      char escaped = source.charAt(index++);
      if (escaped != 'u') {
        return switch (escaped) {
          case '"', '\\', '/' -> escaped;
          case 'b' -> '\b';
          case 'f' -> '\f';
          case 'n' -> '\n';
          case 'r' -> '\r';
          case 't' -> '\t';
          default -> {
            addError("MJJBG-SCHEMA-INVALID-JSON", "Invalid JSON escape.", pointer);
            yield '\0';
          }
        };
      }
      if (index + 4 > source.length()) {
        addError("MJJBG-SCHEMA-INVALID-JSON", "Incomplete unicode escape.", pointer);
        return '\0';
      }
      int value = 0;
      for (int i = 0; i < 4; i++) {
        int hex = Character.digit(source.charAt(index++), 16);
        if (hex < 0) {
          addError("MJJBG-SCHEMA-INVALID-JSON", "Invalid unicode escape digit.", pointer);
          return '\0';
        }
        value = (value << 4) + hex;
      }
      return (char) value;
    }

    private void parseNumber(JsonPointer pointer) {
      consumeIf('-');
      if (index >= source.length() || !isDigit(source.charAt(index))) {
        addError("MJJBG-SCHEMA-INVALID-JSON", "Expected digit in JSON number.", pointer);
        return;
      }
      if (consumeIf('0')) {
        if (index < source.length() && isDigit(source.charAt(index))) {
          addError(
              "MJJBG-SCHEMA-INVALID-JSON", "Leading zeroes are not valid JSON numbers.", pointer);
          return;
        }
      } else {
        consumeDigits();
      }
      if (consumeIf('.')) {
        if (index >= source.length() || !isDigit(source.charAt(index))) {
          addError(
              "MJJBG-SCHEMA-INVALID-JSON",
              "Expected digit after decimal point in JSON number.",
              pointer);
          return;
        }
        consumeDigits();
      }
      if (index < source.length() && (source.charAt(index) == 'e' || source.charAt(index) == 'E')) {
        index++;
        if (index < source.length()
            && (source.charAt(index) == '+' || source.charAt(index) == '-')) {
          index++;
        }
        if (index >= source.length() || !isDigit(source.charAt(index))) {
          addError("MJJBG-SCHEMA-INVALID-JSON", "Expected exponent digit in JSON number.", pointer);
          return;
        }
        consumeDigits();
      }
    }

    private void consumeDigits() {
      while (index < source.length() && isDigit(source.charAt(index))) {
        index++;
      }
    }

    private static boolean isDigit(char value) {
      return value >= '0' && value <= '9';
    }

    private void parseLiteral(String literal, JsonPointer pointer) {
      if (!source.startsWith(literal, index)) {
        addError("MJJBG-SCHEMA-INVALID-JSON", "Invalid JSON literal.", pointer);
        return;
      }
      index += literal.length();
    }

    private boolean consumeIf(char expected) {
      if (index < source.length() && source.charAt(index) == expected) {
        index++;
        return true;
      }
      return false;
    }

    private void skipWhitespace() {
      while (index < source.length()) {
        char current = source.charAt(index);
        if (current != ' ' && current != '\n' && current != '\r' && current != '\t') {
          return;
        }
        index++;
      }
    }

    private boolean diagnosticsPresent() {
      return !diagnostics.isEmpty();
    }

    private void addError(String code, String message, JsonPointer pointer) {
      if (diagnostics.isEmpty()) {
        diagnostics.add(new SchemaScanDiagnostic(code, message, pointer));
      }
    }
  }
}
