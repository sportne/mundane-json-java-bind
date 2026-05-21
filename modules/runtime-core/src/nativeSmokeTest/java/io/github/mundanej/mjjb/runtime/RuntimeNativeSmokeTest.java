package io.github.mundanej.mjjb.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class RuntimeNativeSmokeTest {
  @Test
  void exercisesRuntimePrimitives() {
    JsonPath path = JsonPath.ROOT.property("user").property("names").index(0);
    JsonDiagnostic diagnostic =
        JsonDiagnostic.error(
            "MJJBN-001",
            "Native smoke diagnostic.",
            path,
            new JsonLocation("native-smoke.json", 12L, 3, 7));
    ValidationErrors errors = ValidationErrors.create(ValidationMode.ACCUMULATE);

    assertEquals("$.user.names[0]", path.value());
    assertTrue(JsonField.absent().isAbsent());
    assertTrue(JsonField.explicitNull().isExplicitNull());
    assertEquals("value", JsonField.value("value").requireValue());
    assertEquals("MJJBN-001", new JsonReadException(diagnostic).diagnostic().code());
    assertTrue(errors.add(ValidationError.of("MJJBN-002", "First.", JsonPath.ROOT)));
    assertTrue(errors.add(ValidationError.of("MJJBN-003", "Second.", path)));

    ValidationResult result = errors.toResult();
    assertFalse(result.isValid());
    assertEquals(
        List.of("MJJBN-002", "MJJBN-003"),
        result.errors().stream().map(ValidationError::code).toList());
  }
}
