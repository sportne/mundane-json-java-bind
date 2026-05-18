package io.github.mundanej.mjjb.conformance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjjb.schema.model.SchemaSupportProfile;
import org.junit.jupiter.api.Test;

final class SpecTruthDocumentsTest {
  @Test
  void recordsOfficialDraft202012TruthDocuments() {
    assertTrue(
        SchemaSupportProfile.truthDocuments()
            .contains("https://json-schema.org/draft/2020-12/schema"));
    assertTrue(
        SchemaSupportProfile.truthDocuments()
            .contains("https://github.com/json-schema-org/JSON-Schema-Test-Suite"));
  }
}
