package io.github.mundanej.mjjb.generator.core.internal.emitter;

import io.github.mundanej.mjjb.generator.core.internal.binding.BindingModel;
import io.github.mundanej.mjjb.generator.core.internal.binding.FieldBinding;
import io.github.mundanej.mjjb.generator.core.internal.binding.JavaScalarType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Emits Java reader source from binding IR. */
public final class ReaderSourceEmitter {
  public String emit(BindingModel model) {
    Objects.requireNonNull(model, "model");
    ArrayList<String> lines = new ArrayList<>();
    lines.add("package " + model.packageName() + ";");
    lines.add("");
    for (String importName : imports(model)) {
      lines.add("import " + importName + ";");
    }
    lines.add("");
    lines.add("public final class " + readerTypeName(model) + " {");
    lines.add("  private " + readerTypeName(model) + "() {}");
    lines.add("");
    lines.add(
        "  public static "
            + model.rootTypeName()
            + " read(JsonReader reader) throws JsonReadException {");
    lines.add("    Objects.requireNonNull(reader, \"reader\");");
    lines.add("    if (reader.peek() != JsonToken.BEGIN_OBJECT) {");
    lines.add(
        "      throw error("
            + "\"MJJBR-001\", \"Expected root JSON object.\", JsonPath.ROOT, reader.location());");
    lines.add("    }");
    lines.add("    reader.beginObject();");
    lines.addAll(fieldInitializers(model));
    lines.add("    while (reader.hasNext()) {");
    lines.add("      String name = reader.nextName();");
    lines.add("      switch (name) {");
    for (FieldBinding field : model.rootObject().fields()) {
      lines.addAll(fieldCase(field));
    }
    lines.add("        default ->");
    lines.add(
        "            throw error("
            + "\"MJJBR-004\", \"Unknown JSON property '\" + name + \"'.\", "
            + "propertyPath(name), reader.location());");
    lines.add("      }");
    lines.add("    }");
    lines.add("    reader.endObject();");
    lines.addAll(requiredChecks(model));
    lines.add("    if (reader.peek() != JsonToken.END_DOCUMENT) {");
    lines.add(
        "      throw error("
            + "\"MJJBR-002\", \"Unexpected JSON content after root value.\", "
            + "JsonPath.ROOT, reader.location());");
    lines.add("    }");
    lines.add("    return new " + model.rootTypeName() + "(" + constructorArguments(model) + ");");
    lines.add("  }");
    lines.addAll(helperLines(model));
    lines.add("}");
    lines.add("");
    return String.join("\n", lines);
  }

  public static String readerTypeName(BindingModel model) {
    Objects.requireNonNull(model, "model");
    return model.rootTypeName() + "JsonReader";
  }

  private static List<String> imports(BindingModel model) {
    Set<String> imports = new TreeSet<>();
    imports.add("io.github.mundanej.mjjb.runtime.JsonDiagnostic");
    imports.add("io.github.mundanej.mjjb.runtime.JsonPath");
    imports.add("io.github.mundanej.mjjb.runtime.JsonReadException");
    imports.add("io.github.mundanej.mjjb.runtime.JsonReader");
    imports.add("io.github.mundanej.mjjb.runtime.JsonToken");
    imports.add("java.util.Objects");
    if (hasArrayField(model)) {
      imports.add("java.util.ArrayList");
      imports.add("java.util.List");
    }
    if (model.rootObject().fields().stream().anyMatch(field -> !field.required())) {
      imports.add("java.util.Optional");
    }
    return List.copyOf(imports);
  }

  private static List<String> fieldInitializers(BindingModel model) {
    ArrayList<String> lines = new ArrayList<>();
    for (FieldBinding field : model.rootObject().fields()) {
      if (field.required()) {
        lines.add(
            "    "
                + localJavaType(field)
                + " "
                + field.javaFieldName()
                + " = "
                + requiredDefault(field)
                + ";");
        lines.add("    boolean " + seenName(field) + " = false;");
      } else {
        lines.add(
            "    " + localJavaType(field) + " " + field.javaFieldName() + " = Optional.empty();");
        lines.add("    boolean " + seenName(field) + " = false;");
      }
    }
    return lines;
  }

