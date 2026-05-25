package io.github.mundanej.mjjb.generator.core.internal.emitter;

import static io.github.mundanej.mjjb.generator.core.internal.emitter.BindingTraversal.allFields;
import static io.github.mundanej.mjjb.generator.core.internal.emitter.BindingTraversal.allMaps;
import static io.github.mundanej.mjjb.generator.core.internal.emitter.BindingTraversal.nestedObjects;
import static io.github.mundanej.mjjb.generator.core.internal.emitter.JavaSourceText.stringLiteral;

import io.github.mundanej.mjjb.generator.core.internal.binding.BindingModel;
import io.github.mundanej.mjjb.generator.core.internal.binding.FieldBinding;
import io.github.mundanej.mjjb.generator.core.internal.binding.JavaScalarType;
import io.github.mundanej.mjjb.generator.core.internal.binding.MapBinding;
import io.github.mundanej.mjjb.generator.core.internal.binding.ObjectBinding;
import io.github.mundanej.mjjb.generator.core.internal.binding.TaggedUnionBranch;
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
    PatternConstantSet patternConstants = readerPatternConstants(model);
    if (!patternConstants.empty()) {
      lines.addAll(patternConstants.declarations("  "));
      lines.add("");
    }
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
    if (model.taggedUnion().isPresent()) {
      lines.addAll(taggedUnionReadLines(model));
    } else {
      lines.addAll(
          objectReadLines(
              model.rootObject(), model.rootTypeName(), model.rootTypeName(), patternConstants));
    }
    lines.add("  }");
    if (model.taggedUnion().isPresent()) {
      for (TaggedUnionBranch branch : model.taggedUnion().orElseThrow().branches()) {
        lines.addAll(branchReaderLines(model, branch, patternConstants));
      }
    }
    for (io.github.mundanej.mjjb.generator.core.internal.binding.ObjectBinding object :
        nestedObjects(model)) {
      lines.addAll(objectReaderLines(model.rootTypeName(), object, patternConstants));
    }
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
    if (hasNullableField(model)) {
      imports.add("io.github.mundanej.mjjb.runtime.JsonField");
    }
    if (hasArrayField(model)) {
      imports.add("java.util.ArrayList");
      imports.add("java.util.List");
    }
    if (!allMaps(model).isEmpty()) {
      imports.add("java.util.LinkedHashMap");
      imports.add("java.util.Map");
    }
    if (allMaps(model).stream().anyMatch(MapBinding::patternProperties)) {
      imports.add("java.util.regex.Pattern");
    }
    if (allFields(model).stream()
        .anyMatch(field -> !field.required() && !field.valueType().nullable())) {
      imports.add("java.util.Optional");
    }
    return List.copyOf(imports);
  }

  private static List<String> objectReadLines(
      ObjectBinding object,
      String constructorType,
      String rootTypeName,
      PatternConstantSet patternConstants) {
    return objectReadLines(
        object, constructorType, rootTypeName, "JsonPath.ROOT", true, patternConstants);
  }

  private static List<String> objectReadLines(
      ObjectBinding object,
      String constructorType,
      String rootTypeName,
      String basePath,
      boolean rootObject,
      PatternConstantSet patternConstants) {
    ArrayList<String> lines = new ArrayList<>();
    List<FieldBinding> fields = object.fields();
    lines.addAll(fieldInitializers(fields, rootTypeName));
    object
        .patternProperties()
        .ifPresent(
            map ->
                lines.add(
                    "    Map<String, "
                        + mapLocalValueType(map, rootTypeName)
                        + "> "
                        + map.javaFieldName()
                        + " = new LinkedHashMap<>();"));
    object
        .additionalProperties()
        .ifPresent(
            map ->
                lines.add(
                    "    Map<String, "
                        + mapLocalValueType(map, rootTypeName)
                        + "> "
                        + map.javaFieldName()
                        + " = new LinkedHashMap<>();"));
    String propertyNameLocal = propertyNameLocal(fields);
    lines.add("    while (reader.hasNext()) {");
    lines.add("      String " + propertyNameLocal + " = reader.nextName();");
    lines.add("      switch (" + propertyNameLocal + ") {");
    for (FieldBinding field : fields) {
      lines.addAll(fieldCase(field, basePath));
    }
    if (object.patternProperties().isPresent() || object.additionalProperties().isPresent()) {
      lines.add("        default -> {");
      object
          .patternProperties()
          .ifPresent(
              map -> {
                lines.add(
                    "          if ("
                        + patternMatchExpression(map, propertyNameLocal, patternConstants)
                        + ") {");
                lines.addAll(readMapPutLines(map, propertyNameLocal, basePath, "            "));
                lines.add("          } else {");
              });
      String mapIndent = object.patternProperties().isPresent() ? "            " : "          ";
      object
          .additionalProperties()
          .ifPresentOrElse(
              map -> lines.addAll(readMapPutLines(map, propertyNameLocal, basePath, mapIndent)),
              () ->
                  lines.add(
                      "            throw error("
                          + "\"MJJBR-004\", \"Unknown JSON property '\" + "
                          + propertyNameLocal
                          + " + \"'.\", "
                          + propertyPathExpression(basePath, propertyNameLocal)
                          + ", reader.location());"));
      object.patternProperties().ifPresent(map -> lines.add("          }"));
      lines.add("        }");
    } else {
      lines.add("        default ->");
      lines.add(
          "            throw error("
              + "\"MJJBR-004\", \"Unknown JSON property '\" + "
              + propertyNameLocal
              + " + \"'.\", "
              + propertyPathExpression(basePath, propertyNameLocal)
              + ", reader.location());");
    }
    lines.add("      }");
    lines.add("    }");
    lines.add("    reader.endObject();");
    lines.addAll(requiredChecks(fields, basePath));
    if (rootObject) {
      lines.add("    if (reader.peek() != JsonToken.END_DOCUMENT) {");
      lines.add(
          "      throw error("
              + "\"MJJBR-002\", \"Unexpected JSON content after root value.\", "
              + "JsonPath.ROOT, reader.location());");
      lines.add("    }");
    }
    lines.add("    return new " + constructorType + "(" + constructorArguments(object) + ");");
    return lines;
  }

  private static List<String> taggedUnionReadLines(BindingModel model) {
    ArrayList<String> lines = new ArrayList<>();
    String tagProperty = model.taggedUnion().orElseThrow().tagPropertyName();
    lines.add("    if (!reader.hasNext()) {");
    lines.add(
        "      throw missingRequired("
            + stringLiteral(tagProperty)
            + ", propertyPath("
            + stringLiteral(tagProperty)
            + "), reader.location());");
    lines.add("    }");
    lines.add("    String tagName = reader.nextName();");
    lines.add("    if (!" + stringLiteral(tagProperty) + ".equals(tagName)) {");
    lines.add(
        "      throw missingRequired("
            + stringLiteral(tagProperty)
            + ", propertyPath("
            + stringLiteral(tagProperty)
            + "), reader.location());");
    lines.add("    }");
    lines.add(
        "    String tagValue = readString(reader, propertyPath("
            + stringLiteral(tagProperty)
            + "));");
    lines.add("    " + model.rootTypeName() + " value =");
    lines.add("        switch (tagValue) {");
    for (TaggedUnionBranch branch : model.taggedUnion().orElseThrow().branches()) {
      lines.add(
          "          case "
              + stringLiteral(branch.tagValue())
              + " -> read"
              + branch.object().javaTypeName()
              + "(reader);");
    }
    lines.add(
        "          default -> throw error(\"MJJBR-011\", \"Unknown tagged oneOf value '\" + tagValue + \"'.\", propertyPath("
            + stringLiteral(tagProperty)
            + "), reader.location());");
    lines.add("        };");
    lines.add("    if (reader.peek() != JsonToken.END_DOCUMENT) {");
    lines.add(
        "      throw error("
            + "\"MJJBR-002\", \"Unexpected JSON content after root value.\", "
            + "JsonPath.ROOT, reader.location());");
    lines.add("    }");
    lines.add("    return value;");
    return lines;
  }

  private static List<String> branchReaderLines(
      BindingModel model, TaggedUnionBranch branch, PatternConstantSet patternConstants) {
    ArrayList<String> lines = new ArrayList<>();
    String branchTypeName = model.rootTypeName() + "." + branch.object().javaTypeName();
    String tagProperty = model.taggedUnion().orElseThrow().tagPropertyName();
    ObjectBinding object = branch.object();
    List<FieldBinding> fields = object.fields();
    lines.add("");
    lines.add(
        "  private static "
            + branchTypeName
            + " read"
            + branch.object().javaTypeName()
            + "(JsonReader reader) throws JsonReadException {");
    lines.addAll(fieldInitializers(fields, model.rootTypeName()));
    object
        .patternProperties()
        .ifPresent(
            map ->
                lines.add(
                    "    Map<String, "
                        + mapLocalValueType(map, model.rootTypeName())
                        + "> "
                        + map.javaFieldName()
                        + " = new LinkedHashMap<>();"));
    object
        .additionalProperties()
        .ifPresent(
            map ->
                lines.add(
                    "    Map<String, "
                        + mapLocalValueType(map, model.rootTypeName())
                        + "> "
                        + map.javaFieldName()
                        + " = new LinkedHashMap<>();"));
    String propertyNameLocal = propertyNameLocal(fields);
    lines.add("    while (reader.hasNext()) {");
    lines.add("      String " + propertyNameLocal + " = reader.nextName();");
    lines.add("      switch (" + propertyNameLocal + ") {");
    lines.add("        case " + stringLiteral(tagProperty) + " -> {");
    lines.add(
        "          throw duplicateProperty("
            + stringLiteral(tagProperty)
            + ", reader.location());");
    lines.add("        }");
    for (FieldBinding field : fields) {
      lines.addAll(fieldCase(field, "JsonPath.ROOT"));
    }
    if (object.patternProperties().isPresent() || object.additionalProperties().isPresent()) {
      lines.add("        default -> {");
      object
          .patternProperties()
          .ifPresent(
              map -> {
                lines.add(
                    "          if ("
                        + patternMatchExpression(map, propertyNameLocal, patternConstants)
                        + ") {");
                lines.addAll(
                    readMapPutLines(map, propertyNameLocal, "JsonPath.ROOT", "            "));
                lines.add("          } else {");
              });
      String mapIndent = object.patternProperties().isPresent() ? "            " : "          ";
      object
          .additionalProperties()
          .ifPresentOrElse(
              map ->
                  lines.addAll(readMapPutLines(map, propertyNameLocal, "JsonPath.ROOT", mapIndent)),
              () ->
                  lines.add(
                      "            throw error("
                          + "\"MJJBR-004\", \"Unknown JSON property '\" + "
                          + propertyNameLocal
                          + " + \"'.\", "
                          + propertyPathExpression("JsonPath.ROOT", propertyNameLocal)
                          + ", reader.location());"));
      object.patternProperties().ifPresent(map -> lines.add("          }"));
      lines.add("        }");
    } else {
      lines.add("        default ->");
      lines.add(
          "            throw error("
              + "\"MJJBR-004\", \"Unknown JSON property '\" + "
              + propertyNameLocal
              + " + \"'.\", "
              + propertyPathExpression("JsonPath.ROOT", propertyNameLocal)
              + ", reader.location());");
    }
    lines.add("      }");
    lines.add("    }");
    lines.add("    reader.endObject();");
    lines.addAll(requiredChecks(fields, "JsonPath.ROOT"));
    lines.add("    return new " + branchTypeName + "(" + constructorArguments(object) + ");");
    lines.add("  }");
    return lines;
  }

  private static List<String> objectReaderLines(
      String rootTypeName, ObjectBinding object, PatternConstantSet patternConstants) {
    ArrayList<String> lines = new ArrayList<>();
    String qualifiedType = rootTypeName + "." + object.javaTypeName();
    lines.add("");
    lines.add(
        "  private static "
            + qualifiedType
            + " read"
            + object.javaTypeName()
            + "(JsonReader reader, JsonPath path) throws JsonReadException {");
    lines.add(
        "    requireToken(reader, JsonToken.BEGIN_OBJECT, \"MJJBR-001\", \"Expected JSON object.\", path);");
    lines.add("    try {");
    lines.add("      reader.beginObject();");
    lines.add("    } catch (JsonReadException exception) {");
    lines.add("      throw atPath(exception, path);");
    lines.add("    }");
    lines.addAll(
        objectReadLines(object, qualifiedType, rootTypeName, "path", false, patternConstants));
    lines.add("  }");
    return lines;
  }

  private static List<String> fieldInitializers(List<FieldBinding> fields, String rootTypeName) {
    ArrayList<String> lines = new ArrayList<>();
    for (FieldBinding field : fields) {
      if (field.valueType().nullable()) {
        lines.add(
            "    "
                + localJavaType(field, rootTypeName)
                + " "
                + field.javaFieldName()
                + " = JsonField.absent();");
        lines.add("    boolean " + seenName(field) + " = false;");
      } else if (field.required()) {
        lines.add(
            "    "
                + localJavaType(field, rootTypeName)
                + " "
                + field.javaFieldName()
                + " = "
                + requiredDefault(field)
                + ";");
        lines.add("    boolean " + seenName(field) + " = false;");
      } else {
        lines.add(
            "    "
                + localJavaType(field, rootTypeName)
                + " "
                + field.javaFieldName()
                + " = Optional.empty();");
        lines.add("    boolean " + seenName(field) + " = false;");
      }
    }
    return lines;
  }

  private static List<String> fieldCase(FieldBinding field, String basePath) {
    ArrayList<String> lines = new ArrayList<>();
    lines.add("        case " + stringLiteral(field.jsonPropertyName()) + " -> {");
    lines.add("          if (" + seenName(field) + ") {");
    if ("JsonPath.ROOT".equals(basePath)) {
      lines.add(
          "            throw duplicateProperty("
              + stringLiteral(field.jsonPropertyName())
              + ", reader.location());");
    } else {
      lines.add(
          "            throw error("
              + "\"MJJBR-003\", \"Duplicate JSON property '"
              + field.jsonPropertyName()
              + "'.\", "
              + propertyPathExpression(field, basePath)
              + ", reader.location());");
    }
    lines.add("          }");
    if (field.required() || field.valueType().nullable()) {
      lines.add(
          "          "
              + field.javaFieldName()
              + " = "
              + readExpression(field, propertyPathExpression(field, basePath))
              + ";");
    } else {
      lines.add(
          "          "
              + field.javaFieldName()
              + " = Optional.of("
              + readExpression(field, propertyPathExpression(field, basePath))
              + ");");
    }
    lines.add("          " + seenName(field) + " = true;");
    lines.add("        }");
    return lines;
  }

  private static List<String> requiredChecks(List<FieldBinding> fields, String basePath) {
    ArrayList<String> lines = new ArrayList<>();
    for (FieldBinding field : fields) {
      if (!field.required()) {
        continue;
      }
      lines.add("    if (!" + seenName(field) + ") {");
      lines.add(
          "      throw missingRequired("
              + stringLiteral(field.jsonPropertyName())
              + ", "
              + propertyPathExpression(field, basePath)
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
    if (hasScalarType(model, JavaScalarType.STRING) || model.taggedUnion().isPresent()) {
      lines.addAll(stringHelper());
      if (hasNullableScalarType(model, JavaScalarType.STRING)) {
        lines.addAll(nullableScalarHelper(JavaScalarType.STRING));
      }
    }
    if (hasScalarType(model, JavaScalarType.INTEGER)) {
      lines.addAll(integerHelper());
      if (hasNullableScalarType(model, JavaScalarType.INTEGER)) {
        lines.addAll(nullableScalarHelper(JavaScalarType.INTEGER));
      }
    }
    if (hasScalarType(model, JavaScalarType.NUMBER)) {
      lines.addAll(numberHelper());
      if (hasNullableScalarType(model, JavaScalarType.NUMBER)) {
        lines.addAll(nullableScalarHelper(JavaScalarType.NUMBER));
      }
    }
    if (hasScalarType(model, JavaScalarType.BOOLEAN)) {
      lines.addAll(booleanHelper());
      if (hasNullableScalarType(model, JavaScalarType.BOOLEAN)) {
        lines.addAll(nullableScalarHelper(JavaScalarType.BOOLEAN));
      }
    }
    if (hasArrayType(model, JavaScalarType.STRING)) {
      lines.addAll(arrayHelper(JavaScalarType.STRING));
      if (hasNullableArrayType(model, JavaScalarType.STRING)) {
        lines.addAll(nullableArrayHelper(JavaScalarType.STRING));
      }
    }
    if (hasArrayType(model, JavaScalarType.INTEGER)) {
      lines.addAll(arrayHelper(JavaScalarType.INTEGER));
      if (hasNullableArrayType(model, JavaScalarType.INTEGER)) {
        lines.addAll(nullableArrayHelper(JavaScalarType.INTEGER));
      }
    }
    if (hasArrayType(model, JavaScalarType.NUMBER)) {
      lines.addAll(arrayHelper(JavaScalarType.NUMBER));
      if (hasNullableArrayType(model, JavaScalarType.NUMBER)) {
        lines.addAll(nullableArrayHelper(JavaScalarType.NUMBER));
      }
    }
    if (hasArrayType(model, JavaScalarType.BOOLEAN)) {
      lines.addAll(arrayHelper(JavaScalarType.BOOLEAN));
      if (hasNullableArrayType(model, JavaScalarType.BOOLEAN)) {
        lines.addAll(nullableArrayHelper(JavaScalarType.BOOLEAN));
      }
    }
    if (!allFields(model).isEmpty() || model.taggedUnion().isPresent()) {
      lines.addAll(requireTokenHelper());
      if (hasNullableField(model)) {
        lines.addAll(nullableHelper());
      }
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

  private static List<String> nullableScalarHelper(JavaScalarType scalarType) {
    String typeName = scalarType.boxedJavaType();
    String methodName = nullableScalarReadMethodName(scalarType);
    String valueExpression = readScalarExpression(scalarType, "path");
    return List.of(
        "",
        "  private static JsonField<"
            + typeName
            + "> "
            + methodName
            + "(JsonReader reader, JsonPath path)",
        "      throws JsonReadException {",
        "    if (isNull(reader, path)) {",
        "      readNull(reader, path);",
        "      return JsonField.explicitNull();",
        "    }",
        "    return JsonField.value(" + valueExpression + ");",
        "  }");
  }

  private static List<String> nullableArrayHelper(JavaScalarType scalarType) {
    String typeName = "List<" + scalarType.boxedJavaType() + ">";
    String methodName = nullableArrayReadMethodName(scalarType);
    String valueExpression = arrayReadMethodName(scalarType) + "(reader, path)";
    return List.of(
        "",
        "  private static JsonField<"
            + typeName
            + "> "
            + methodName
            + "(JsonReader reader, JsonPath path)",
        "      throws JsonReadException {",
        "    if (isNull(reader, path)) {",
        "      readNull(reader, path);",
        "      return JsonField.explicitNull();",
        "    }",
        "    return JsonField.value(" + valueExpression + ");",
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

  private static List<String> nullableHelper() {
    return List.of(
        "",
        "  private static boolean isNull(JsonReader reader, JsonPath path) throws JsonReadException {",
        "    try {",
        "      return reader.peek() == JsonToken.NULL;",
        "    } catch (JsonReadException exception) {",
        "      throw atPath(exception, path);",
        "    }",
        "  }",
        "",
        "  private static void readNull(JsonReader reader, JsonPath path) throws JsonReadException {",
        "    try {",
        "      reader.nextNull();",
        "    } catch (JsonReadException exception) {",
        "      throw atPath(exception, path);",
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

  private static String constructorArguments(ObjectBinding object) {
    ArrayList<String> arguments = new ArrayList<>();
    for (FieldBinding field : object.fields()) {
      arguments.add(field.javaFieldName());
    }
    object.patternProperties().ifPresent(map -> arguments.add(map.javaFieldName()));
    object.additionalProperties().ifPresent(map -> arguments.add(map.javaFieldName()));
    if (arguments.isEmpty()) {
      return "";
    }
    return String.join(", ", arguments);
  }

  private static String propertyNameLocal(List<FieldBinding> fields) {
    if (fields.stream().noneMatch(field -> "name".equals(field.javaFieldName()))) {
      return "name";
    }
    return "__mjjbPropertyName";
  }

  private static String readExpression(FieldBinding field, String pathExpression) {
    if (field.object()) {
      return "read"
          + field.valueType().objectBinding().orElseThrow().javaTypeName()
          + "(reader, "
          + pathExpression
          + ")";
    }
    if (field.valueType().nullable() && field.array()) {
      return nullableArrayReadMethodName(field.scalarType()) + "(reader, " + pathExpression + ")";
    }
    if (field.valueType().nullable()) {
      return nullableScalarReadMethodName(field.scalarType()) + "(reader, " + pathExpression + ")";
    }
    if (field.array()) {
      return arrayReadMethodName(field.scalarType()) + "(reader, " + pathExpression + ")";
    }
    return readScalarExpression(field.scalarType(), pathExpression);
  }

  private static String readMapExpression(MapBinding map, String pathExpression) {
    if (map.object()) {
      return "read"
          + map.valueType().objectBinding().orElseThrow().javaTypeName()
          + "(reader, "
          + pathExpression
          + ")";
    }
    if (map.valueType().nullable() && map.array()) {
      return nullableArrayReadMethodName(map.scalarType()) + "(reader, " + pathExpression + ")";
    }
    if (map.valueType().nullable()) {
      return nullableScalarReadMethodName(map.scalarType()) + "(reader, " + pathExpression + ")";
    }
    if (map.array()) {
      return arrayReadMethodName(map.scalarType()) + "(reader, " + pathExpression + ")";
    }
    return readScalarExpression(map.scalarType(), pathExpression);
  }

  private static List<String> readMapPutLines(
      MapBinding map, String propertyNameLocal, String basePath, String indent) {
    ArrayList<String> lines = new ArrayList<>();
    lines.add(indent + "if (" + map.javaFieldName() + ".containsKey(" + propertyNameLocal + ")) {");
    lines.add(
        indent
            + "  throw error(\"MJJBR-003\", \"Duplicate JSON property '\" + "
            + propertyNameLocal
            + " + \"'.\", "
            + propertyPathExpression(basePath, propertyNameLocal)
            + ", reader.location());");
    lines.add(indent + "}");
    lines.add(
        indent
            + map.javaFieldName()
            + ".put("
            + propertyNameLocal
            + ", "
            + readMapExpression(map, propertyPathExpression(basePath, propertyNameLocal))
            + ");");
    return lines;
  }

  private static PatternConstantSet readerPatternConstants(BindingModel model) {
    PatternConstantSet constants = new PatternConstantSet();
    allMaps(model).stream()
        .filter(MapBinding::patternProperties)
        .map(MapBinding::pattern)
        .forEach(constants::add);
    return constants;
  }

  private static String patternMatchExpression(
      MapBinding map, String propertyNameLocal, PatternConstantSet patternConstants) {
    return patternConstants.name(map.pattern()) + ".matcher(" + propertyNameLocal + ").find()";
  }

  private static String readScalarExpression(JavaScalarType scalarType, String pathExpression) {
    return switch (scalarType) {
      case STRING -> "readString(reader, " + pathExpression + ")";
      case INTEGER -> "readInteger(reader, " + pathExpression + ")";
      case NUMBER -> "readNumber(reader, " + pathExpression + ")";
      case BOOLEAN -> "readBoolean(reader, " + pathExpression + ")";
    };
  }

  private static String propertyPathExpression(FieldBinding field, String basePath) {
    return propertyPathExpression(basePath, stringLiteral(field.jsonPropertyName()));
  }

  private static String propertyPathExpression(String basePath, String nameExpression) {
    if ("JsonPath.ROOT".equals(basePath)) {
      return "propertyPath(" + nameExpression + ")";
    }
    return basePath + ".property(" + nameExpression + ")";
  }

  private static String localJavaType(FieldBinding field, String rootTypeName) {
    if (field.object()) {
      String objectType =
          rootTypeName + "." + field.valueType().objectBinding().orElseThrow().javaTypeName();
      if (field.required()) {
        return objectType;
      }
      return "Optional<" + objectType + ">";
    }
    if (field.required()) {
      return field.valueType().requiredJavaType();
    }
    return field.valueType().optionalJavaType();
  }

  private static String mapLocalValueType(MapBinding map, String rootTypeName) {
    if (map.object()) {
      return rootTypeName + "." + map.valueType().objectBinding().orElseThrow().javaTypeName();
    }
    if (map.valueType().nullable() || map.array()) {
      return map.valueType().requiredJavaType();
    }
    return map.scalarType().boxedJavaType();
  }

  private static String requiredDefault(FieldBinding field) {
    if (field.valueType().nullable()) {
      return "JsonField.absent()";
    }
    if (field.object()) {
      return "null";
    }
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
    return allFields(model).stream()
            .anyMatch(field -> !field.object() && field.scalarType() == scalarType)
        || allMaps(model).stream().anyMatch(map -> !map.object() && map.scalarType() == scalarType);
  }

  private static boolean hasNullableField(BindingModel model) {
    return allFields(model).stream().anyMatch(field -> field.valueType().nullable())
        || allMaps(model).stream().anyMatch(map -> map.valueType().nullable());
  }

  private static boolean hasArrayField(BindingModel model) {
    return allFields(model).stream().anyMatch(FieldBinding::array)
        || allMaps(model).stream().anyMatch(MapBinding::array);
  }

  private static boolean hasArrayType(BindingModel model, JavaScalarType scalarType) {
    return allFields(model).stream()
            .anyMatch(field -> !field.object() && field.array() && field.scalarType() == scalarType)
        || allMaps(model).stream()
            .anyMatch(map -> !map.object() && map.array() && map.scalarType() == scalarType);
  }

  private static boolean hasNullableScalarType(BindingModel model, JavaScalarType scalarType) {
    return allFields(model).stream()
            .anyMatch(
                field ->
                    field.valueType().nullable()
                        && !field.array()
                        && field.scalarType() == scalarType)
        || allMaps(model).stream()
            .anyMatch(
                map ->
                    map.valueType().nullable() && !map.array() && map.scalarType() == scalarType);
  }

  private static boolean hasNullableArrayType(BindingModel model, JavaScalarType scalarType) {
    return allFields(model).stream()
            .anyMatch(
                field ->
                    field.valueType().nullable()
                        && field.array()
                        && field.scalarType() == scalarType)
        || allMaps(model).stream()
            .anyMatch(
                map -> map.valueType().nullable() && map.array() && map.scalarType() == scalarType);
  }

  private static String arrayReadMethodName(JavaScalarType scalarType) {
    return switch (scalarType) {
      case STRING -> "readStringArray";
      case INTEGER -> "readIntegerArray";
      case NUMBER -> "readNumberArray";
      case BOOLEAN -> "readBooleanArray";
    };
  }

  private static String nullableScalarReadMethodName(JavaScalarType scalarType) {
    return switch (scalarType) {
      case STRING -> "readNullableString";
      case INTEGER -> "readNullableInteger";
      case NUMBER -> "readNullableNumber";
      case BOOLEAN -> "readNullableBoolean";
    };
  }

  private static String nullableArrayReadMethodName(JavaScalarType scalarType) {
    return switch (scalarType) {
      case STRING -> "readNullableStringArray";
      case INTEGER -> "readNullableIntegerArray";
      case NUMBER -> "readNullableNumberArray";
      case BOOLEAN -> "readNullableBooleanArray";
    };
  }
}
