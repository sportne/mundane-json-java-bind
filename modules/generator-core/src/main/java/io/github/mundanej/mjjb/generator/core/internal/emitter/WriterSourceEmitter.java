package io.github.mundanej.mjjb.generator.core.internal.emitter;

import io.github.mundanej.mjjb.generator.core.internal.binding.BindingModel;
import io.github.mundanej.mjjb.generator.core.internal.binding.FieldBinding;
import io.github.mundanej.mjjb.generator.core.internal.binding.JavaScalarType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Emits Java writer source from binding IR. */
public final class WriterSourceEmitter {
  public String emit(BindingModel model) {
    Objects.requireNonNull(model, "model");
    ArrayList<String> lines = new ArrayList<>();
    lines.add("package " + model.packageName() + ";");
    lines.add("");
    for (String importName : imports()) {
      lines.add("import " + importName + ";");
    }
    lines.add("");
    lines.add("public final class " + writerTypeName(model) + " {");
    lines.add("  private " + writerTypeName(model) + "() {}");
    lines.add("");
    lines.add(
        "  public static void write(JsonWriter writer, "
            + model.rootTypeName()
            + " value) throws JsonWriteException {");
    lines.add("    Objects.requireNonNull(writer, \"writer\");");
    lines.add("    Objects.requireNonNull(value, \"value\");");
    lines.addAll(numberPreflightLines(model));
    lines.add("    writer.beginObject();");
    for (FieldBinding field : model.rootObject().fields()) {
      lines.addAll(writeFieldLines(field));
    }
    lines.add("    writer.endObject();");
    lines.add("  }");
    if (hasNumberField(model)) {
      lines.add("");
      lines.add(
          "  private static void requireFinite(double value, String name)"
              + " throws JsonWriteException {");
      lines.add("    if (!Double.isFinite(value)) {");
      lines.add(
          "      throw new JsonWriteException("
              + "\"JSON number field '\" + name + \"' must be finite.\");");
      lines.add("    }");
      lines.add("  }");
    }
    lines.add("}");
    lines.add("");
    return String.join("\n", lines);
  }

  public static String writerTypeName(BindingModel model) {
    Objects.requireNonNull(model, "model");
    return model.rootTypeName() + "JsonWriter";
  }

  private static List<String> imports() {
    Set<String> imports = new TreeSet<>();
    imports.add("io.github.mundanej.mjjb.runtime.JsonWriteException");
    imports.add("io.github.mundanej.mjjb.runtime.JsonWriter");
    imports.add("java.util.Objects");
    return List.copyOf(imports);
  }

  private static List<String> numberPreflightLines(BindingModel model) {
    ArrayList<String> lines = new ArrayList<>();
    for (FieldBinding field : model.rootObject().fields()) {
      if (field.scalarType() != JavaScalarType.NUMBER) {
        continue;
      }
      if (field.array() && field.required()) {
        lines.add("    for (Double item : value." + field.javaFieldName() + "()) {");
        lines.add(
            "      requireFinite(item, " + javaStringLiteral(field.jsonPropertyName()) + ");");
        lines.add("    }");
      } else if (field.array()) {
        lines.add("    if (value." + field.javaFieldName() + "().isPresent()) {");
        lines.add(
            "      for (Double item : value." + field.javaFieldName() + "().orElseThrow()) {");
        lines.add(
            "        requireFinite(item, " + javaStringLiteral(field.jsonPropertyName()) + ");");
        lines.add("      }");
        lines.add("    }");
      } else if (field.required()) {
        lines.add(
            "    requireFinite(value."
                + field.javaFieldName()
                + "(), "
                + javaStringLiteral(field.jsonPropertyName())
                + ");");
      } else {
        lines.add("    if (value." + field.javaFieldName() + "().isPresent()) {");
        lines.add(
            "      requireFinite(value."
                + field.javaFieldName()
                + "().orElseThrow(), "
                + javaStringLiteral(field.jsonPropertyName())
                + ");");
        lines.add("    }");
      }
    }
    return lines;
  }

  private static List<String> writeFieldLines(FieldBinding field) {
    if (field.required()) {
      return requiredFieldLines(field);
    }
    return optionalFieldLines(field);
  }

  private static List<String> requiredFieldLines(FieldBinding field) {
    if (field.array()) {
      ArrayList<String> lines = new ArrayList<>();
      lines.add("    writer.name(" + javaStringLiteral(field.jsonPropertyName()) + ");");
      lines.addAll(writeArrayLines(field, "value." + field.javaFieldName() + "()", "    "));
      return lines;
    }
    return List.of(
        "    writer.name(" + javaStringLiteral(field.jsonPropertyName()) + ");",
        "    " + writeValueStatement(field, "value." + field.javaFieldName() + "()"));
  }

  private static List<String> optionalFieldLines(FieldBinding field) {
    ArrayList<String> lines = new ArrayList<>();
    String fieldName = field.javaFieldName();
    lines.add("    if (value." + fieldName + "().isPresent()) {");
    lines.add("      writer.name(" + javaStringLiteral(field.jsonPropertyName()) + ");");
    if (field.array()) {
      lines.addAll(writeArrayLines(field, "value." + fieldName + "().orElseThrow()", "      "));
    } else {
      lines.add("      " + writeValueStatement(field, "value." + fieldName + "().orElseThrow()"));
    }
    lines.add("    }");
    return lines;
  }

  private static String writeValueStatement(FieldBinding field, String valueExpression) {
    return writeScalarStatement(field.scalarType(), valueExpression);
  }

  private static List<String> writeArrayLines(
      FieldBinding field, String valueExpression, String indent) {
    return List.of(
        indent + "writer.beginArray();",
        indent
            + "for ("
            + field.scalarType().boxedJavaType()
            + " item : "
            + valueExpression
            + ") {",
        indent + "  " + writeScalarStatement(field.scalarType(), "item"),
        indent + "}",
        indent + "writer.endArray();");
  }

  private static String writeScalarStatement(JavaScalarType scalarType, String valueExpression) {
    return switch (scalarType) {
      case STRING -> "writer.value(" + valueExpression + ");";
      case INTEGER -> "writer.number(Long.toString(" + valueExpression + "));";
      case NUMBER -> "writer.number(Double.toString(" + valueExpression + "));";
      case BOOLEAN -> "writer.value(" + valueExpression + ");";
    };
  }

  private static boolean hasNumberField(BindingModel model) {
    return model.rootObject().fields().stream()
        .anyMatch(field -> field.scalarType() == JavaScalarType.NUMBER);
  }

  private static String javaStringLiteral(String value) {
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
