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
    if (isSimplePropertyName(propertyName)) {
      return new JsonPath(value + "." + propertyName);
    }
    return new JsonPath(value + "[\"" + escapePropertyName(propertyName) + "\"]");
  }

  public JsonPath index(int index) {
    if (index < 0) {
      throw new IllegalArgumentException("index must be non-negative");
    }
    return new JsonPath(value + "[" + index + "]");
  }

  private static boolean isSimplePropertyName(String propertyName) {
    if (propertyName.isEmpty()) {
      return false;
    }
    char first = propertyName.charAt(0);
    if (!isIdentifierStart(first)) {
      return false;
    }
    for (int index = 1; index < propertyName.length(); index++) {
      if (!isIdentifierPart(propertyName.charAt(index))) {
        return false;
      }
    }
    return true;
  }

  private static boolean isIdentifierStart(char value) {
    return (value >= 'A' && value <= 'Z') || (value >= 'a' && value <= 'z') || value == '_';
  }

  private static boolean isIdentifierPart(char value) {
    return isIdentifierStart(value) || (value >= '0' && value <= '9');
  }

  private static String escapePropertyName(String propertyName) {
    StringBuilder escaped = new StringBuilder();
    for (int index = 0; index < propertyName.length(); index++) {
      char value = propertyName.charAt(index);
      switch (value) {
        case '"' -> escaped.append("\\\"");
        case '\\' -> escaped.append("\\\\");
        case '\b' -> escaped.append("\\b");
        case '\f' -> escaped.append("\\f");
        case '\n' -> escaped.append("\\n");
        case '\r' -> escaped.append("\\r");
        case '\t' -> escaped.append("\\t");
        default -> {
          if (value < 0x20) {
            escaped.append("\\u");
            String hex = Integer.toHexString(value);
            for (int pad = hex.length(); pad < 4; pad++) {
              escaped.append('0');
            }
            escaped.append(hex);
          } else {
            escaped.append(value);
          }
        }
      }
    }
    return escaped.toString();
  }
}
