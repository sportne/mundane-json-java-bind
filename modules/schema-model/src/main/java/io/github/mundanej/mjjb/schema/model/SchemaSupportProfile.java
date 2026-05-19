package io.github.mundanej.mjjb.schema.model;

import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.ArrayValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.BooleanValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.Member;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.NumberValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.ObjectValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.StringValue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** v1 schema support profile helpers. */
public final class SchemaSupportProfile {
  public static final String UNSUPPORTED_KEYWORD_CODE = "MJJBG-SCHEMA-UNSUPPORTED-KEYWORD";
  public static final String INVALID_KEYWORD_VALUE_CODE = "MJJBG-SCHEMA-INVALID-KEYWORD-VALUE";
  public static final String UNSUPPORTED_KEYWORD_VALUE_CODE =
      "MJJBG-SCHEMA-UNSUPPORTED-KEYWORD-VALUE";

  private static final Set<String> TYPE_NAMES =
      Set.of("null", "boolean", "object", "array", "number", "string", "integer");
  private static final Set<String> NON_NULL_TYPE_NAMES =
      Set.of("boolean", "object", "array", "number", "string", "integer");
  private static final Set<String> SUPPORTED_FORMATS = Set.of("date", "date-time", "uuid");

  private SchemaSupportProfile() {}

  public static boolean supportsKeyword(String keyword) {
    Objects.requireNonNull(keyword, "keyword");
    return JsonSchemaKeyword.supportedKeywordName(keyword);
  }

  public static boolean acceptsKeyword(String keyword) {
    Objects.requireNonNull(keyword, "keyword");
    return JsonSchemaKeyword.acceptedKeywordName(keyword);
  }

  public static List<SchemaSupportDiagnostic> validate(SchemaSyntaxValue root) {
    Objects.requireNonNull(root, "root");
    ArrayList<SchemaSupportDiagnostic> diagnostics = new ArrayList<>();
    validateSchema(root, diagnostics);
    diagnostics.sort(
        Comparator.comparing((SchemaSupportDiagnostic diagnostic) -> diagnostic.pointer().value())
            .thenComparing(SchemaSupportDiagnostic::code)
            .thenComparing(SchemaSupportDiagnostic::message));
    return List.copyOf(diagnostics);
  }

  public static SchemaSupportDiagnostic unsupportedKeyword(String keyword, JsonPointer pointer) {
    Objects.requireNonNull(keyword, "keyword");
    Objects.requireNonNull(pointer, "pointer");
    return new SchemaSupportDiagnostic(
        UNSUPPORTED_KEYWORD_CODE,
        "JSON Schema Draft 2020-12 keyword '" + keyword + "' is not supported by JSP-DATA-2020-12.",
        pointer);
  }

  public static List<String> truthDocuments() {
    return List.of(
        "https://json-schema.org/draft/2020-12/json-schema-core.html",
        "https://json-schema.org/draft/2020-12/json-schema-validation",
        "https://json-schema.org/draft/2020-12/schema",
        "https://github.com/json-schema-org/JSON-Schema-Test-Suite");
  }

  private static void validateSchema(
      SchemaSyntaxValue schema, List<SchemaSupportDiagnostic> diagnostics) {
    if (schema instanceof BooleanValue) {
      diagnostics.add(
          unsupportedValue(
              "Boolean schemas are valid JSON Schema but are not supported by JSP-DATA-2020-12.",
              schema.pointer()));
      return;
    }
    if (!(schema instanceof ObjectValue object)) {
      diagnostics.add(
          invalidValue(
              "A JSON Schema resource must be an object or boolean schema.", schema.pointer()));
      return;
    }
    for (Member member : object.members()) {
      JsonSchemaKeyword.fromKeyword(member.name())
          .ifPresentOrElse(
              keyword -> validateKnownKeyword(keyword, member, diagnostics),
              () -> {
                // Unknown extension keywords are annotations in JSON Schema Core.
              });
    }
  }

