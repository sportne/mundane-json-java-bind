package io.github.mundanej.mjjb.schema.model;

import java.util.Optional;

/** Generated-binding support profiles. */
public enum JsonSchemaProfile {
  JSP_DATA_2020_12("JSP-DATA-2020-12");

  private final String token;

  JsonSchemaProfile(String token) {
    this.token = token;
  }

  public String token() {
    return token;
  }

  public static Optional<JsonSchemaProfile> fromToken(String token) {
    if (token == null) {
      return Optional.empty();
    }
    for (JsonSchemaProfile profile : values()) {
      if (profile.token.equals(token)) {
        return Optional.of(profile);
      }
    }
    return Optional.empty();
  }
}