  private static List<String> fieldCase(FieldBinding field) {
    ArrayList<String> lines = new ArrayList<>();
    lines.add("        case " + javaStringLiteral(field.jsonPropertyName()) + " -> {");
    lines.add("          if (" + seenName(field) + ") {");
    lines.add(
        "            throw duplicateProperty("
            + javaStringLiteral(field.jsonPropertyName())
            + ", reader.location());");
    lines.add("          }");
    if (field.required()) {
      lines.add(
          "          "
              + field.javaFieldName()
              + " = "
              + readExpression(field, propertyPathExpression(field))
              + ";");
    } else {
      lines.add(
          "          "
              + field.javaFieldName()
              + " = Optional.of("
              + readExpression(field, propertyPathExpression(field))
              + ");");
    }
    lines.add("          " + seenName(field) + " = true;");
    lines.add("        }");
    return lines;
  }

  private static List<String> requiredChecks(BindingModel model) {
    ArrayList<String> lines = new ArrayList<>();
    for (FieldBinding field : model.rootObject().fields()) {
      if (!field.required()) {
        continue;
      }
      lines.add("    if (!" + seenName(field) + ") {");
      lines.add(
          "      throw missingRequired("
              + javaStringLiteral(field.jsonPropertyName())
              + ", "
              + propertyPathExpression(field)
              + ", reader.location());");
      lines.add("    }");
    }
    return lines;
  }

  private static List<String> helperLines(BindingModel model) {
    ArrayList<String> lines = new ArrayList<>();
    lines.add("");
    lines.add("  private static JsonReadException duplicateProperty(");
    lines.add("      String name, io.github.mundanej.mjjb.runtime.JsonLocation location) {");
    lines.add(
        "    return error("
            + "\"MJJBR-003\", \"Duplicate JSON property '\" + name + \"'.\", "
            + "propertyPath(name), location);");
    lines.add("  }");
    lines.add("");
    lines.add("  private static JsonReadException missingRequired(");
    lines.add(
        "      String name, JsonPath path, io.github.mundanej.mjjb.runtime.JsonLocation location) {");
    lines.add(
        "    return error("
            + "\"MJJBR-005\", \"Missing required JSON property '\" + name + \"'.\", "
            + "path, location);");
    lines.add("  }");
    if (hasScalarType(model, JavaScalarType.STRING)) {
      lines.addAll(stringHelper());
    }
    if (hasScalarType(model, JavaScalarType.INTEGER)) {
      lines.addAll(integerHelper());
    }
    if (hasScalarType(model, JavaScalarType.NUMBER)) {
      lines.addAll(numberHelper());
    }
    if (hasScalarType(model, JavaScalarType.BOOLEAN)) {
      lines.addAll(booleanHelper());
    }
    if (hasArrayType(model, JavaScalarType.STRING)) {
      lines.addAll(arrayHelper(JavaScalarType.STRING));
    }
    if (hasArrayType(model, JavaScalarType.INTEGER)) {
      lines.addAll(arrayHelper(JavaScalarType.INTEGER));
    }
    if (hasArrayType(model, JavaScalarType.NUMBER)) {
      lines.addAll(arrayHelper(JavaScalarType.NUMBER));
    }
    if (hasArrayType(model, JavaScalarType.BOOLEAN)) {
      lines.addAll(arrayHelper(JavaScalarType.BOOLEAN));
    }
    if (!model.rootObject().fields().isEmpty()) {
      lines.addAll(requireTokenHelper());
      if (hasArrayField(model)) {
        lines.addAll(hasNextHelper());
      }
      lines.addAll(atPathHelper());
    }
    lines.add("");
    lines.add("  private static JsonReadException error(");
    lines.add(
        "      String code, String message, JsonPath path, "
            + "io.github.mundanej.mjjb.runtime.JsonLocation location) {");
    lines.add(
        "    return new JsonReadException(JsonDiagnostic.error(code, message, path, location));");
    lines.add("  }");
    lines.add("");
    lines.add("  private static JsonPath propertyPath(String name) {");
    lines.add("    return JsonPath.ROOT.property(name);");
    lines.add("  }");
    return lines;
  }

