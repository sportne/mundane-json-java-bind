package io.github.mundanej.mjjb.generator.core.internal.emitter;

import io.github.mundanej.mjjb.generator.core.internal.binding.BindingModel;
import io.github.mundanej.mjjb.generator.core.internal.binding.FieldBinding;
import io.github.mundanej.mjjb.generator.core.internal.binding.LiteralValue;
import io.github.mundanej.mjjb.generator.core.internal.binding.TaggedUnionBranch;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Emits Java model source from binding IR. */
public final class ModelSourceEmitter {
  public String emit(BindingModel model) {
    Objects.requireNonNull(model, "model");
    ArrayList<String> lines = new ArrayList<>();
    lines.add("package " + model.packageName() + ";");
    List<String> imports = imports(model);
    if (!imports.isEmpty()) {
      lines.add("");
      for (String importName : imports) {
        lines.add("import " + importName + ";");
      }
    }
    lines.add("");
    if (model.taggedUnion().isPresent()) {
      lines.addAll(taggedUnionLines(model));
    } else {
      lines.addAll(recordLines(model));
    }
    lines.add("");
    return String.join("\n", lines);
  }

  private static List<String> imports(BindingModel model) {
    Set<String> imports = new TreeSet<>();
    for (FieldBinding field : allFields(model)) {
      if (requiresNullCheck(field)) {
        imports.add("java.util.Objects");
      }
      if (field.valueType().nullable()) {
        imports.add("io.github.mundanej.mjjb.runtime.JsonField");
      }
      if (field.array()) {
        imports.add("java.util.List");
      }
      if (!field.required()) {
        imports.add("java.util.Optional");
      }
      if (field.valueType().literals().hasDefault()) {
        imports.add("java.util.Optional");
      }
    }
    return List.copyOf(imports);
  }

  private static List<String> taggedUnionLines(BindingModel model) {
    ArrayList<String> lines = new ArrayList<>();
    List<TaggedUnionBranch> branches = model.taggedUnion().orElseThrow().branches();
    lines.add("public sealed interface " + model.rootTypeName() + " permits");
    for (int index = 0; index < branches.size(); index++) {
      String suffix = index == branches.size() - 1 ? " {" : ",";
      lines.add(
          "    "
              + model.rootTypeName()
              + "."
              + branches.get(index).object().javaTypeName()
              + suffix);
    }
    for (TaggedUnionBranch branch : branches) {
      lines.add("");
      lines.addAll(branchRecordLines(model, branch));
    }
    lines.add("}");
    return lines;
  }

  private static List<String> branchRecordLines(BindingModel model, TaggedUnionBranch branch) {
    List<FieldBinding> fields = branch.object().fields();
    String typeName = branch.object().javaTypeName();
    if (fields.isEmpty()) {
      return List.of("  record " + typeName + "() implements " + model.rootTypeName() + " {}");
    }
    ArrayList<String> lines = new ArrayList<>();
    lines.add("  record " + typeName + "(");
    for (int index = 0; index < fields.size(); index++) {
      FieldBinding field = fields.get(index);
      String suffix =
          index == fields.size() - 1 ? ") implements " + model.rootTypeName() + " {" : ",";
      lines.add("      " + javaType(field) + " " + field.javaFieldName() + suffix);
    }
    List<FieldBinding> defaultFields =
        fields.stream().filter(field -> field.valueType().literals().hasDefault()).toList();
    for (FieldBinding field : defaultFields) {
      lines.addAll(indent(defaultAccessorLines(field), "  "));
    }
    List<FieldBinding> checkedFields =
        fields.stream().filter(ModelSourceEmitter::requiresNullCheck).toList();
    if (checkedFields.isEmpty()) {
      lines.add("  }");
      return lines;
    }
    if (!defaultFields.isEmpty()) {
      lines.add("");
    }
    lines.add("    public " + typeName + " {");
    for (FieldBinding field : checkedFields) {
      for (String assignmentLine : constructorAssignmentLines(field)) {
        lines.add("      " + assignmentLine);
      }
    }
    lines.add("    }");
    lines.add("  }");
    return lines;
  }

