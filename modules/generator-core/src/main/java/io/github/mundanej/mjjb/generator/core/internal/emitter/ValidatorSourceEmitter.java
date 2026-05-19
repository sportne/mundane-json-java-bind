package io.github.mundanej.mjjb.generator.core.internal.emitter;

import io.github.mundanej.mjjb.generator.core.internal.binding.BindingModel;
import io.github.mundanej.mjjb.generator.core.internal.binding.FieldBinding;
import io.github.mundanej.mjjb.generator.core.internal.binding.JavaScalarType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Emits Java validator source from binding IR. */
public final class ValidatorSourceEmitter {
  public String emit(BindingModel model) {
    Objects.requireNonNull(model, "model");
    ArrayList<String> lines = new ArrayList<>();
    lines.add("package " + model.packageName() + ";");
    lines.add("");
    for (String importName : imports()) {
      lines.add("import " + importName + ";");
    }
    lines.add("");
    lines.add("public final class " + validatorTypeName(model) + " {");
    lines.add("  private " + validatorTypeName(model) + "() {}");
    lines.add("");
    lines.add("  public static ValidationResult validate(" + model.rootTypeName() + " value) {");
    lines.add("    return validate(value, ValidationMode.ACCUMULATE);");
    lines.add("  }");
    lines.add("");
    lines.add(
        "  public static ValidationResult validate("
            + model.rootTypeName()
            + " value, ValidationMode mode) {");
    lines.add("    Objects.requireNonNull(mode, \"mode\");");
    lines.add("    ValidationErrors errors = ValidationErrors.create(mode);");
    lines.add("    if (value == null) {");
    lines.add(
        "      errors.add(ValidationError.of("
            + "\"MJJBV-001\", \"Expected generated object value.\", JsonPath.ROOT));");
    lines.add("      return errors.toResult();");
    lines.add("    }");
    for (FieldBinding field : model.rootObject().fields()) {
      lines.addAll(validateFieldLines(field));
    }
    lines.add("    return errors.toResult();");
    lines.add("  }");
    if (hasNumberField(model)) {
      lines.add("");
      lines.add(
          "  private static boolean validateFinite("
              + "ValidationErrors errors, double value, JsonPath path) {");
      lines.add("    if (Double.isFinite(value)) {");
      lines.add("      return true;");
      lines.add("    }");
      lines.add(
          "    return errors.add("
              + "ValidationError.of(\"MJJBV-004\", \"Expected finite JSON number.\", path));");
      lines.add("  }");
    }
    if (hasArrayWithMinItems(model)) {
      lines.add("");
      lines.add(
          "  private static boolean validateMinItems("
              + "ValidationErrors errors, int size, long minItems, JsonPath path) {");
      lines.add("    if (size >= minItems) {");
      lines.add("      return true;");
      lines.add("    }");
      lines.add(
          "    return errors.add("
              + "ValidationError.of(\"MJJBV-005\", \"Expected at least \" + minItems + "
              + "\" array items.\", path));");
      lines.add("  }");
    }
    if (hasArrayWithMaxItems(model)) {
      lines.add("");
      lines.add(
          "  private static boolean validateMaxItems("
              + "ValidationErrors errors, int size, long maxItems, JsonPath path) {");
      lines.add("    if (size <= maxItems) {");
      lines.add("      return true;");
      lines.add("    }");
      lines.add(
          "    return errors.add("
              + "ValidationError.of(\"MJJBV-006\", \"Expected at most \" + maxItems + "
              + "\" array items.\", path));");
      lines.add("  }");
    }
    lines.add("}");
    lines.add("");
    return String.join("\n", lines);
  }

  public static String validatorTypeName(BindingModel model) {
    Objects.requireNonNull(model, "model");
    return model.rootTypeName() + "JsonValidator";
  }

  private static List<String> imports() {
    Set<String> imports = new TreeSet<>();
    imports.add("io.github.mundanej.mjjb.runtime.JsonPath");
    imports.add("io.github.mundanej.mjjb.runtime.ValidationError");
    imports.add("io.github.mundanej.mjjb.runtime.ValidationErrors");
    imports.add("io.github.mundanej.mjjb.runtime.ValidationMode");
    imports.add("io.github.mundanej.mjjb.runtime.ValidationResult");
    imports.add("java.util.Objects");
    return List.copyOf(imports);
  }