  private static List<String> stringHelper() {
    return List.of(
        "",
        "  private static String readString(JsonReader reader, JsonPath path)",
        "      throws JsonReadException {",
        "    requireToken(reader, JsonToken.STRING, \"MJJBR-006\", \"Expected JSON string.\", path);",
        "    try {",
        "      return reader.nextString();",
        "    } catch (JsonReadException exception) {",
        "      throw atPath(exception, path);",
        "    }",
        "  }");
  }

  private static List<String> integerHelper() {
    return List.of(
        "",
        "  private static long readInteger(JsonReader reader, JsonPath path)",
        "      throws JsonReadException {",
        "    String literal =",
        "        readNumberLiteral(reader, path, \"MJJBR-007\", \"Expected JSON integer.\");",
        "    if (!isIntegerLiteral(literal)) {",
        "      throw error(",
        "          \"MJJBR-007\", \"Expected JSON integer.\", path, reader.location());",
        "    }",
        "    try {",
        "      return Long.parseLong(literal);",
        "    } catch (NumberFormatException exception) {",
        "      throw error(",
        "          \"MJJBR-007\", \"Expected JSON integer.\", path, reader.location());",
        "    }",
        "  }",
        "",
        "  private static boolean isIntegerLiteral(String literal) {",
        "    return literal.indexOf('.') < 0 && literal.indexOf('e') < 0 && literal.indexOf('E') < 0;",
        "  }");
  }

  private static List<String> numberHelper() {
    return List.of(
        "",
        "  private static double readNumber(JsonReader reader, JsonPath path)",
        "      throws JsonReadException {",
        "    String literal =",
        "        readNumberLiteral(reader, path, \"MJJBR-008\", \"Expected JSON number.\");",
        "    double value;",
        "    try {",
        "      value = Double.parseDouble(literal);",
        "    } catch (NumberFormatException exception) {",
        "      throw error(\"MJJBR-008\", \"Expected JSON number.\", path, reader.location());",
        "    }",
        "    if (!Double.isFinite(value)) {",
        "      throw error(\"MJJBR-008\", \"Expected finite JSON number.\", path, reader.location());",
        "    }",
        "    return value;",
        "  }");
  }

  private static List<String> booleanHelper() {
    return List.of(
        "",
        "  private static boolean readBoolean(JsonReader reader, JsonPath path)",
        "      throws JsonReadException {",
        "    requireToken(",
        "        reader, JsonToken.BOOLEAN, \"MJJBR-009\", \"Expected JSON boolean.\", path);",
        "    try {",
        "      return reader.nextBoolean();",
        "    } catch (JsonReadException exception) {",
        "      throw atPath(exception, path);",
        "    }",
        "  }");
  }

  private static List<String> arrayHelper(JavaScalarType scalarType) {
    String typeName = scalarType.boxedJavaType();
    String methodName = arrayReadMethodName(scalarType);
    String itemReadExpression = readScalarExpression(scalarType, "path.index(index)");
    return List.of(
        "",
        "  private static List<"
            + typeName
            + "> "
            + methodName
            + "(JsonReader reader, JsonPath path)",
        "      throws JsonReadException {",
        "    requireToken(reader, JsonToken.BEGIN_ARRAY, \"MJJBR-010\", \"Expected JSON array.\", path);",
        "    ArrayList<" + typeName + "> values = new ArrayList<>();",
        "    try {",
        "      reader.beginArray();",
        "    } catch (JsonReadException exception) {",
        "      throw atPath(exception, path);",
        "    }",
        "    int index = 0;",
        "    while (hasNext(reader, path)) {",
        "      values.add(" + itemReadExpression + ");",
        "      index++;",
        "    }",
        "    try {",
        "      reader.endArray();",
        "    } catch (JsonReadException exception) {",
        "      throw atPath(exception, path);",
        "    }",
        "    return List.copyOf(values);",
        "  }");
  }

