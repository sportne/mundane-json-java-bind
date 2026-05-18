package io.github.mundanej.mjjb.examples.basicrecord.generated;

import java.util.Objects;
import java.util.Optional;

public record BasicRecord(
    String id,
    long count,
    Optional<String> displayName,
    Optional<Double> score,
    Optional<Boolean> active) {
  public BasicRecord {
    id = Objects.requireNonNull(id, "id");
    displayName = Objects.requireNonNull(displayName, "displayName");
    score = Objects.requireNonNull(score, "score");
    active = Objects.requireNonNull(active, "active");
  }
}