  private static List<String> validateFieldLines(FieldBinding field) {
    ArrayList<String> lines = new ArrayList<>();
    if (field.required()
        && (field.array() || "String".equals(field.scalarType().requiredJavaType()))) {
      lines.add("    if (value." + field.javaFieldName() + "() == null) {");
      lines.add(
          "      if (!errors.add("
              + "ValidationError.of(\"MJJBV-002\", \"Required property '"
              + field.jsonPropertyName()
              + "' must not be null.\", "
              + propertyPathExpression(field)
              + "))) {");
      lines.add("        return errors.toResult();");
      lines.add("      }");
      lines.add("    }");
    }
    if (!field.required()) {
      lines.add("    if (value." + field.javaFieldName() + "() == null) {");
      lines.add(
          "      if (!errors.add("
              + "ValidationError.of(\"MJJBV-003\", \"Optional property container '"
              + field.jsonPropertyName()
              + "' must not be null.\", "
              + propertyPathExpression(field)
              + "))) {");
      lines.add("        return errors.toResult();");
      lines.add("      }");
      lines.add("    }");
    }
    if (field.array()) {
      lines.addAll(validateArrayLines(field));
      return lines;
    }
    if (field.scalarType() == JavaScalarType.NUMBER) {
      if (field.required()) {
        lines.add(
            "    if (!validateFinite(errors, value."
                + field.javaFieldName()
                + "(), "
                + propertyPathExpression(field)
                + ")) {");
        lines.add("      return errors.toResult();");
        lines.add("    }");
      } else {
        lines.add(
            "    if (value."
                + field.javaFieldName()
                + "() != null && value."
                + field.javaFieldName()
                + "().isPresent()) {");
        lines.add(
            "      if (!validateFinite(errors, value."
                + field.javaFieldName()
                + "().orElseThrow(), "
                + propertyPathExpression(field)
                + ")) {");
        lines.add("        return errors.toResult();");
        lines.add("      }");
        lines.add("    }");
      }
    }
    return lines;
  }

  private static List<String> validateArrayLines(FieldBinding field) {
    ArrayList<String> lines = new ArrayList<>();
    if (field.valueType().minItems().isEmpty()
        && field.valueType().maxItems().isEmpty()
        && field.scalarType() != JavaScalarType.NUMBER) {
      return lines;
    }
    String valueExpression = arrayValueExpression(field);
    String guard = arrayGuard(field);
    lines.add("    if (" + guard + ") {");
    if (field.valueType().minItems().isPresent()) {
      lines.add(
          "      if (!validateMinItems(errors, "
              + valueExpression
              + ".size(), "
              + field.valueType().minItems().getAsLong()
              + "L, "
              + propertyPathExpression(field)
              + ")) {");
      lines.add("        return errors.toResult();");
      lines.add("      }");
    }
    if (field.valueType().maxItems().isPresent()) {
      lines.add(
          "      if (!validateMaxItems(errors, "
              + valueExpression
              + ".size(), "
              + field.valueType().maxItems().getAsLong()
              + "L, "
              + propertyPathExpression(field)
              + ")) {");
      lines.add("        return errors.toResult();");
      lines.add("      }");
    }
    if (field.scalarType() == JavaScalarType.NUMBER) {
      lines.add("      int index = 0;");
      lines.add("      for (Double item : " + valueExpression + ") {");
      lines.add(
          "        if (!validateFinite(errors, item, "
              + propertyPathExpression(field)
              + ".index(index))) {");
      lines.add("          return errors.toResult();");
      lines.add("        }");
      lines.add("        index++;");
      lines.add("      }");
    }
    lines.add("    }");
    return lines;
  }

  private static boolean hasNumberField(BindingModel model) {
    return model.rootObject().fields().stream()
        .anyMatch(field -> field.scalarType() == JavaScalarType.NUMBER);
  }

  private static boolean hasArrayWithMinItems(BindingModel model) {
    return model.rootObject().fields().stream()
        .anyMatch(field -> field.array() && field.valueType().minItems().isPresent());
  }

  private static boolean hasArrayWithMaxItems(BindingModel model) {
    return model.rootObject().fields().stream()
        .anyMatch(field -> field.array() && field.valueType().maxItems().isPresent());
  }

  private static String arrayGuard(FieldBinding field) {
    if (field.required()) {
      return "value." + field.javaFieldName() + "() != null";
    }
    return "value."
        + field.javaFieldName()
        + "() != null && value."
        + field.javaFieldName()
        + "().isPresent()";
  }

  private static String arrayValueExpression(FieldBinding field) {
    if (field.required()) {
      return "value." + field.javaFieldName() + "()";
    }
    return "value." + field.javaFieldName() + "().orElseThrow()";
  }

  private static String propertyPathExpression(FieldBinding field) {
    return "JsonPath.ROOT.property(" + javaStringLiteral(field.jsonPropertyName()) + ")";
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
