package io.github.mundanej.mjjb.generator.api;

import java.util.Optional;

/** Supported public generator profiles. */
public enum GeneratorProfile {
  JSP_DATA_2020_12("JSP-DATA-2020-12");

  private final String cliToken;

  GeneratorProfile(String cliToken) {
    this.cliToken = cliToken;
  }

  public String cliToken() {
    return cliToken;
  }

  public static Optional<GeneratorProfile> fromCliToken(String token) {
    if (token == null) {
      return Optional.empty();
    }
    for (GeneratorProfile profile : values()) {
      if (profile.cliToken.equals(token)) {
        return Optional.of(profile);
      }
    }
    return Optional.empty();
  }
}
