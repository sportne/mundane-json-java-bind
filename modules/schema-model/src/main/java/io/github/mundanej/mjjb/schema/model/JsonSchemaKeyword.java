package io.github.mundanej.mjjb.schema.model;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Draft 2020-12 keywords known to the v1 generated-binding profile. */
public enum JsonSchemaKeyword {
  TYPE("type", true),
  PROPERTIES("properties", true),
  REQUIRED("required", true),
  ADDITIONAL_PROPERTIES("additionalProperties", true),
  ENUM("enum", true),
  CONST("const", true),
  DEFAULT("default", true),
  ITEMS("items", true),
  MIN_ITEMS("minItems", true),
  MAX_ITEMS("maxItems", true),
  MINIMUM("minimum", true),
  MAXIMUM("maximum", true),
  EXCLUSIVE_MINIMUM("exclusiveMinimum", true),
  EXCLUSIVE_MAXIMUM("exclusiveMaximum", true),
  MIN_LENGTH("minLength", true),
  MAX_LENGTH("maxLength", true),
  PATTERN("pattern", true),
  FORMAT("format", true),
  ONE_OF("oneOf", true),
  REF("$ref", false),
  DEFS("$defs", false),
  ALL_OF("allOf", false),
  ANY_OF("anyOf", false),
  NOT("not", false),
  IF("if", false),
  THEN("then", false),
  ELSE("else", false),
  DEPENDENT_SCHEMAS("dependentSchemas", false),
  UNEVALUATED_PROPERTIES("unevaluatedProperties", false),
  UNEVALUATED_ITEMS("unevaluatedItems", false),
  PATTERN_PROPERTIES("patternProperties", false),
  PREFIX_ITEMS("prefixItems", false),
  CONTAINS("contains", false);

  private static final Set<String> SUPPORTED_NAMES =
      Arrays.stream(values())
          .filter(JsonSchemaKeyword::supportedInV1)
          .map(JsonSchemaKeyword::keyword)
          .collect(Collectors.toUnmodifiableSet());

  private final String keyword;
  private final boolean supportedInV1;

  JsonSchemaKeyword(String keyword, boolean supportedInV1) {
    this.keyword = keyword;
    this.supportedInV1 = supportedInV1;
  }

  public String keyword() {
    return keyword;
  }

  public boolean supportedInV1() {
    return supportedInV1;
  }

  public static Optional<JsonSchemaKeyword> fromKeyword(String keyword) {
    return Arrays.stream(values()).filter(value -> value.keyword.equals(keyword)).findFirst();
  }

  public static boolean supportedKeywordName(String keyword) {
    return SUPPORTED_NAMES.contains(keyword);
  }
}
