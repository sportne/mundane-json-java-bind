package io.github.mundanej.mjjb.generator.core.internal.emitter;

import java.util.List;
import java.util.Objects;

final class JavaSourceText {
  private JavaSourceText() {}

  static List<String> indent(List<String> lines, String indent) {
    Objects.requireNonNull(lines, "lines");
    Objects.requireNonNull(indent, "indent");
    return lines.stream().map(line -> line.isEmpty() ? line : indent + line).toList();
  }

  static List<String> indentAll(List<String> lines, String indent) {
    Objects.requireNonNull(lines, "lines");
    Objects.requireNonNull(indent, "indent");
    return lines.stream().map(line -> indent + line).toList();
  }

  static String stringLiteral(String value) {
    Objects.requireNonNull(value, "value");
    StringBuilder literal = new StringBuilder("\"");
    for (int index = 0; index < value.length(); index++) {
      char current = value.charAt(index);
      switch (current) {
        case '"' -> literal.append("\\\"");
        case '\\' -> literal.append("\\\\");
        case '\b' -> literal.append("\\b");
        case '\f' -> literal.append("\\f");
        case '\n' -> literal.append("\\n");
        case '\r' -> literal.append("\\r");
        case '\t' -> literal.append("\\t");
        default -> {
          if (current < 0x20) {
            literal.append(String.format("\\u%04x", (int) current));
          } else {
            literal.append(current);
          }
        }
      }
    }
    literal.append('"');
    return literal.toString();
  }
}
