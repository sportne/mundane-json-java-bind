package io.github.mundanej.mjjb.schema.model;

import java.net.URI;

/** Supported JSON Schema dialect identifiers. */
public enum JsonSchemaDialect {
  DRAFT_2020_12(URI.create("https://json-schema.org/draft/2020-12/schema"));

  private final URI uri;

  JsonSchemaDialect(URI uri) {
    this.uri = uri;
  }

  public URI uri() {
    return uri;
  }
}