  private static List<FieldBinding> allFields(BindingModel model) {
    if (model.taggedUnion().isEmpty()) {
      return model.rootObject().fields();
    }
    return model.taggedUnion().orElseThrow().branches().stream()
        .flatMap(branch -> branch.object().fields().stream())
        .toList();
  }

  private static List<String> indent(List<String> lines, String indent) {
    return lines.stream().map(line -> line.isEmpty() ? line : indent + line).toList();
  }

  private static List<String> recordLines(BindingModel model) {
    List<FieldBinding> fields = model.rootObject().fields();
    if (fields.isEmpty()) {
      return List.of("public record " + model.rootTypeName() + "() {}");
    }
    ArrayList<String> lines = new ArrayList<>();
    lines.add("public record " + model.rootTypeName() + "(");
    for (int index = 0; index < fields.size(); index++) {
      FieldBinding field = fields.get(index);
      String suffix = index == fields.size() - 1 ? ") {" : ",";
      lines.add("    " + javaType(field) + " " + field.javaFieldName() + suffix);
    }
    List<FieldBinding> defaultFields =
        fields.stream().filter(field -> field.valueType().literals().hasDefault()).toList();
    for (FieldBinding field : defaultFields) {
      lines.addAll(defaultAccessorLines(field));
    }
    List<FieldBinding> checkedFields =
        fields.stream().filter(ModelSourceEmitter::requiresNullCheck).toList();
    if (checkedFields.isEmpty()) {
      lines.add("}");
      return lines;
    }
    if (!defaultFields.isEmpty()) {
      lines.add("");
    }
    lines.add("  public " + model.rootTypeName() + " {");
    for (FieldBinding field : checkedFields) {
      for (String assignmentLine : constructorAssignmentLines(field)) {
        lines.add("    " + assignmentLine);
      }
    }
    lines.add("  }");
    lines.add("}");
    return lines;
  }

  private static String javaType(FieldBinding field) {
    if (field.required()) {
      return field.valueType().requiredJavaType();
    }
    return field.valueType().optionalJavaType();
  }

  private static boolean requiresNullCheck(FieldBinding field) {
    return field.valueType().nullable()
        || !field.required()
        || field.array()
        || "String".equals(field.scalarType().requiredJavaType());
  }

  private static List<String> constructorAssignmentLines(FieldBinding field) {
    String name = field.javaFieldName();
    if (field.valueType().nullable() && field.array()) {
      return List.of(
          name + " = Objects.requireNonNull(" + name + ", \"" + name + "\");",
          "if (" + name + ".hasValue()) {",
          "  " + name + " = JsonField.value(List.copyOf(" + name + ".requireValue()));",
          "}");
    }
    if (field.valueType().nullable()) {
      return List.of(name + " = Objects.requireNonNull(" + name + ", \"" + name + "\");");
    }
    if (field.array() && field.required()) {
      return List.of(
          name + " = List.copyOf(Objects.requireNonNull(" + name + ", \"" + name + "\"));");
    }
    if (field.array()) {
      return List.of(
          name + " = Objects.requireNonNull(" + name + ", \"" + name + "\").map(List::copyOf);");
    }
    return List.of(name + " = Objects.requireNonNull(" + name + ", \"" + name + "\");");
  }

  private static List<String> defaultAccessorLines(FieldBinding field) {
    LiteralValue literal = field.valueType().literals().defaultValue().orElseThrow();
    return List.of(
        "",
        "  public static Optional<"
            + field.scalarType().boxedJavaType()
            + "> default"
            + capitalized(field.javaFieldName())
            + "() {",
        "    return " + optionalLiteralExpression(literal) + ";",
        "  }");
  }

  private static String optionalLiteralExpression(LiteralValue literal) {
    return switch (literal.kind()) {
      case STRING -> "Optional.of(" + javaStringLiteral(literal.value()) + ")";
      case INTEGER -> "Optional.of(" + literal.value() + "L)";
      case NUMBER -> "Optional.of(Double.parseDouble(" + javaStringLiteral(literal.value()) + "))";
      case BOOLEAN -> "Optional.of(" + literal.value() + ")";
      case NULL -> "Optional.empty()";
    };
  }

  private static String capitalized(String value) {
    return value.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + value.substring(1);
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