  private static void validateKnownKeyword(
      JsonSchemaKeyword keyword, Member member, List<SchemaSupportDiagnostic> diagnostics) {
    if (keyword.support() == JsonSchemaKeywordSupport.UNSUPPORTED) {
      diagnostics.add(unsupportedKeyword(keyword.keyword(), member.pointer()));
      return;
    }
    if (keyword.support() == JsonSchemaKeywordSupport.IGNORED_ANNOTATION) {
      return;
    }
    switch (keyword) {
      case TYPE -> validateType(member.value(), diagnostics);
      case PROPERTIES -> validateProperties(member.value(), diagnostics);
      case REQUIRED -> validateRequired(member.value(), diagnostics);
      case ADDITIONAL_PROPERTIES -> validateAdditionalProperties(member.value(), diagnostics);
      case ENUM ->
          requireArray(member.value(), "The 'enum' keyword value must be an array.", diagnostics);
      case ITEMS -> validateItems(member.value(), diagnostics);
      case MIN_ITEMS, MAX_ITEMS, MIN_LENGTH, MAX_LENGTH ->
          requireNonNegativeInteger(member.value(), keyword.keyword(), diagnostics);
      case MINIMUM, MAXIMUM, EXCLUSIVE_MINIMUM, EXCLUSIVE_MAXIMUM ->
          requireNumber(member.value(), keyword.keyword(), diagnostics);
      case PATTERN -> validatePattern(member.value(), diagnostics);
      case FORMAT -> validateFormat(member.value(), diagnostics);
      case ONE_OF -> validateOneOf(member.value(), diagnostics);
      case CONST, DEFAULT -> {
        // These keywords accept any JSON value.
      }
      default -> throw new IllegalStateException("unhandled binding keyword " + keyword.keyword());
    }
  }

  private static void validateType(
      SchemaSyntaxValue value, List<SchemaSupportDiagnostic> diagnostics) {
    if (value instanceof StringValue stringValue) {
      if (!TYPE_NAMES.contains(stringValue.value())) {
        diagnostics.add(
            invalidValue(
                "The 'type' keyword value must name a JSON Schema primitive type.",
                value.pointer()));
      }
      return;
    }
    if (value instanceof ArrayValue arrayValue) {
      validateTypeArray(arrayValue, diagnostics);
      return;
    }
    diagnostics.add(
        invalidValue(
            "The 'type' keyword value must be a string or array of strings.", value.pointer()));
  }

  private static void validateTypeArray(
      ArrayValue arrayValue, List<SchemaSupportDiagnostic> diagnostics) {
    ArrayList<String> names = new ArrayList<>();
    for (SchemaSyntaxValue item : arrayValue.items()) {
      if (!(item instanceof StringValue stringItem)) {
        diagnostics.add(invalidValue("Every 'type' array item must be a string.", item.pointer()));
        return;
      }
      if (!TYPE_NAMES.contains(stringItem.value())) {
        diagnostics.add(
            invalidValue(
                "Every 'type' array item must name a JSON Schema primitive type.", item.pointer()));
        return;
      }
      if (names.contains(stringItem.value())) {
        diagnostics.add(
            invalidValue("The 'type' array must contain unique type names.", item.pointer()));
        return;
      }
      names.add(stringItem.value());
    }
    boolean nullablePair =
        names.size() == 2
            && names.contains("null")
            && names.stream().anyMatch(NON_NULL_TYPE_NAMES::contains);
    if (!nullablePair) {
      diagnostics.add(
          unsupportedValue(
              "JSP-DATA-2020-12 supports 'type' arrays only for one non-null type plus 'null'.",
              arrayValue.pointer()));
    }
  }

  private static void validateProperties(
      SchemaSyntaxValue value, List<SchemaSupportDiagnostic> diagnostics) {
    if (!(value instanceof ObjectValue properties)) {
      diagnostics.add(
          invalidValue("The 'properties' keyword value must be an object.", value.pointer()));
      return;
    }
    for (Member property : properties.members()) {
      validateSchemaObjectOnly(
          property.value(), "Property schemas must be schema objects.", diagnostics);
    }
  }

  private static void validateRequired(
      SchemaSyntaxValue value, List<SchemaSupportDiagnostic> diagnostics) {
    if (!(value instanceof ArrayValue required)) {
      diagnostics.add(
          invalidValue("The 'required' keyword value must be an array.", value.pointer()));
      return;
    }
    ArrayList<String> names = new ArrayList<>();
    for (SchemaSyntaxValue item : required.items()) {
      if (!(item instanceof StringValue stringItem)) {
        diagnostics.add(
            invalidValue("Every 'required' array item must be a string.", item.pointer()));
        return;
      }
      if (names.contains(stringItem.value())) {
        diagnostics.add(
            invalidValue(
                "The 'required' array must contain unique property names.", item.pointer()));
        return;
      }
      names.add(stringItem.value());
    }
  }

  private static void validateAdditionalProperties(
      SchemaSyntaxValue value, List<SchemaSupportDiagnostic> diagnostics) {
    if (value instanceof BooleanValue booleanValue && !booleanValue.value()) {
      return;
    }
    if (value instanceof BooleanValue || value instanceof ObjectValue) {
      diagnostics.add(
          unsupportedValue(
              "JSP-DATA-2020-12 supports 'additionalProperties' only as literal false.",
              value.pointer()));
      return;
    }
    diagnostics.add(
        invalidValue(
            "The 'additionalProperties' keyword value must be a JSON Schema.", value.pointer()));
  }

