package io.github.mundanej.mjjb.generator.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class GeneratorResultTest {
  @Test
  void successFactoryCreatesSuccessfulResultWithGeneratedSourcesOnly() {
    Path generatedSource = Path.of("generated/Example.java");

    GeneratorResult result = GeneratorResult.success(List.of(generatedSource));

    assertTrue(result.successful());
    assertEquals(List.of(generatedSource), result.generatedSources());
    assertEquals(List.of(), result.diagnostics());
  }

  @Test
  void failureFactoryCreatesUnsuccessfulResultWithDiagnosticsOnly() {
    GeneratorDiagnostic diagnostic = diagnostic();

    GeneratorResult result = GeneratorResult.failure(List.of(diagnostic));

    assertFalse(result.successful());
    assertEquals(List.of(), result.generatedSources());
    assertEquals(List.of(diagnostic), result.diagnostics());
  }

  @Test
  void successfulDependsOnlyOnDiagnostics() {
    Path generatedSource = Path.of("generated/Example.java");
    GeneratorDiagnostic diagnostic = diagnostic();

    GeneratorResult noDiagnostics = new GeneratorResult(List.of(), List.of());
    GeneratorResult generatedWithDiagnostic =
        new GeneratorResult(List.of(generatedSource), List.of(diagnostic));

    assertTrue(noDiagnostics.successful());
    assertFalse(generatedWithDiagnostic.successful());
  }

  @Test
  void generatedSourcesAreDefensivelyCopiedAndImmutable() {
    Path first = Path.of("generated/First.java");
    Path second = Path.of("generated/Second.java");
    List<Path> generatedSources = new ArrayList<>();
    generatedSources.add(first);

    GeneratorResult result = GeneratorResult.success(generatedSources);
    generatedSources.add(second);

    assertEquals(List.of(first), result.generatedSources());
    assertThrows(UnsupportedOperationException.class, () -> result.generatedSources().add(second));
  }

  @Test
  void diagnosticsAreDefensivelyCopiedAndImmutable() {
    GeneratorDiagnostic first = diagnostic("MJJBT-001");
    GeneratorDiagnostic second = diagnostic("MJJBT-002");
    List<GeneratorDiagnostic> diagnostics = new ArrayList<>();
    diagnostics.add(first);

    GeneratorResult result = GeneratorResult.failure(diagnostics);
    diagnostics.add(second);

    assertEquals(List.of(first), result.diagnostics());
    assertThrows(UnsupportedOperationException.class, () -> result.diagnostics().add(second));
  }

  @Test
  void rejectsNullLists() {
    List<Path> generatedSources = List.of(Path.of("generated/Example.java"));
    List<GeneratorDiagnostic> diagnostics = List.of(diagnostic());

    assertThrows(NullPointerException.class, () -> new GeneratorResult(null, diagnostics));
    assertThrows(NullPointerException.class, () -> new GeneratorResult(generatedSources, null));
    assertThrows(NullPointerException.class, () -> GeneratorResult.success(null));
    assertThrows(NullPointerException.class, () -> GeneratorResult.failure(null));
  }

  @Test
  void rejectsNullGeneratedSourceElements() {
    List<Path> generatedSources = new ArrayList<>();
    generatedSources.add(Path.of("generated/Example.java"));
    generatedSources.add(null);

    assertThrows(NullPointerException.class, () -> GeneratorResult.success(generatedSources));
    assertThrows(
        NullPointerException.class, () -> new GeneratorResult(generatedSources, List.of()));
  }

  @Test
  void rejectsNullDiagnosticElements() {
    List<GeneratorDiagnostic> diagnostics = new ArrayList<>();
    diagnostics.add(diagnostic());
    diagnostics.add(null);

    assertThrows(NullPointerException.class, () -> GeneratorResult.failure(diagnostics));
    assertThrows(NullPointerException.class, () -> new GeneratorResult(List.of(), diagnostics));
  }

  private static GeneratorDiagnostic diagnostic() {
    return diagnostic("MJJBT-001");
  }

  private static GeneratorDiagnostic diagnostic(String code) {
    return new GeneratorDiagnostic(code, "Broken schema.", Path.of("schema.json"), "/type");
  }
}
