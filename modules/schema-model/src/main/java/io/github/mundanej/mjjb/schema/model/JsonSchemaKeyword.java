package io.github.mundanej.mjjb.schema.model;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Draft 2020-12 keywords known to the v1 generated-binding profile. */
public enum JsonSchemaKeyword {
  TYPE("type", JsonSchemaKeywordSupport.SUPPORTED_BINDING),
  PROPERTIES("properties", JsonSchemaKeywordSupport.SUPPORTED_BINDING),
  REQUIRED("required", JsonSchemaKeywordSupport.SUPPORTED_BINDING),
  ADDITIONAL_PROPERTIES("additionalProperties", JsonSchemaKeywordSupport.SUPPORTED_BINDING),
  ENUM("enum", JsonSchemaKeywordSupport.SUPPORTED_BINDING),
  CONST("const", JsonSchemaKeywordSupport.SUPPORTED_BINDING),
  DEFAULT("default", JsonSchemaKeywordSupport.SUPPORTED_BINDING),
  ITEMS("items", JsonSchemaKeywordSupport.SUPPORTED_BINDING),
  MIN_ITEMS("minItems", JsonSchemaKeywordSupport.SUPPORTED_BINDING),
  MAX_ITEMS("maxItems", JsonSchemaKeywordSupport.SUPPORTED_BINDING),
  MINIMUM("minimum", JsonSchemaKeywordSupport.SUPPORTED_BINDING),
  MAXIMUM("maximum", JsonSchemaKeywordSupport.SUPPORTED_BINDING),
  EXCLUSIVE_MINIMUM("exclusiveMinimum", JsonSchemaKeywordSupport.SUPPORTED_BINDING),
  EXCLUSIVE_MAXIMUM("exclusiveMaximum", JsonSchemaKeywordSupport.SUPPORTED_BINDING),
  MIN_LENGTH("minLength", JsonSchemaKeywordSupport.SUPPORTED_BINDING),
  MAX_LENGTH("maxLength", JsonSchemaKeywordSupport.SUPPORTED_BINDING),
  PATTERN("pattern", JsonSchemaKeywordSupport.SUPPORTED_BINDING),
  FORMAT("format", JsonSchemaKeywordSupport.SUPPORTED_BINDING),
  ONE_OF("oneOf", JsonSchemaKeywordSupport.SUPPORTED_BINDING),
  SCHEMA("$schema", JsonSchemaKeywordSupport.IGNORED_ANNOTATION),
  TITLE("title", JsonSchemaKeywordSupport.IGNORED_ANNOTATION),
  DESCRIPTION("description", JsonSchemaKeywordSupport.IGNORED_ANNOTATION),
  COMMENT("$comment", JsonSchemaKeywordSupport.IGNORED_ANNOTATION),
  EXAMPLES("examples", JsonSchemaKeywordSupport.IGNORED_ANNOTATION),
  DEPRECATED("deprecated", JsonSchemaKeywordSupport.IGNORED_ANNOTATION),
  READ_ONLY("readOnly", JsonSchemaKeywordSupport.IGNORED_ANNOTATION),
  WRITE_ONLY("writeOnly", JsonSchemaKeywordSupport.IGNORED_ANNOTATION),
  ID("$id", JsonSchemaKeywordSupport.UNSUPPORTED),
  ANCHOR("$anchor", JsonSchemaKeywordSupport.UNSUPPORTED),
  DYNAMIC_ANCHOR("$dynamicAnchor", JsonSchemaKeywordSupport.UNSUPPORTED),
  VOCABULARY("$vocabulary", JsonSchemaKeywordSupport.UNSUPPORTED),
  REF("$ref", JsonSchemaKeywordSupport.SUPPORTED_BINDING),
  DYNAMIC_REF("$dynamicRef", JsonSchemaKeywordSupport.UNSUPPORTED),
  DEFS("$defs", JsonSchemaKeywordSupport.SUPPORTED_BINDING),
  ALL_OF("allOf", JsonSchemaKeywordSupport.SUPPORTED_BINDING),
  ANY_OF("anyOf", JsonSchemaKeywordSupport.UNSUPPORTED),
  NOT("not", JsonSchemaKeywordSupport.UNSUPPORTED),
  IF("if", JsonSchemaKeywordSupport.UNSUPPORTED),
  THEN("then", JsonSchemaKeywordSupport.UNSUPPORTED),
  ELSE("else", JsonSchemaKeywordSupport.UNSUPPORTED),
  DEPENDENT_SCHEMAS("dependentSchemas", JsonSchemaKeywordSupport.UNSUPPORTED),
  PREFIX_ITEMS("prefixItems", JsonSchemaKeywordSupport.UNSUPPORTED),
  CONTAINS("contains", JsonSchemaKeywordSupport.UNSUPPORTED),
  PATTERN_PROPERTIES("patternProperties", JsonSchemaKeywordSupport.SUPPORTED_BINDING),
  PROPERTY_NAMES("propertyNames", JsonSchemaKeywordSupport.SUPPORTED_BINDING),
  UNEVALUATED_ITEMS("unevaluatedItems", JsonSchemaKeywordSupport.UNSUPPORTED),
  UNEVALUATED_PROPERTIES("unevaluatedProperties", JsonSchemaKeywordSupport.UNSUPPORTED),
  MULTIPLE_OF("multipleOf", JsonSchemaKeywordSupport.SUPPORTED_BINDING),
  UNIQUE_ITEMS("uniqueItems", JsonSchemaKeywordSupport.SUPPORTED_BINDING),
  MAX_CONTAINS("maxContains", JsonSchemaKeywordSupport.UNSUPPORTED),
  MIN_CONTAINS("minContains", JsonSchemaKeywordSupport.UNSUPPORTED),
  MAX_PROPERTIES("maxProperties", JsonSchemaKeywordSupport.SUPPORTED_BINDING),
  MIN_PROPERTIES("minProperties", JsonSchemaKeywordSupport.SUPPORTED_BINDING),
  DEPENDENT_REQUIRED("dependentRequired", JsonSchemaKeywordSupport.SUPPORTED_BINDING),
  CONTENT_ENCODING("contentEncoding", JsonSchemaKeywordSupport.UNSUPPORTED),
  CONTENT_MEDIA_TYPE("contentMediaType", JsonSchemaKeywordSupport.UNSUPPORTED),
  CONTENT_SCHEMA("contentSchema", JsonSchemaKeywordSupport.UNSUPPORTED);