  private static void validateItems(
      SchemaSyntaxValue value, List<SchemaSupportDiagnostic> diagnostics) {
    validateSchemaObjectOnly(
        value, "The 'items' keyword value must be a schema object.", diagnostics);
  }

  private static void validateOneOf(
      SchemaSyntaxValue value, List<SchemaSupportDiagnostic> diagnostics) {
    if (!(value instanceof ArrayValue arrayValue)) {
      diagnostics.add(
          invalidValue("The 'oneOf' keyword value must be a non-empty array.", value.pointer()));
      return;
    }
    if (arrayValue.items().isEmpty()) {
      diagnostics.add(
          invalidValue("The 'oneOf' keyword value must be a non-empty array.", value.pointer()));
      return;
    }
    for (SchemaSyntaxValue item : arrayValue.items()) {
      if (!(item instanceof ObjectValue || item instanceof BooleanValue)) {
        diagnostics.add(
            invalidValue("Every 'oneOf' array item must be a JSON Schema.", item.pointer()));
        return;
      }
    }
    diagnostics.add(
        unsupportedValue(
            "Tagged 'oneOf' is part of the profile scope but is implemented in TASK-0020.",
            value.pointer()));
  }

  private static void validateSchemaObjectOnly(
      SchemaSyntaxValue value, String message, List<SchemaSupportDiagnostic> diagnostics) {
    if (value instanceof ObjectValue) {
      validateSchema(value, diagnostics);
      return;
    }
    if (value instanceof BooleanValue) {
      diagnostics.add(
          unsupportedValue(
              "Boolean schemas are valid JSON Schema but are not supported by JSP-DATA-2020-12.",
              value.pointer()));
      return;
    }
    diagnostics.add(invalidValue(message, value.pointer()));
  }

  private static void requireArray(
      SchemaSyntaxValue value, String message, List<SchemaSupportDiagnostic> diagnostics) {
    if (!(value instanceof ArrayValue)) {
      diagnostics.add(invalidValue(message, value.pointer()));
    }
  }

  private static void validateFormat(
      SchemaSyntaxValue value, List<SchemaSupportDiagnostic> diagnostics) {
    if (!(value instanceof StringValue stringValue)) {
      diagnostics.add(
          invalidValue("The 'format' keyword value must be a string.", value.pointer()));
      return;
    }
    if (!SUPPORTED_FORMATS.contains(stringValue.value())) {
      diagnostics.add(
          unsupportedValue(
              "JSP-DATA-2020-12 supports only date, date-time, and uuid formats.",
              value.pointer()));
    }
  }

  private static void validatePattern(
      SchemaSyntaxValue value, List<SchemaSupportDiagnostic> diagnostics) {
    if (!(value instanceof StringValue stringValue)) {
      diagnostics.add(
          invalidValue("The 'pattern' keyword value must be a string.", value.pointer()));
      return;
    }
    try {
      Pattern.compile(stringValue.value());
    } catch (PatternSyntaxException exception) {
      diagnostics.add(
          invalidValue(
              "The 'pattern' keyword value must compile as a deterministic v1 regular expression.",
              value.pointer()));
    }
  }

  private static void requireNumber(
      SchemaSyntaxValue value, String keyword, List<SchemaSupportDiagnostic> diagnostics) {
    if (!(value instanceof NumberValue)) {
      diagnostics.add(
          invalidValue("The '" + keyword + "' keyword value must be a number.", value.pointer()));
    }
  }

  private static void requireNonNegativeInteger(
      SchemaSyntaxValue value, String keyword, List<SchemaSupportDiagnostic> diagnostics) {
    if (!(value instanceof NumberValue numberValue)
        || !isNonNegativeIntegerLiteral(numberValue.literal())) {
      diagnostics.add(
          invalidValue(
              "The '" + keyword + "' keyword value must be a non-negative integer.",
              value.pointer()));
    }
  }

  private static boolean isNonNegativeIntegerLiteral(String literal) {
    for (int index = 0; index < literal.length(); index++) {
      char current = literal.charAt(index);
      if (current < '0' || current > '9') {
        return false;
      }
    }
    return true;
  }

  private static SchemaSupportDiagnostic invalidValue(String message, JsonPointer pointer) {
    return new SchemaSupportDiagnostic(INVALID_KEYWORD_VALUE_CODE, message, pointer);
  }

  private static SchemaSupportDiagnostic unsupportedValue(String message, JsonPointer pointer) {
    return new SchemaSupportDiagnostic(UNSUPPORTED_KEYWORD_VALUE_CODE, message, pointer);
  }
}
