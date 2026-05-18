package io.github.mundanej.mjjb.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import org.junit.jupiter.api.Test;

final class SchemaSupportProfileTest {
  @Test
  void declaresSupportedAndUnsupportedKeywords() {
    assertTrue(SchemaSupportProfile.supportsKeyword("type"));
    assertTrue(SchemaSupportProfile.supportsKeyword("properties"));
    assertFalse(SchemaSupportProfile.supportsKeyword("$ref"));
    assertFalse(SchemaSupportProfile.supportsKeyword("unevaluatedProperties"));
  }

  @Test
  void jsonPointerEscapesTokens() {
    assertEquals("/a~1b/c~0d", JsonPointer.ROOT.property("a/b").property("c~d").value());
  }

  @Test
  void unsupportedKeywordDiagnosticReferencesProfile() {
    SchemaSupportDiagnostic diagnostic =
        SchemaSupportProfile.unsupportedKeyword("$ref", JsonPointer.ROOT.property("$ref"));

    assertEquals("MJJBG-SCHEMA-UNSUPPORTED-KEYWORD", diagnostic.code());
    assertEquals("/$ref", diagnostic.pointer().value());
  }

  @Test
  void profileAndDialectExposeDraft202012Tokens() {
    assertEquals(
        URI.create("https://json-schema.org/draft/2020-12/schema"),
        JsonSchemaDialect.DRAFT_2020_12.uri());
    assertEquals("JSP-DATA-2020-12", JsonSchemaProfile.JSP_DATA_2020_12.token());
    assertTrue(JsonSchemaProfile.fromToken("JSP-DATA-2020-12").isPresent());
    assertTrue(JsonSchemaProfile.fromToken(null).isEmpty());
    assertTrue(JsonSchemaProfile.fromToken("other").isEmpty());
  }

  @Test
  void keywordLookupIsDeterministic() {
    assertEquals(JsonSchemaKeyword.TYPE, JsonSchemaKeyword.fromKeyword("type").orElseThrow());
    assertTrue(JsonSchemaKeyword.fromKeyword("notAKeyword").isEmpty());
    assertTrue(JsonSchemaKeyword.ONE_OF.supportedInV1());
    assertFalse(JsonSchemaKeyword.REF.supportedInV1());
  }
}