  private static final Set<String> SUPPORTED_NAMES =
      Arrays.stream(values())
          .filter(JsonSchemaKeyword::supportedInV1)
          .map(JsonSchemaKeyword::keyword)
          .collect(Collectors.toUnmodifiableSet());

  private static final Set<String> ACCEPTED_NAMES =
      Arrays.stream(values())
          .filter(value -> value.support != JsonSchemaKeywordSupport.UNSUPPORTED)
          .map(JsonSchemaKeyword::keyword)
          .collect(Collectors.toUnmodifiableSet());

  private final String keyword;
  private final JsonSchemaKeywordSupport support;

  JsonSchemaKeyword(String keyword, JsonSchemaKeywordSupport support) {
    this.keyword = keyword;
    this.support = support;
  }

  public String keyword() {
    return keyword;
  }

  public boolean supportedInV1() {
    return support == JsonSchemaKeywordSupport.SUPPORTED_BINDING;
  }

  public boolean acceptedInV1() {
    return support != JsonSchemaKeywordSupport.UNSUPPORTED;
  }

  public JsonSchemaKeywordSupport support() {
    return support;
  }

  public static Optional<JsonSchemaKeyword> fromKeyword(String keyword) {
    return Arrays.stream(values()).filter(value -> value.keyword.equals(keyword)).findFirst();
  }

  public static boolean supportedKeywordName(String keyword) {
    return SUPPORTED_NAMES.contains(keyword);
  }

  public static boolean acceptedKeywordName(String keyword) {
    return ACCEPTED_NAMES.contains(keyword);
  }

  public static List<JsonSchemaKeyword> bindingKeywords() {
    return Arrays.stream(values())
        .filter(value -> value.support == JsonSchemaKeywordSupport.SUPPORTED_BINDING)
        .toList();
  }

  public static List<JsonSchemaKeyword> ignoredAnnotationKeywords() {
    return Arrays.stream(values())
        .filter(value -> value.support == JsonSchemaKeywordSupport.IGNORED_ANNOTATION)
        .toList();
  }

  public static List<JsonSchemaKeyword> unsupportedKeywords() {
    return Arrays.stream(values())
        .filter(value -> value.support == JsonSchemaKeywordSupport.UNSUPPORTED)
        .toList();
  }
}
