package io.github.mundanej.mjjb.runtime;

import java.util.Objects;

/** Deterministic JSON instance path using JSONPath-like syntax rooted at {@code $}. */
public record JsonPath(String value) {
  public static final JsonPath ROOT = new JsonPath("$");

  public JsonPath {
    Objects.requireNonNull(value, "value");
    if (value.isBlank() || value.charAt(0) != '$') {
      throw new IllegalArgumentException("value must be rooted at $");
    }
  }

  public JsonPath property(String propertyName) {
    Objects.requireNonNull(propertyName, "propertyName");
    return new JsonPath(value + "." + propertyName);
  }

  public JsonPath index(int index) {
    if (index < 0) {
      throw new IllegalArgumentException("index must be non-negative");
    }
    return new JsonPath(value + "[" + index + "]");
  }
}