  private static List<String> requireTokenHelper() {
    return List.of(
        "",
        "  private static String readNumberLiteral(",
        "      JsonReader reader, JsonPath path, String code, String message)",
        "      throws JsonReadException {",
        "    requireToken(reader, JsonToken.NUMBER, code, message, path);",
        "    try {",
        "      return reader.nextNumberLiteral();",
        "    } catch (JsonReadException exception) {",
        "      throw atPath(exception, path);",
        "    }",
        "  }",
        "",
        "  private static void requireToken(",
        "      JsonReader reader, JsonToken expected, String code, String message, JsonPath path)",
        "      throws JsonReadException {",
        "    JsonToken actual;",
        "    try {",
        "      actual = reader.peek();",
        "    } catch (JsonReadException exception) {",
        "      throw atPath(exception, path);",
        "    }",
        "    if (actual != expected) {",
        "      throw error(code, message, path, reader.location());",
        "    }",
        "  }");
  }

  private static List<String> hasNextHelper() {
    return List.of(
        "",
        "  private static boolean hasNext(JsonReader reader, JsonPath path)",
        "      throws JsonReadException {",
        "    try {",
        "      return reader.hasNext();",
        "    } catch (JsonReadException exception) {",
        "      throw atPath(exception, path);",
        "    }",
        "  }");
  }

  private static List<String> atPathHelper() {
    return List.of(
        "",
        "  private static JsonReadException atPath(JsonReadException exception, JsonPath path) {",
        "    JsonDiagnostic diagnostic = exception.diagnostic();",
        "    return error(diagnostic.code(), diagnostic.message(), path, diagnostic.location());",
        "  }");
  }

  private static String constructorArguments(BindingModel model) {
    return model.rootObject().fields().stream()
        .map(FieldBinding::javaFieldName)
        .collect(java.util.stream.Collectors.joining(", "));
  }

  private static String readExpression(FieldBinding field, String pathExpression) {
    if (field.array()) {
      return arrayReadMethodName(field.scalarType()) + "(reader, " + pathExpression + ")";
    }
    return readScalarExpression(field.scalarType(), pathExpression);
  }

  private static String readScalarExpression(JavaScalarType scalarType, String pathExpression) {
    return switch (scalarType) {
      case STRING -> "readString(reader, " + pathExpression + ")";
      case INTEGER -> "readInteger(reader, " + pathExpression + ")";
      case NUMBER -> "readNumber(reader, " + pathExpression + ")";
      case BOOLEAN -> "readBoolean(reader, " + pathExpression + ")";
    };
  }

  private static String propertyPathExpression(FieldBinding field) {
    return "propertyPath(" + javaStringLiteral(field.jsonPropertyName()) + ")";
  }

  private static String localJavaType(FieldBinding field) {
    if (field.required()) {
      return field.valueType().requiredJavaType();
    }
    return field.valueType().optionalJavaType();
  }

  private static String requiredDefault(FieldBinding field) {
    if (field.array()) {
      return "null";
    }
    return switch (field.scalarType()) {
      case STRING -> "null";
      case INTEGER -> "0L";
      case NUMBER -> "0.0D";
      case BOOLEAN -> "false";
    };
  }

  private static String seenName(FieldBinding field) {
    return field.javaFieldName() + "Seen";
  }

  private static boolean hasScalarType(BindingModel model, JavaScalarType scalarType) {
    return model.rootObject().fields().stream().anyMatch(field -> field.scalarType() == scalarType);
  }

  private static boolean hasArrayField(BindingModel model) {
    return model.rootObject().fields().stream().anyMatch(FieldBinding::array);
  }

  private static boolean hasArrayType(BindingModel model, JavaScalarType scalarType) {
    return model.rootObject().fields().stream()
        .anyMatch(field -> field.array() && field.scalarType() == scalarType);
  }

  private static String arrayReadMethodName(JavaScalarType scalarType) {
    return switch (scalarType) {
      case STRING -> "readStringArray";
      case INTEGER -> "readIntegerArray";
      case NUMBER -> "readNumberArray";
      case BOOLEAN -> "readBooleanArray";
    };
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
