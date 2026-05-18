package io.github.mundanej.mjjb.schema.model;

import java.util.List;
import java.util.Objects;

/** JSON syntax value in a schema document with an exact schema JSON Pointer. */
public sealed interface SchemaSyntaxValue
    permits SchemaSyntaxValue.ObjectValue,
        SchemaSyntaxValue.ArrayValue,
        SchemaSyntaxValue.StringValue,
        SchemaSyntaxValue.NumberValue,
        SchemaSyntaxValue.BooleanValue,
        SchemaSyntaxValue.NullValue {
  JsonPointer pointer();

  /** Object value with source-order members. */
  record ObjectValue(JsonPointer pointer, List<Member> members) implements SchemaSyntaxValue {
    public ObjectValue {
      Objects.requireNonNull(pointer, "pointer");
      members = List.copyOf(Objects.requireNonNull(members, "members"));
    }
  }

  /** Object member name and value. */
  record Member(String name, SchemaSyntaxValue value) {
    public Member {
      Objects.requireNonNull(name, "name");
      Objects.requireNonNull(value, "value");
    }

    public JsonPointer pointer() {
      return value.pointer();
    }
  }

  /** Array value with source-order items. */
  record ArrayValue(JsonPointer pointer, List<SchemaSyntaxValue> items)
      implements SchemaSyntaxValue {
    public ArrayValue {
      Objects.requireNonNull(pointer, "pointer");
      items = List.copyOf(Objects.requireNonNull(items, "items"));
    }
  }

  /** String value. */
  record StringValue(JsonPointer pointer, String value) implements SchemaSyntaxValue {
    public StringValue {
      Objects.requireNonNull(pointer, "pointer");
      Objects.requireNonNull(value, "value");
    }
  }

  /** Number value stored as its source literal. */
  record NumberValue(JsonPointer pointer, String literal) implements SchemaSyntaxValue {
    public NumberValue {
      Objects.requireNonNull(pointer, "pointer");
      Objects.requireNonNull(literal, "literal");
      if (literal.isBlank()) {
        throw new IllegalArgumentException("literal must not be blank");
      }
    }
  }

  /** Boolean value. */
  record BooleanValue(JsonPointer pointer, boolean value) implements SchemaSyntaxValue {}

  /** Null value. */
  record NullValue(JsonPointer pointer) implements SchemaSyntaxValue {
    public NullValue {
      Objects.requireNonNull(pointer, "pointer");
    }
  }
}
