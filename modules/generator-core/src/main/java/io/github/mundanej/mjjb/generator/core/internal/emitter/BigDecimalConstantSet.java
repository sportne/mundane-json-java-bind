package io.github.mundanej.mjjb.generator.core.internal.emitter;

import static io.github.mundanej.mjjb.generator.core.internal.emitter.JavaSourceText.stringLiteral;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class BigDecimalConstantSet {
  private final Map<String, String> names = new LinkedHashMap<>();

  void add(String literal) {
    Objects.requireNonNull(literal, "literal");
    names.computeIfAbsent(literal, ignored -> "DECIMAL_" + (names.size() + 1));
  }

  boolean empty() {
    return names.isEmpty();
  }

  List<String> declarations(String indent) {
    ArrayList<String> lines = new ArrayList<>();
    for (Map.Entry<String, String> entry : names.entrySet()) {
      lines.add(
          indent
              + "private static final BigDecimal "
              + entry.getValue()
              + " = new BigDecimal("
              + stringLiteral(entry.getKey())
              + ");");
    }
    return List.copyOf(lines);
  }

  List<String> lookupCases(String indent) {
    ArrayList<String> lines = new ArrayList<>();
    for (Map.Entry<String, String> entry : names.entrySet()) {
      lines.add(indent + "case " + stringLiteral(entry.getKey()) + " -> " + entry.getValue() + ";");
    }
    return List.copyOf(lines);
  }
}
