package io.github.mundanej.mjjb.schema.model;

import java.util.List;
import java.util.Objects;

/** v1 schema support profile helpers. */
public final class SchemaSupportProfile {
  private SchemaSupportProfile() {}

  public static boolean supportsKeyword(String keyword) {
    Objects.requireNonNull(keyword, "keyword");
    return JsonSchemaKeyword.supportedKeywordName(keyword);
  }

  public static SchemaSupportDiagnostic unsupportedKeyword(String keyword, JsonPointer pointer) {
    Objects.requireNonNull(keyword, "keyword");
    Objects.requireNonNull(pointer, "pointer");
    return new SchemaSupportDiagnostic(
        "MJJBG-SCHEMA-UNSUPPORTED-KEYWORD",
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
}
