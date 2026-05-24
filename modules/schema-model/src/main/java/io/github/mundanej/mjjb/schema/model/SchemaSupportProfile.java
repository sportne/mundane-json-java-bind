package io.github.mundanej.mjjb.schema.model;

import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.ArrayValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.BooleanValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.Member;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.NumberValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.ObjectValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.StringValue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
      case PATTERN_PROPERTIES -> validatePatternProperties(member.value(), diagnostics);
      case REQUIRED -> validateRequired(member.value(), diagnostics);
      case ADDITIONAL_PROPERTIES -> validateAdditionalProperties(member.value(), diagnostics);
      case ENUM -> validateEnum(member.value(), diagnostics);
      case ITEMS -> validateItems(member.value(), diagnostics);
      case MIN_ITEMS, MAX_ITEMS, MIN_LENGTH, MAX_LENGTH, MIN_PROPERTIES, MAX_PROPERTIES ->
          requireNonNegativeInteger(member.value(), keyword.keyword(), diagnostics);
      case MINIMUM, MAXIMUM, EXCLUSIVE_MINIMUM, EXCLUSIVE_MAXIMUM ->
          requireNumber(member.value(), keyword.keyword(), diagnostics);
      case PATTERN -> validatePattern(member.value(), diagnostics);
      case FORMAT -> validateFormat(member.value(), diagnostics);
      case ONE_OF -> validateOneOf(member.value(), diagnostics);
      case ALL_OF -> validateAllOf(member.value(), diagnostics);
      case REF -> validateRef(member.value(), diagnostics);
      case DEFS -> validateDefs(member.value(), diagnostics);
      case PROPERTY_NAMES -> validatePropertyNames(member.value(), diagnostics);
      case DEPENDENT_REQUIRED -> validateDependentRequired(member.value(), diagnostics);
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

  private static void validatePatternProperties(
      SchemaSyntaxValue value, List<SchemaSupportDiagnostic> diagnostics) {
    if (!(value instanceof ObjectValue patternProperties)) {
      diagnostics.add(
          invalidValue(
              "The 'patternProperties' keyword value must be an object.", value.pointer()));
      return;
    }
    if (patternProperties.members().size() != 1) {
      diagnostics.add(
          unsupportedValue(
              "JSP-DATA-2020-12 supports exactly one patternProperties entry per object.",
              value.pointer()));
      return;
    }
    Member patternMember = patternProperties.members().getFirst();
    try {
      Pattern.compile(patternMember.name());
    } catch (PatternSyntaxException exception) {
      diagnostics.add(
          invalidValue(
              "Every 'patternProperties' member name must be a valid regular expression.",
              patternMember.pointer()));
      return;
    }
    validateSchemaObjectOnly(
        patternMember.value(),
        "patternProperties value schemas must be schema objects.",
        diagnostics);
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
    if (value instanceof ObjectValue objectValue) {
      validateSchema(objectValue, diagnostics);
      return;
    }
    if (value instanceof BooleanValue) {
      diagnostics.add(
          unsupportedValue(
              "JSP-DATA-2020-12 supports 'additionalProperties' only as literal false or a schema object.",
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
    if (!taggedOneOfShape(arrayValue, diagnostics)) {
      diagnostics.add(
          unsupportedValue(
              "JSP-DATA-2020-12 supports oneOf only as root tagged object branches.",
              value.pointer()));
    }
  }

  private static void validateAllOf(
      SchemaSyntaxValue value, List<SchemaSupportDiagnostic> diagnostics) {
    if (!(value instanceof ArrayValue arrayValue)) {
      diagnostics.add(
          invalidValue("The 'allOf' keyword value must be a non-empty array.", value.pointer()));
      return;
    }
    if (arrayValue.items().isEmpty()) {
      diagnostics.add(
          invalidValue("The 'allOf' keyword value must be a non-empty array.", value.pointer()));
      return;
    }
    for (SchemaSyntaxValue item : arrayValue.items()) {
      validateSchemaObjectOnly(
          item, "Every 'allOf' array item must be a JSON Schema.", diagnostics);
    }
  }

  private static void validateRef(
      SchemaSyntaxValue value, List<SchemaSupportDiagnostic> diagnostics) {
    if (!(value instanceof StringValue)) {
      diagnostics.add(invalidValue("The '$ref' keyword value must be a string.", value.pointer()));
    }
  }

  private static void validateDefs(
      SchemaSyntaxValue value, List<SchemaSupportDiagnostic> diagnostics) {
    if (!(value instanceof ObjectValue defs)) {
      diagnostics.add(
          invalidValue("The '$defs' keyword value must be an object.", value.pointer()));
      return;
    }
    for (Member member : defs.members()) {
      validateSchemaObjectOnly(
          member.value(), "Every '$defs' member value must be a JSON Schema.", diagnostics);
    }
  }

  private static void validatePropertyNames(
      SchemaSyntaxValue value, List<SchemaSupportDiagnostic> diagnostics) {
    if (value instanceof BooleanValue) {
      diagnostics.add(
          unsupportedValue(
              "Boolean schemas are valid JSON Schema but are not supported for 'propertyNames'.",
              value.pointer()));
      return;
    }
    if (!(value instanceof ObjectValue propertyNames)) {
      diagnostics.add(
          invalidValue(
              "The 'propertyNames' keyword value must be a JSON Schema.", value.pointer()));
      return;
    }
    for (Member member : propertyNames.members()) {
      Optional<JsonSchemaKeyword> keyword = JsonSchemaKeyword.fromKeyword(member.name());
      if (keyword.isEmpty()) {
        continue;
      }
      if (keyword.get().support() == JsonSchemaKeywordSupport.IGNORED_ANNOTATION
          || keyword.get() == JsonSchemaKeyword.DEFAULT) {
        continue;
      }
      switch (keyword.get()) {
        case TYPE -> validatePropertyNamesType(member.value(), diagnostics);
        case MIN_LENGTH, MAX_LENGTH ->
            requireNonNegativeInteger(member.value(), keyword.get().keyword(), diagnostics);
        case PATTERN -> validatePattern(member.value(), diagnostics);
        case FORMAT -> validateFormat(member.value(), diagnostics);
        default ->
            diagnostics.add(
                unsupportedValue(
                    "JSP-DATA-2020-12 supports 'propertyNames' only with string assertion keywords.",
                    member.pointer()));
      }
    }
  }

  private static void validatePropertyNamesType(
      SchemaSyntaxValue value, List<SchemaSupportDiagnostic> diagnostics) {
    if (!(value instanceof StringValue stringValue)) {
      diagnostics.add(
          invalidValue(
              "The 'propertyNames/type' keyword value must be the string value 'string'.",
              value.pointer()));
      return;
    }
    if (!"string".equals(stringValue.value())) {
      diagnostics.add(
          unsupportedValue(
              "JSP-DATA-2020-12 supports 'propertyNames/type' only as 'string'.", value.pointer()));
    }
  }

  private static void validateDependentRequired(
      SchemaSyntaxValue value, List<SchemaSupportDiagnostic> diagnostics) {
    if (!(value instanceof ObjectValue dependencies)) {
      diagnostics.add(
          invalidValue(
              "The 'dependentRequired' keyword value must be an object.", value.pointer()));
      return;
    }
    for (Member dependency : dependencies.members()) {
      if (!(dependency.value() instanceof ArrayValue required)) {
        diagnostics.add(
            invalidValue(
                "Every 'dependentRequired' member value must be an array.", dependency.pointer()));
        continue;
      }
      HashSet<String> names = new HashSet<>();
      for (SchemaSyntaxValue item : required.items()) {
        if (!(item instanceof StringValue stringItem)) {
          diagnostics.add(
              invalidValue(
                  "Every 'dependentRequired' array item must be a string.", item.pointer()));
          break;
        }
        if (!names.add(stringItem.value())) {
          diagnostics.add(
              invalidValue(
                  "Every 'dependentRequired' array must contain unique property names.",
                  item.pointer()));
          break;
        }
      }
    }
  }

  private static boolean taggedOneOfShape(
      ArrayValue oneOf, List<SchemaSupportDiagnostic> diagnostics) {
    if (oneOf.items().size() < 2) {
      return false;
    }
    String tagName = null;
    HashSet<String> tagValues = new HashSet<>();
    for (SchemaSyntaxValue item : oneOf.items()) {
      if (!(item instanceof ObjectValue branch)) {
        return false;
      }
      validateSchema(branch, diagnostics);
      if (member(branch, "$ref").isPresent()) {
        continue;
      }
      Optional<TagCandidate> tagCandidate = tagCandidate(branch);
      if (tagCandidate.isEmpty()) {
        return false;
      }
      if (tagName == null) {
        tagName = tagCandidate.get().name();
      } else if (!tagName.equals(tagCandidate.get().name())) {
        return false;
      }
      if (!tagValues.add(tagCandidate.get().value())) {
        return false;
      }
    }
    return diagnostics.isEmpty()
        && (tagName != null
            || oneOf.items().stream()
                .allMatch(
                    item ->
                        item instanceof ObjectValue objectValue
                            && member(objectValue, "$ref").isPresent()));
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

  private static Optional<TagCandidate> tagCandidate(ObjectValue branch) {
    if (!stringMember(branch, "type").filter("object"::equals).isPresent()) {
      return Optional.empty();
    }
    Optional<ObjectValue> properties = objectMember(branch, "properties");
    Optional<ArrayValue> required = arrayMember(branch, "required");
    if (properties.isEmpty() || required.isEmpty()) {
      return Optional.empty();
    }
    for (SchemaSyntaxValue requiredItem : required.get().items()) {
      if (!(requiredItem instanceof StringValue requiredName)) {
        continue;
      }
      Optional<ObjectValue> propertySchema = objectMember(properties.get(), requiredName.value());
      if (propertySchema.isEmpty()) {
        continue;
      }
      Optional<String> type = stringMember(propertySchema.get(), "type");
      Optional<String> constValue = stringMember(propertySchema.get(), "const");
      if (type.filter("string"::equals).isPresent() && constValue.isPresent()) {
        return Optional.of(new TagCandidate(requiredName.value(), constValue.get()));
      }
    }
    return Optional.empty();
  }

  private static Optional<String> stringMember(ObjectValue object, String name) {
    return member(object, name)
        .filter(member -> member.value() instanceof StringValue)
        .map(member -> ((StringValue) member.value()).value());
  }

  private static Optional<ObjectValue> objectMember(ObjectValue object, String name) {
    return member(object, name)
        .filter(member -> member.value() instanceof ObjectValue)
        .map(member -> (ObjectValue) member.value());
  }

  private static Optional<ArrayValue> arrayMember(ObjectValue object, String name) {
    return member(object, name)
        .filter(member -> member.value() instanceof ArrayValue)
        .map(member -> (ArrayValue) member.value());
  }

  private static Optional<Member> member(ObjectValue object, String name) {
    for (Member member : object.members()) {
      if (name.equals(member.name())) {
        return Optional.of(member);
      }
    }
    return Optional.empty();
  }

  private static void validateEnum(
      SchemaSyntaxValue value, List<SchemaSupportDiagnostic> diagnostics) {
    if (!(value instanceof ArrayValue arrayValue)) {
      diagnostics.add(invalidValue("The 'enum' keyword value must be an array.", value.pointer()));
      return;
    }
    if (arrayValue.items().isEmpty()) {
      diagnostics.add(
          invalidValue("The 'enum' keyword value must be a non-empty array.", value.pointer()));
      return;
    }
    HashSet<String> values = new HashSet<>();
    for (SchemaSyntaxValue item : arrayValue.items()) {
      String key = canonicalValue(item);
      if (!values.add(key)) {
        diagnostics.add(
            invalidValue(
                "The 'enum' keyword value must contain unique JSON values.", item.pointer()));
        return;
      }
    }
  }

  private static String canonicalValue(SchemaSyntaxValue value) {
    return switch (value) {
      case StringValue stringValue -> "s:" + stringValue.value();
      case NumberValue numberValue ->
          "n:"
              + new java.math.BigDecimal(numberValue.literal())
                  .stripTrailingZeros()
                  .toPlainString();
      case BooleanValue booleanValue -> "b:" + booleanValue.value();
      case SchemaSyntaxValue.NullValue ignored -> "z:null";
      case ArrayValue arrayValue ->
          "a:["
              + arrayValue.items().stream()
                  .map(SchemaSupportProfile::canonicalValue)
                  .collect(java.util.stream.Collectors.joining(","))
              + "]";
      case ObjectValue objectValue ->
          "o:{"
              + objectValue.members().stream()
                  .sorted(Comparator.comparing(Member::name))
                  .map(member -> member.name() + ":" + canonicalValue(member.value()))
                  .collect(java.util.stream.Collectors.joining(","))
              + "}";
    };
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

  private record TagCandidate(String name, String value) {
    private TagCandidate {
      Objects.requireNonNull(name, "name");
      Objects.requireNonNull(value, "value");
    }
  }
}
