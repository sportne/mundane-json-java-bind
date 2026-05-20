package io.github.mundanej.mjjb.runtime;

import java.util.Objects;
import java.util.Optional;

/** Generated-model field state for values where absent, explicit null, and value are distinct. */
public final class JsonField<T> {
  private static final JsonField<?> ABSENT = new JsonField<>(State.ABSENT, null);
  private static final JsonField<?> NULL = new JsonField<>(State.NULL, null);

  private final State state;
  private final T value;

  private JsonField(State state, T value) {
    this.state = Objects.requireNonNull(state, "state");
    this.value = value;
  }

  public static <T> JsonField<T> absent() {
    @SuppressWarnings("unchecked")
    JsonField<T> field = (JsonField<T>) ABSENT;
    return field;
  }

  public static <T> JsonField<T> explicitNull() {
    @SuppressWarnings("unchecked")
    JsonField<T> field = (JsonField<T>) NULL;
    return field;
  }

  public static <T> JsonField<T> value(T value) {
    return new JsonField<>(State.VALUE, Objects.requireNonNull(value, "value"));
  }

  public boolean isAbsent() {
    return state == State.ABSENT;
  }

  public boolean isExplicitNull() {
    return state == State.NULL;
  }

  public boolean hasValue() {
    return state == State.VALUE;
  }

  public Optional<T> value() {
    return Optional.ofNullable(value);
  }

  public T requireValue() {
    if (!hasValue()) {
      throw new IllegalStateException("field does not contain a value");
    }
    return value;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof JsonField<?> jsonField)) {
      return false;
    }
    return state == jsonField.state && Objects.equals(value, jsonField.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(state, value);
  }

  @Override
  public String toString() {
    return switch (state) {
      case ABSENT -> "JsonField.absent";
      case NULL -> "JsonField.explicitNull";
      case VALUE -> "JsonField.value[" + value + "]";
    };
  }

  private enum State {
    ABSENT,
    NULL,
    VALUE
  }
}
