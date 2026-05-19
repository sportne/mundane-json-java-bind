package io.github.mundanej.mjjb.generator.core.internal.binding;

import java.math.BigDecimal;
import java.util.Objects;

/** Scalar JSON literal value carried into generated validation metadata. */
public record LiteralValue(Kind kind, String value) {
  public enum Kind {
    STRING,
    INTEGER,
    NUMBER,
    BOOLEAN,
    NULL
  }

  public LiteralValue {
    Objects.requireNonNull(kind, "kind");
    if (kind == Kind.NULL) {
      value = "";
    } else {
      Objects.requireNonNull(value, "value");
    }
  }

  public String normalizedKey() {
    return switch (kind) {
      case STRING -> "s:" + value;
      case INTEGER -> "i:" + value;
      case NUMBER -> "n:" + new BigDecimal(value).stripTrailingZeros().toPlainString();
      case BOOLEAN -> "b:" + value;
      case NULL -> "z:null";
    };
  }
}
