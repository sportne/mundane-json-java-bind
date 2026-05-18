package io.github.mundanej.mjjb.parser;

import io.github.mundanej.mjjb.runtime.JsonWriteException;
import io.github.mundanej.mjjb.runtime.JsonWriter;
import java.util.ArrayDeque;
import java.util.Objects;

/** Simple deterministic JSON writer backed by a {@link StringBuilder}. */
public final class JsonStringWriter implements JsonWriter {
  private final StringBuilder output = new StringBuilder();
  private final ArrayDeque<Context> stack = new ArrayDeque<>();
  private boolean rootComplete;

  @Override
  public void beginObject() throws JsonWriteException {
    beforeValue();
    output.append('{');
    stack.push(new Context(true));
  }

  @Override
  public void endObject() throws JsonWriteException {
    Context context = requireContext(true);
    if (!context.expectingName) {
      throw new JsonWriteException("Cannot end object before writing property value.");
    }
    stack.pop();
    output.append('}');
    afterValue();
  }

  @Override
  public void beginArray() throws JsonWriteException {
    beforeValue();
    output.append('[');
    stack.push(new Context(false));
  }

  @Override
  public void endArray() throws JsonWriteException {
    requireContext(false);
    stack.pop();
    output.append(']');
    afterValue();
  }

  @Override
  public void name(String name) throws JsonWriteException {
    Objects.requireNonNull(name, "name");
    Context context = requireContext(true);
    if (!context.expectingName) {
      throw new JsonWriteException("Expected property value before next name.");
    }
    if (!context.first) {
      output.append(',');
    }
    writeString(name);
    output.append(':');
    context.expectingName = false;
  }

  @Override
  public void value(String value) throws JsonWriteException {
    Objects.requireNonNull(value, "value");
    beforeValue();
    writeString(value);
    afterValue();
  }

  @Override
  public void number(String literal) throws JsonWriteException {
    Objects.requireNonNull(literal, "literal");
    validateNumberLiteral(literal);
    beforeValue();
    output.append(literal);
    afterValue();
  }

  @Override
  public void value(boolean value) throws JsonWriteException {
    beforeValue();
    output.append(value);
    afterValue();
  }

  @Override
  public void nullValue() throws JsonWriteException {
    beforeValue();
    output.append("null");
    afterValue();
  }

  public String json() throws JsonWriteException {
    if (!rootComplete || !stack.isEmpty()) {
      throw new JsonWriteException("JSON document is not complete.");
    }
    return output.toString();
  }

  private void beforeValue() throws JsonWriteException {
    if (stack.isEmpty()) {
      if (rootComplete) {
        throw new JsonWriteException("JSON document already has a root value.");
      }
      return;
    }
    Context context = stack.peek();
    if (context.object) {
      if (context.expectingName) {
        throw new JsonWriteException("Expected property name before value.");
      }
    } else if (!context.first) {
      output.append(',');
    }
  }

  private void afterValue() {
    if (stack.isEmpty()) {
      rootComplete = true;
      return;
    }
    Context context = stack.peek();
    context.first = false;
    if (context.object) {
      context.expectingName = true;
    }
  }

  private Context requireContext(boolean object) throws JsonWriteException {
    if (stack.isEmpty() || stack.peek().object != object) {
      throw new JsonWriteException("Unexpected JSON writer structure.");
    }
    return stack.peek();
  }

  private void writeString(String value) {
    output.append('"');
    for (int i = 0; i < value.length(); i++) {
      char current = value.charAt(i);
      switch (current) {
        case '"' -> output.append("\\\"");
        case '\\' -> output.append("\\\\");
        case '\b' -> output.append("\\b");
        case '\f' -> output.append("\\f");
        case '\n' -> output.append("\\n");
        case '\r' -> output.append("\\r");
        case '\t' -> output.append("\\t");
        default -> {
          if (current < 0x20) {
            output.append(String.format("\\u%04x", (int) current));
          } else {
            output.append(current);
          }
        }
      }
    }
    output.append('"');
  }

  private static void validateNumberLiteral(String literal) throws JsonWriteException {
    int index = 0;
    if (literal.isEmpty()) {
      throw new JsonWriteException("JSON number literal must not be empty.");
    }
    if (literal.charAt(index) == '-') {
      index++;
    }
    if (index >= literal.length()) {
      throw new JsonWriteException("JSON number literal must contain digits.");
    }
    if (literal.charAt(index) == '0') {
      index++;
      if (index < literal.length() && isDigit(literal.charAt(index))) {
        throw new JsonWriteException("Leading zeroes are not valid JSON numbers.");
      }
    } else {
      index = consumeDigits(literal, index);
    }
    if (index < literal.length() && literal.charAt(index) == '.') {
      index++;
      int fractionStart = index;
      index = consumeDigits(literal, index);
      if (index == fractionStart) {
        throw new JsonWriteException("JSON number fraction must contain digits.");
      }
    }
    if (index < literal.length()
        && (literal.charAt(index) == 'e' || literal.charAt(index) == 'E')) {
      index++;
      if (index < literal.length()
          && (literal.charAt(index) == '+' || literal.charAt(index) == '-')) {
        index++;
      }
      int exponentStart = index;
      index = consumeDigits(literal, index);
      if (index == exponentStart) {
        throw new JsonWriteException("JSON number exponent must contain digits.");
      }
    }
    if (index != literal.length()) {
      throw new JsonWriteException("Invalid JSON number literal.");
    }
  }

  private static int consumeDigits(String literal, int index) throws JsonWriteException {
    if (index >= literal.length() || !isDigit(literal.charAt(index))) {
      throw new JsonWriteException("JSON number literal must contain digits.");
    }
    int next = index;
    while (next < literal.length() && isDigit(literal.charAt(next))) {
      next++;
    }
    return next;
  }

  private static boolean isDigit(char value) {
    return value >= '0' && value <= '9';
  }

  private static final class Context {
    private final boolean object;
    private boolean first = true;
    private boolean expectingName;

    private Context(boolean object) {
      this.object = object;
      this.expectingName = object;
    }
  }
}
