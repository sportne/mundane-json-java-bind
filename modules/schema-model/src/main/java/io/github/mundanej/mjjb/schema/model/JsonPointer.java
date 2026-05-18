package io.github.mundanej.mjjb.schema.model;

import java.util.Objects;

/** JSON Pointer value used for schema locations. */
public record JsonPointer(String value) {
  public static final JsonPointer ROOT = new JsonPointer("");

  public JsonPointer {
    Objects.requireNonNull(value, "value");
    if (!value.isEmpty() && value.charAt(0) != '/') {
      throw new IllegalArgumentException("value must be empty or start with /");
    }
  }

  public JsonPointer property(String token) {
    Objects.requireNonNull(token, "token");
    return new JsonPointer(value + "/" + escape(token));
  }

  private static String escape(String token) {
    return token.replace("~", "~0").replace("/", "~1");
  }
}
