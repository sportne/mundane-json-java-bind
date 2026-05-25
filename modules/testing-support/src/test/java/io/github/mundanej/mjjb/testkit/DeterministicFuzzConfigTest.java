package io.github.mundanej.mjjb.testkit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class DeterministicFuzzConfigTest {
  @Test
  void readsDefaultValuesWhenPropertiesAreUnset() {
    withFuzzProperties(
        Map.of(),
        () -> {
          DeterministicFuzzConfig config =
              DeterministicFuzzConfig.fromSystemProperties(11L, 12, 13, 14);

          assertAll(
              () -> assertEquals(11L, config.seed()),
              () -> assertEquals(12, config.iterations()),
              () -> assertEquals(13, config.maxDepth()),
              () -> assertEquals(14, config.maxStringLength()));
        });
  }

  @Test
  void readsSystemPropertyOverrides() {
    withFuzzProperties(
        Map.of(
            "mjjb.fuzz.seed", "101",
            "mjjb.fuzz.iterations", "102",
            "mjjb.fuzz.maxDepth", "103",
            "mjjb.fuzz.maxStringLength", "104"),
        () -> {
          DeterministicFuzzConfig config =
              DeterministicFuzzConfig.fromSystemProperties(1L, 2, 3, 4);

          assertAll(
              () -> assertEquals(101L, config.seed()),
              () -> assertEquals(102, config.iterations()),
              () -> assertEquals(103, config.maxDepth()),
              () -> assertEquals(104, config.maxStringLength()));
        });
  }

  @Test
  void rejectsNonPositiveBounds() {
    assertAll(
        () ->
            assertEquals(
                "iterations must be positive",
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new DeterministicFuzzConfig(1L, 0, 1, 1))
                    .getMessage()),
        () ->
            assertEquals(
                "maxDepth must be positive",
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new DeterministicFuzzConfig(1L, 1, 0, 1))
                    .getMessage()),
        () ->
            assertEquals(
                "maxStringLength must be positive",
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new DeterministicFuzzConfig(1L, 1, 1, 0))
                    .getMessage()));
  }

  @Test
  void caseLabelsIncludeReproductionDetails() {
    DeterministicFuzzConfig config = new DeterministicFuzzConfig(42L, 3, 4, 5);

    String label = config.caseLabel("scenario-name", 2, "{\"id\":1}");

    assertAll(
        () -> assertTrue(label.contains("scenario=scenario-name")),
        () -> assertTrue(label.contains("seed=42")),
        () -> assertTrue(label.contains("iteration=2")),
        () -> assertTrue(label.contains("input={\"id\":1}")));
  }

  private static void withFuzzProperties(Map<String, String> values, Runnable action) {
    LinkedHashMap<String, String> originalValues = new LinkedHashMap<>();
    for (String name : fuzzPropertyNames()) {
      originalValues.put(name, System.getProperty(name));
      System.clearProperty(name);
    }
    try {
      values.forEach(System::setProperty);
      action.run();
    } finally {
      for (Map.Entry<String, String> entry : originalValues.entrySet()) {
        if (entry.getValue() == null) {
          System.clearProperty(entry.getKey());
        } else {
          System.setProperty(entry.getKey(), entry.getValue());
        }
      }
    }
  }

  private static Iterable<String> fuzzPropertyNames() {
    return List.of(
        "mjjb.fuzz.seed",
        "mjjb.fuzz.iterations",
        "mjjb.fuzz.maxDepth",
        "mjjb.fuzz.maxStringLength");
  }
}
