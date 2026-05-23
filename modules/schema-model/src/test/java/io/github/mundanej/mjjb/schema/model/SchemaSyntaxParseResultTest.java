package io.github.mundanej.mjjb.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.ObjectValue;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SchemaSyntaxParseResultTest {
  @Test
  void successFactoryCreatesSuccessfulResultWithRootAndNoDiagnostics() {
    ObjectValue root = rootValue();

    SchemaSyntaxParseResult result = SchemaSyntaxParseResult.success(root);

    assertTrue(result.successful());
    assertSame(root, result.root());
    assertTrue(result.diagnostics().isEmpty());
  }

  @Test
  void failureFactoryCreatesUnsuccessfulResultWithDiagnosticsAndNoRoot() {
    SchemaSyntaxDiagnostic diagnostic = diagnostic();

    SchemaSyntaxParseResult result = SchemaSyntaxParseResult.failure(List.of(diagnostic));

    assertFalse(result.successful());
    assertEquals(List.of(diagnostic), result.diagnostics());
    assertNull(result.root());
  }

  @Test
  void rejectsIllegalRootAndDiagnosticCombinations() {
    ObjectValue root = rootValue();
    List<SchemaSyntaxDiagnostic> diagnostics = List.of(diagnostic());

    assertThrows(
        IllegalArgumentException.class, () -> new SchemaSyntaxParseResult(null, List.of()));
    assertThrows(
        IllegalArgumentException.class, () -> new SchemaSyntaxParseResult(root, diagnostics));
    assertThrows(IllegalArgumentException.class, () -> SchemaSyntaxParseResult.failure(List.of()));
  }

  @Test
  void defensivelyCopiesDiagnosticsAndExposesImmutableList() {
    SchemaSyntaxDiagnostic first = diagnostic();
    SchemaSyntaxDiagnostic second =
        new SchemaSyntaxDiagnostic("other", "Other diagnostic", JsonPointer.ROOT);
    ArrayList<SchemaSyntaxDiagnostic> diagnostics = new ArrayList<>(List.of(first));

    SchemaSyntaxParseResult result = SchemaSyntaxParseResult.failure(diagnostics);
    diagnostics.add(second);

    assertEquals(List.of(first), result.diagnostics());
    assertThrows(UnsupportedOperationException.class, () -> result.diagnostics().add(second));
  }

  @Test
  void rejectsNullRootDiagnosticsAndDiagnosticElements() {
    ArrayList<SchemaSyntaxDiagnostic> diagnosticsWithNull = new ArrayList<>(List.of(diagnostic()));
    diagnosticsWithNull.add(null);

    assertThrows(NullPointerException.class, () -> SchemaSyntaxParseResult.success(null));
    assertThrows(NullPointerException.class, () -> SchemaSyntaxParseResult.failure(null));
    assertThrows(NullPointerException.class, () -> new SchemaSyntaxParseResult(rootValue(), null));
    assertThrows(
        NullPointerException.class, () -> SchemaSyntaxParseResult.failure(diagnosticsWithNull));
  }

  private static ObjectValue rootValue() {
    return new ObjectValue(JsonPointer.ROOT, List.of());
  }

  private static SchemaSyntaxDiagnostic diagnostic() {
    return new SchemaSyntaxDiagnostic("invalid-json", "Invalid JSON", JsonPointer.ROOT);
  }
}
