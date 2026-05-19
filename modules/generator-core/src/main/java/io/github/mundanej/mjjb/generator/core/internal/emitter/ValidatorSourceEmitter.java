package io.github.mundanej.mjjb.generator.core.internal.emitter;

import io.github.mundanej.mjjb.generator.core.internal.binding.BindingModel;
import io.github.mundanej.mjjb.generator.core.internal.binding.FacetConstraints;
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
    for (String importName : imports(model)) {
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
    if (hasMinLengthFacet(model)) {
      lines.addAll(validateMinLengthHelper());
    }
    if (hasMaxLengthFacet(model)) {
      lines.addAll(validateMaxLengthHelper());
    }
    if (hasPatternFacet(model)) {
      lines.addAll(validatePatternHelper());
    }
    if (hasFormatFacet(model)) {
      lines.addAll(validateFormatHelper());
    }
    if (hasMinimumFacet(model)) {
      lines.addAll(validateMinimumHelper());
    }
    if (hasMaximumFacet(model)) {
      lines.addAll(validateMaximumHelper());
    }
    if (hasExclusiveMinimumFacet(model)) {
      lines.addAll(validateExclusiveMinimumHelper());
    }
    if (hasExclusiveMaximumFacet(model)) {
      lines.addAll(validateExclusiveMaximumHelper());
    }
    lines.add("}");
    lines.add("");
    return String.join("\n", lines);
  }

  public static String validatorTypeName(BindingModel model) {
    Objects.requireNonNull(model, "model");
    return model.rootTypeName() + "JsonValidator";
  }

  private static List<String> imports(BindingModel model) {
    Set<String> imports = new TreeSet<>();
    imports.add("io.github.mundanej.mjjb.runtime.JsonPath");
    imports.add("io.github.mundanej.mjjb.runtime.ValidationError");
    imports.add("io.github.mundanej.mjjb.runtime.ValidationErrors");
    imports.add("io.github.mundanej.mjjb.runtime.ValidationMode");
    imports.add("io.github.mundanej.mjjb.runtime.ValidationResult");
    if (hasNumericFacet(model)) {
      imports.add("java.math.BigDecimal");
    }
    imports.add("java.util.Objects");
    if (hasFormatFacet(model)) {
      imports.add("java.time.LocalDate");
      imports.add("java.time.OffsetDateTime");
      imports.add("java.time.format.DateTimeParseException");
      imports.add("java.util.UUID");
    }
    if (hasPatternFacet(model)) {
      imports.add("java.util.regex.Pattern");
    }
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
    if (field.scalarType() == JavaScalarType.STRING) {
      lines.addAll(validateStringFacetLines(field, scalarValueExpression(field)));
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
    if (field.scalarType() == JavaScalarType.INTEGER
        || field.scalarType() == JavaScalarType.NUMBER) {
      lines.addAll(validateNumericFacetLines(field, scalarValueExpression(field)));
    }
    return lines;
  }

  private static List<String> validateArrayLines(FieldBinding field) {
    ArrayList<String> lines = new ArrayList<>();
    if (field.valueType().minItems().isEmpty()
        && field.valueType().maxItems().isEmpty()
        && field.scalarType() != JavaScalarType.NUMBER
        && !field.valueType().facets().hasStringFacets()
        && !field.valueType().facets().hasNumericFacets()) {
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
      lines.addAll(
          indent(
              validateNumericFacetLines(
                  field, "item", "Double.isFinite(item)", itemPathExpression(field)),
              "      "));
      lines.add("        index++;");
      lines.add("      }");
    } else if (field.scalarType() == JavaScalarType.INTEGER
        && field.valueType().facets().hasNumericFacets()) {
      lines.add("      int index = 0;");
      lines.add("      for (Long item : " + valueExpression + ") {");
      lines.addAll(
          indent(
              validateNumericFacetLines(field, "item", "true", itemPathExpression(field)),
              "      "));
      lines.add("        index++;");
      lines.add("      }");
    } else if (field.scalarType() == JavaScalarType.STRING
        && field.valueType().facets().hasStringFacets()) {
      lines.add("      int index = 0;");
      lines.add("      for (String item : " + valueExpression + ") {");
      lines.addAll(
          indent(
              validateStringFacetLines(field, "item", "true", itemPathExpression(field)),
              "      "));
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

  private static List<String> validateStringFacetLines(FieldBinding field, String valueExpression) {
    return validateStringFacetLines(
        field, valueExpression, scalarGuard(field), propertyPathExpression(field));
  }

  private static List<String> validateStringFacetLines(
      FieldBinding field, String valueExpression, String guard, String pathExpression) {
    ArrayList<String> lines = new ArrayList<>();
    FacetConstraints facets = field.valueType().facets();
    if (!facets.hasStringFacets()) {
      return lines;
    }
    lines.add("    if (" + guard + ") {");
    if (facets.minLength().isPresent()) {
      lines.add(
          "      if (!validateMinLength(errors, "
              + valueExpression
              + ", "
              + facets.minLength().getAsLong()
              + "L, "
              + pathExpression
              + ")) {");
      lines.add("        return errors.toResult();");
      lines.add("      }");
    }
    if (facets.maxLength().isPresent()) {
      lines.add(
          "      if (!validateMaxLength(errors, "
              + valueExpression
              + ", "
              + facets.maxLength().getAsLong()
              + "L, "
              + pathExpression
              + ")) {");
      lines.add("        return errors.toResult();");
      lines.add("      }");
    }
    if (facets.pattern().isPresent()) {
      lines.add(
          "      if (!validatePattern(errors, "
              + valueExpression
              + ", "
              + javaStringLiteral(facets.pattern().orElseThrow())
              + ", "
              + pathExpression
              + ")) {");
      lines.add("        return errors.toResult();");
      lines.add("      }");
    }
    if (facets.format().isPresent()) {
      lines.add(
          "      if (!validateFormat(errors, "
              + valueExpression
              + ", "
              + javaStringLiteral(facets.format().orElseThrow())
              + ", "
              + pathExpression
              + ")) {");
      lines.add("        return errors.toResult();");
      lines.add("      }");
    }
    lines.add("    }");
    return lines;
  }

  private static List<String> validateNumericFacetLines(
      FieldBinding field, String valueExpression) {
    return validateNumericFacetLines(
        field,
        valueExpression,
        numericGuard(field, valueExpression),
        propertyPathExpression(field));
  }

  private static List<String> validateNumericFacetLines(
      FieldBinding field, String valueExpression, String guard, String pathExpression) {
    ArrayList<String> lines = new ArrayList<>();
    FacetConstraints facets = field.valueType().facets();
    if (!facets.hasNumericFacets()) {
      return lines;
    }
    String numericValue = numericValueExpression(field.scalarType(), valueExpression);
    lines.add("    if (" + guard + ") {");
    if (facets.minimum().isPresent()) {
      lines.add(
          "      if (!validateMinimum(errors, "
              + numericValue
              + ", "
              + javaStringLiteral(facets.minimum().orElseThrow())
              + ", "
              + pathExpression
              + ")) {");
      lines.add("        return errors.toResult();");
      lines.add("      }");
    }
    if (facets.maximum().isPresent()) {
      lines.add(
          "      if (!validateMaximum(errors, "
              + numericValue
              + ", "
              + javaStringLiteral(facets.maximum().orElseThrow())
              + ", "
              + pathExpression
              + ")) {");
      lines.add("        return errors.toResult();");
      lines.add("      }");
    }
    if (facets.exclusiveMinimum().isPresent()) {
      lines.add(
          "      if (!validateExclusiveMinimum(errors, "
              + numericValue
              + ", "
              + javaStringLiteral(facets.exclusiveMinimum().orElseThrow())
              + ", "
              + pathExpression
              + ")) {");
      lines.add("        return errors.toResult();");
      lines.add("      }");
    }
    if (facets.exclusiveMaximum().isPresent()) {
      lines.add(
          "      if (!validateExclusiveMaximum(errors, "
              + numericValue
              + ", "
              + javaStringLiteral(facets.exclusiveMaximum().orElseThrow())
              + ", "
              + pathExpression
              + ")) {");
      lines.add("        return errors.toResult();");
      lines.add("      }");
    }
    lines.add("    }");
    return lines;
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

  private static String scalarValueExpression(FieldBinding field) {
    if (field.required()) {
      return "value." + field.javaFieldName() + "()";
    }
    return "value." + field.javaFieldName() + "().orElseThrow()";
  }

  private static String scalarGuard(FieldBinding field) {
    if (field.required() && field.scalarType() == JavaScalarType.STRING) {
      return "value." + field.javaFieldName() + "() != null";
    }
    if (field.required()) {
      return "true";
    }
    return "value."
        + field.javaFieldName()
        + "() != null && value."
        + field.javaFieldName()
        + "().isPresent()";
  }

  private static String numericGuard(FieldBinding field, String valueExpression) {
    if (field.scalarType() == JavaScalarType.NUMBER) {
      String finiteGuard = "Double.isFinite(" + valueExpression + ")";
      if (field.required()) {
        return finiteGuard;
      }
      return scalarGuard(field) + " && " + finiteGuard;
    }
    return scalarGuard(field);
  }

  private static String numericValueExpression(JavaScalarType scalarType, String valueExpression) {
    return switch (scalarType) {
      case INTEGER, NUMBER -> "BigDecimal.valueOf(" + valueExpression + ")";
      case STRING, BOOLEAN ->
          throw new IllegalArgumentException("numeric facets are not valid for " + scalarType);
    };
  }

  private static String propertyPathExpression(FieldBinding field) {
    return "JsonPath.ROOT.property(" + javaStringLiteral(field.jsonPropertyName()) + ")";
  }

  private static String itemPathExpression(FieldBinding field) {
    return propertyPathExpression(field) + ".index(index)";
  }

  private static List<String> indent(List<String> lines, String indent) {
    return lines.stream().map(line -> indent + line).toList();
  }

  private static boolean hasMinLengthFacet(BindingModel model) {
    return model.rootObject().fields().stream()
        .anyMatch(field -> field.valueType().facets().minLength().isPresent());
  }

  private static boolean hasMaxLengthFacet(BindingModel model) {
    return model.rootObject().fields().stream()
        .anyMatch(field -> field.valueType().facets().maxLength().isPresent());
  }

  private static boolean hasPatternFacet(BindingModel model) {
    return model.rootObject().fields().stream()
        .anyMatch(field -> field.valueType().facets().pattern().isPresent());
  }

  private static boolean hasFormatFacet(BindingModel model) {
    return model.rootObject().fields().stream()
        .anyMatch(field -> field.valueType().facets().format().isPresent());
  }

  private static boolean hasMinimumFacet(BindingModel model) {
    return model.rootObject().fields().stream()
        .anyMatch(field -> field.valueType().facets().minimum().isPresent());
  }

  private static boolean hasMaximumFacet(BindingModel model) {
    return model.rootObject().fields().stream()
        .anyMatch(field -> field.valueType().facets().maximum().isPresent());
  }

  private static boolean hasExclusiveMinimumFacet(BindingModel model) {
    return model.rootObject().fields().stream()
        .anyMatch(field -> field.valueType().facets().exclusiveMinimum().isPresent());
  }

  private static boolean hasExclusiveMaximumFacet(BindingModel model) {
    return model.rootObject().fields().stream()
        .anyMatch(field -> field.valueType().facets().exclusiveMaximum().isPresent());
  }

  private static boolean hasNumericFacet(BindingModel model) {
    return model.rootObject().fields().stream()
        .anyMatch(field -> field.valueType().facets().hasNumericFacets());
  }

  private static List<String> validateMinLengthHelper() {
    return List.of(
        "",
        "  private static boolean validateMinLength(",
        "      ValidationErrors errors, String value, long minLength, JsonPath path) {",
        "    if (value.codePointCount(0, value.length()) >= minLength) {",
        "      return true;",
        "    }",
        "    return errors.add(",
        "        ValidationError.of(",
        "            \"MJJBV-007\",",
        "            \"Expected at least \" + minLength + \" string code points.\",",
        "            path));",
        "  }");
  }

  private static List<String> validateMaxLengthHelper() {
    return List.of(
        "",
        "  private static boolean validateMaxLength(",
        "      ValidationErrors errors, String value, long maxLength, JsonPath path) {",
        "    if (value.codePointCount(0, value.length()) <= maxLength) {",
        "      return true;",
        "    }",
        "    return errors.add(",
        "        ValidationError.of(",
        "            \"MJJBV-008\",",
        "            \"Expected at most \" + maxLength + \" string code points.\",",
        "            path));",
        "  }");
  }

  private static List<String> validatePatternHelper() {
    return List.of(
        "",
        "  private static boolean validatePattern(",
        "      ValidationErrors errors, String value, String pattern, JsonPath path) {",
        "    if (Pattern.compile(pattern).matcher(value).find()) {",
        "      return true;",
        "    }",
        "    return errors.add(",
        "        ValidationError.of(\"MJJBV-009\", \"Expected string to match pattern.\", path));",
        "  }");
  }

  private static List<String> validateFormatHelper() {
    return List.of(
        "",
        "  private static boolean validateFormat(",
        "      ValidationErrors errors, String value, String format, JsonPath path) {",
        "    boolean valid =",
        "        switch (format) {",
        "          case \"date\" -> isDate(value);",
        "          case \"date-time\" -> isDateTime(value);",
        "          case \"uuid\" -> isUuid(value);",
        "          default -> true;",
        "        };",
        "    if (valid) {",
        "      return true;",
        "    }",
        "    return errors.add(",
        "        ValidationError.of(",
        "            \"MJJBV-010\", \"Expected string to match format '\" + format + \"'.\", path));",
        "  }",
        "",
        "  private static boolean isDate(String value) {",
        "    try {",
        "      LocalDate.parse(value);",
        "      return true;",
        "    } catch (DateTimeParseException exception) {",
        "      return false;",
        "    }",
        "  }",
        "",
        "  private static boolean isDateTime(String value) {",
        "    try {",
        "      OffsetDateTime.parse(value);",
        "      return true;",
        "    } catch (DateTimeParseException exception) {",
        "      return false;",
        "    }",
        "  }",
        "",
        "  private static boolean isUuid(String value) {",
        "    try {",
        "      UUID.fromString(value);",
        "      return true;",
        "    } catch (IllegalArgumentException exception) {",
        "      return false;",
        "    }",
        "  }");
  }

  private static List<String> validateMinimumHelper() {
    return List.of(
        "",
        "  private static boolean validateMinimum(",
        "      ValidationErrors errors, BigDecimal value, String minimum, JsonPath path) {",
        "    if (value.compareTo(new BigDecimal(minimum)) >= 0) {",
        "      return true;",
        "    }",
        "    return errors.add(",
        "        ValidationError.of(",
        "            \"MJJBV-011\", \"Expected number to be at least \" + minimum + \".\", path));",
        "  }");
  }

  private static List<String> validateMaximumHelper() {
    return List.of(
        "",
        "  private static boolean validateMaximum(",
        "      ValidationErrors errors, BigDecimal value, String maximum, JsonPath path) {",
        "    if (value.compareTo(new BigDecimal(maximum)) <= 0) {",
        "      return true;",
        "    }",
        "    return errors.add(",
        "        ValidationError.of(",
        "            \"MJJBV-012\", \"Expected number to be at most \" + maximum + \".\", path));",
        "  }");
  }

  private static List<String> validateExclusiveMinimumHelper() {
    return List.of(
        "",
        "  private static boolean validateExclusiveMinimum(",
        "      ValidationErrors errors, BigDecimal value, String exclusiveMinimum, JsonPath path) {",
        "    if (value.compareTo(new BigDecimal(exclusiveMinimum)) > 0) {",
        "      return true;",
        "    }",
        "    return errors.add(",
        "        ValidationError.of(",
        "            \"MJJBV-013\",",
        "            \"Expected number to be greater than \" + exclusiveMinimum + \".\",",
        "            path));",
        "  }");
  }

  private static List<String> validateExclusiveMaximumHelper() {
    return List.of(
        "",
        "  private static boolean validateExclusiveMaximum(",
        "      ValidationErrors errors, BigDecimal value, String exclusiveMaximum, JsonPath path) {",
        "    if (value.compareTo(new BigDecimal(exclusiveMaximum)) < 0) {",
        "      return true;",
        "    }",
        "    return errors.add(",
        "        ValidationError.of(",
        "            \"MJJBV-014\",",
        "            \"Expected number to be less than \" + exclusiveMaximum + \".\",",
        "            path));",
        "  }");
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
