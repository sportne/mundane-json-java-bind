package io.github.mundanej.mjjb.generator.core.internal.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjjb.schema.model.JsonPointer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class BindingBuildResultTest {
  @Test
  void successFactoryCreatesSuccessfulResultWithModelAndNoDiagnostics() {
    BindingModel model = model();

    BindingBuildResult result = BindingBuildResult.success(model);

    assertTrue(result.model().isPresent());
    assertSame(model, result.model().orElseThrow());
    assertTrue(result.diagnostics().isEmpty());
  }

  @Test
  void failureFactoryCreatesUnsuccessfulResultWithDiagnosticsAndNoModel() {
    BindingDiagnostic diagnostic = diagnostic("first", JsonPointer.ROOT);

    BindingBuildResult result = BindingBuildResult.failure(List.of(diagnostic));

    assertFalse(result.model().isPresent());
    assertEquals(List.of(diagnostic), result.diagnostics());
  }

  @Test
  void successStateIsRepresentedByModelPresenceAndEmptyDiagnostics() {
    BindingBuildResult success = BindingBuildResult.success(model());
    BindingBuildResult failure = BindingBuildResult.failure(List.of(diagnostic()));

    assertTrue(success.model().isPresent());
    assertTrue(success.diagnostics().isEmpty());
    assertTrue(failure.model().isEmpty());
    assertFalse(failure.diagnostics().isEmpty());
  }

  @Test
  void rejectsIllegalModelAndDiagnosticCombinations() {
    BindingModel model = model();
    List<BindingDiagnostic> diagnostics = List.of(diagnostic());

    assertThrows(
        IllegalArgumentException.class,
        () -> new BindingBuildResult(Optional.of(model), diagnostics));
    assertThrows(
        IllegalArgumentException.class, () -> new BindingBuildResult(Optional.empty(), List.of()));
  }

  @Test
  void defensivelyCopiesDiagnosticsAndExposesImmutableList() {
    BindingDiagnostic first = diagnostic("first", JsonPointer.ROOT);
    BindingDiagnostic second = diagnostic("second", JsonPointer.ROOT.property("type"));
    ArrayList<BindingDiagnostic> diagnostics = new ArrayList<>(List.of(first));

    BindingBuildResult result = BindingBuildResult.failure(diagnostics);
    diagnostics.add(second);

    assertEquals(List.of(first), result.diagnostics());
    assertThrows(UnsupportedOperationException.class, () -> result.diagnostics().add(second));
  }

  @Test
  void preservesDiagnosticListOrderAndElements() {
    BindingDiagnostic first = diagnostic("first", JsonPointer.ROOT.property("type"));
    BindingDiagnostic second = diagnostic("second", JsonPointer.ROOT.property("properties"));

    BindingBuildResult result = BindingBuildResult.failure(List.of(first, second));

    assertEquals(List.of(first, second), result.diagnostics());
  }

  @Test
  void rejectsNullModelDiagnosticsAndDiagnosticElements() {
    ArrayList<BindingDiagnostic> diagnosticsWithNull = new ArrayList<>(List.of(diagnostic()));
    diagnosticsWithNull.add(null);

    assertThrows(NullPointerException.class, () -> BindingBuildResult.success(null));
    assertThrows(NullPointerException.class, () -> BindingBuildResult.failure(null));
    assertThrows(NullPointerException.class, () -> new BindingBuildResult(null, List.of()));
    assertThrows(
        NullPointerException.class, () -> new BindingBuildResult(Optional.of(model()), null));
    assertThrows(NullPointerException.class, () -> BindingBuildResult.failure(diagnosticsWithNull));
  }

  private static BindingModel model() {
    return new BindingModel(
        "example.generated",
        "GeneratedBindings",
        new ObjectBinding("GeneratedBindings", JsonPointer.ROOT, List.of()));
  }

  private static BindingDiagnostic diagnostic() {
    return diagnostic("binding-error", JsonPointer.ROOT);
  }

  private static BindingDiagnostic diagnostic(String codeSuffix, JsonPointer pointer) {
    return new BindingDiagnostic(
        "MJJBG-BINDING-" + codeSuffix.toUpperCase(java.util.Locale.ROOT),
        "Binding diagnostic " + codeSuffix,
        pointer);
  }
}
