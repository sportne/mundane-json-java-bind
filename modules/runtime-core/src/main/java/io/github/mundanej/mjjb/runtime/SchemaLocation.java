package io.github.mundanej.mjjb.runtime;

import java.net.URI;
import java.util.Objects;

/** Location inside a JSON Schema document, addressed by URI and JSON Pointer. */
public record SchemaLocation(URI schemaUri, String pointer) {
  public static final SchemaLocation UNKNOWN =
      new SchemaLocation(URI.create("urn:mjjb:unknown"), "");

  public SchemaLocation {
    Objects.requireNonNull(schemaUri, "schemaUri");
    Objects.requireNonNull(pointer, "pointer");
    if (!pointer.isEmpty() && pointer.charAt(0) != '/') {
      throw new IllegalArgumentException("pointer must be empty or start with /");
    }
  }
}
