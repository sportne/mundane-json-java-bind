package io.github.mundanej.mjjb.testkit;

import java.util.Objects;

/** Reproducible configuration for deterministic fuzz-style tests. */
public record DeterministicFuzzConfig(
    long seed, int iterations, int maxDepth, int maxStringLength) {
  private static final String SEED_PROPERTY = "mjjb.fuzz.seed";
  private static final String ITERATIONS_PROPERTY = "mjjb.fuzz.iterations";
  private static final String MAX_DEPTH_PROPERTY = "mjjb.fuzz.maxDepth";
  private static final String MAX_STRING_LENGTH_PROPERTY = "mjjb.fuzz.maxStringLength";

  public DeterministicFuzzConfig {
    if (iterations <= 0) {
      throw new IllegalArgumentException("iterations must be positive");
    }
    if (maxDepth <= 0) {
      throw new IllegalArgumentException("maxDepth must be positive");
    }
    if (maxStringLength <= 0) {
      throw new IllegalArgumentException("maxStringLength must be positive");
    }
  }

  public static DeterministicFuzzConfig fromSystemProperties(
      long defaultSeed, int defaultIterations, int defaultMaxDepth, int defaultMaxStringLength) {
    return new DeterministicFuzzConfig(
        longProperty(SEED_PROPERTY, defaultSeed),
        intProperty(ITERATIONS_PROPERTY, defaultIterations),
        intProperty(MAX_DEPTH_PROPERTY, defaultMaxDepth),
        intProperty(MAX_STRING_LENGTH_PROPERTY, defaultMaxStringLength));
  }

  public String caseLabel(String scenario, int iteration, String input) {
    Objects.requireNonNull(scenario, "scenario");
    Objects.requireNonNull(input, "input");
    return "scenario="
        + scenario
        + ", seed="
        + seed
        + ", iteration="
        + iteration
        + ", input="
        + input;
  }

  private static long longProperty(String name, long defaultValue) {
    String value = System.getProperty(name);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    return Long.parseLong(value);
  }

  private static int intProperty(String name, int defaultValue) {
    String value = System.getProperty(name);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    return Integer.parseInt(value);
  }
}
