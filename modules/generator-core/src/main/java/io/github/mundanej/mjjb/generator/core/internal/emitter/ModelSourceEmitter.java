package io.github.mundanej.mjjb.generator.core.internal.emitter;

import static io.github.mundanej.mjjb.generator.core.internal.emitter.BindingTraversal.allFields;
import static io.github.mundanej.mjjb.generator.core.internal.emitter.BindingTraversal.allMaps;
import static io.github.mundanej.mjjb.generator.core.internal.emitter.BindingTraversal.nestedObjects;
import static io.github.mundanej.mjjb.generator.core.internal.emitter.BindingTraversal.objectMaps;
import static io.github.mundanej.mjjb.generator.core.internal.emitter.JavaSourceText.indent;
import static io.github.mundanej.mjjb.generator.core.internal.emitter.JavaSourceText.stringLiteral;

import io.github.mundanej.mjjb.generator.core.internal.binding.BindingModel;
import io.github.mundanej.mjjb.generator.core.internal.binding.FieldBinding;
import io.github.mundanej.mjjb.generator.core.internal.binding.LiteralValue;
import io.github.mundanej.mjjb.generator.core.internal.binding.MapBinding;
import io.github.mundanej.mjjb.generator.core.internal.binding.ObjectBinding;
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
    if (!allMaps(model).isEmpty()) {
      imports.add("java.util.Map");
      imports.add("java.util.Set");
      imports.add("java.util.Objects");
    }
    if (allMaps(model).stream().anyMatch(MapBinding::patternProperties)) {
      imports.add("java.util.regex.Pattern");
    }
    for (MapBinding map : allMaps(model)) {
      if (map.valueType().nullable()) {
        imports.add("io.github.mundanej.mjjb.runtime.JsonField");
      }
      if (map.array()) {
        imports.add("java.util.List");
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
    for (ObjectBinding object : nestedObjects(model)) {
      lines.add("");
      lines.addAll(indent(objectRecordLines(object), "  "));
    }
    lines.add("}");
    return lines;
  }

  private static List<String> branchRecordLines(BindingModel model, TaggedUnionBranch branch) {
    List<FieldBinding> fields = branch.object().fields();
    List<String> components = recordComponents(branch.object());
    String typeName = branch.object().javaTypeName();
    if (components.isEmpty()) {
      return List.of("  record " + typeName + "() implements " + model.rootTypeName() + " {}");
    }
    ArrayList<String> lines = new ArrayList<>();
    lines.add("  record " + typeName + "(");
    for (int index = 0; index < components.size(); index++) {
      String suffix =
          index == components.size() - 1 ? ") implements " + model.rootTypeName() + " {" : ",";
      lines.add("      " + components.get(index) + suffix);
    }
    List<FieldBinding> defaultFields =
        fields.stream().filter(field -> field.valueType().literals().hasDefault()).toList();
    PatternConstantSet patternConstants = constructorPatternConstants(branch.object());
    if (!patternConstants.empty()) {
      lines.addAll(indent(patternConstants.declarations(""), "  "));
    }
    for (FieldBinding field : defaultFields) {
      lines.addAll(indent(defaultAccessorLines(field), "  "));
    }
    List<FieldBinding> checkedFields =
        fields.stream().filter(ModelSourceEmitter::requiresNullCheck).toList();
    if (checkedFields.isEmpty() && objectMaps(branch.object()).isEmpty()) {
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
    for (MapBinding map : objectMaps(branch.object())) {
      mapConstructorLines(map, branch.object(), patternConstants)
          .forEach(line -> lines.add("      " + line));
    }
    lines.add("    }");
    lines.add("  }");
    return lines;
  }

  private static List<String> recordLines(BindingModel model) {
    List<FieldBinding> fields = model.rootObject().fields();
    List<String> components = recordComponents(model.rootObject());
    if (components.isEmpty()) {
      return List.of("public record " + model.rootTypeName() + "() {}");
    }
    ArrayList<String> lines = new ArrayList<>();
    lines.add("public record " + model.rootTypeName() + "(");
    for (int index = 0; index < components.size(); index++) {
      String suffix = index == components.size() - 1 ? ") {" : ",";
      lines.add("    " + components.get(index) + suffix);
    }
    List<FieldBinding> defaultFields =
        fields.stream().filter(field -> field.valueType().literals().hasDefault()).toList();
    PatternConstantSet patternConstants = constructorPatternConstants(model.rootObject());
    if (!patternConstants.empty()) {
      lines.addAll(patternConstants.declarations("  "));
    }
    for (FieldBinding field : defaultFields) {
      lines.addAll(defaultAccessorLines(field));
    }
    for (ObjectBinding object : nestedObjects(model)) {
      lines.add("");
      lines.addAll(indent(objectRecordLines(object), "  "));
    }
    List<FieldBinding> checkedFields =
        fields.stream().filter(ModelSourceEmitter::requiresNullCheck).toList();
    if (checkedFields.isEmpty() && objectMaps(model.rootObject()).isEmpty()) {
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
    for (MapBinding map : objectMaps(model.rootObject())) {
      mapConstructorLines(map, model.rootObject(), patternConstants)
          .forEach(line -> lines.add("    " + line));
    }
    lines.add("  }");
    lines.add("}");
    return lines;
  }

  private static List<String> objectRecordLines(ObjectBinding object) {
    List<FieldBinding> fields = object.fields();
    List<String> components = recordComponents(object);
    if (components.isEmpty()) {
      return List.of("public record " + object.javaTypeName() + "() {}");
    }
    ArrayList<String> lines = new ArrayList<>();
    lines.add("public record " + object.javaTypeName() + "(");
    for (int index = 0; index < components.size(); index++) {
      String suffix = index == components.size() - 1 ? ") {" : ",";
      lines.add("    " + components.get(index) + suffix);
    }
    List<FieldBinding> checkedFields =
        fields.stream().filter(ModelSourceEmitter::requiresNullCheck).toList();
    PatternConstantSet patternConstants = constructorPatternConstants(object);
    if (!patternConstants.empty()) {
      lines.addAll(patternConstants.declarations("  "));
    }
    if (checkedFields.isEmpty() && objectMaps(object).isEmpty()) {
      lines.add("}");
      return lines;
    }
    lines.add("  public " + object.javaTypeName() + " {");
    for (FieldBinding field : checkedFields) {
      for (String assignmentLine : constructorAssignmentLines(field)) {
        lines.add("    " + assignmentLine);
      }
    }
    for (MapBinding map : objectMaps(object)) {
      mapConstructorLines(map, object, patternConstants).forEach(line -> lines.add("    " + line));
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

  private static List<String> recordComponents(ObjectBinding object) {
    ArrayList<String> components = new ArrayList<>();
    for (FieldBinding field : object.fields()) {
      components.add(javaType(field) + " " + field.javaFieldName());
    }
    object
        .patternProperties()
        .ifPresent(map -> components.add(mapJavaType(map) + " " + map.javaFieldName()));
    object
        .additionalProperties()
        .ifPresent(map -> components.add(mapJavaType(map) + " " + map.javaFieldName()));
    return List.copyOf(components);
  }

  private static String mapJavaType(MapBinding map) {
    return "Map<String, " + mapValueType(map) + ">";
  }

  private static String mapValueType(MapBinding map) {
    if (map.valueType().nullable() || map.array() || map.object()) {
      return map.valueType().requiredJavaType();
    }
    return map.scalarType().boxedJavaType();
  }

  private static boolean requiresNullCheck(FieldBinding field) {
    return field.object()
        || field.valueType().nullable()
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
    if (field.object() && field.required()) {
      return List.of(name + " = Objects.requireNonNull(" + name + ", \"" + name + "\");");
    }
    if (field.object()) {
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

  private static PatternConstantSet constructorPatternConstants(ObjectBinding object) {
    PatternConstantSet constants = new PatternConstantSet();
    for (MapBinding map : objectMaps(object)) {
      if (map.patternProperties()) {
        constants.add(map.pattern());
      } else {
        object.patternProperties().ifPresent(patternMap -> constants.add(patternMap.pattern()));
      }
    }
    return constants;
  }

  private static List<String> mapConstructorLines(
      MapBinding map, ObjectBinding object, PatternConstantSet patternConstants) {
    ArrayList<String> lines = new ArrayList<>();
    String name = map.javaFieldName();
    if (map.valueType().nullable()) {
      String copyName = name + "Copy";
      lines.add(
          "var "
              + copyName
              + " = new java.util.LinkedHashMap<String, "
              + mapValueType(map)
              + ">();");
      lines.add(
          "for (Map.Entry<String, "
              + mapValueType(map)
              + "> entry : Objects.requireNonNull("
              + name
              + ", \""
              + name
              + "\").entrySet()) {");
      lines.add(
          "  "
              + mapValueType(map)
              + " entryValue = Objects.requireNonNull(entry.getValue(), \""
              + name
              + " value\");");
      lines.add("  if (entryValue.isAbsent()) {");
      lines.add(
          "    throw new IllegalArgumentException(\"additionalProperties nullable entries must be present or explicit null\");");
      lines.add("  }");
      lines.add("  if (entryValue.hasValue()) {");
      if (map.array()) {
        lines.add("    entryValue = JsonField.value(List.copyOf(entryValue.requireValue()));");
      } else {
        lines.add("    entryValue = JsonField.value(entryValue.requireValue());");
      }
      lines.add("  }");
      lines.add(
          "  "
              + copyName
              + ".put(Objects.requireNonNull(entry.getKey(), \""
              + name
              + " key\"), entryValue);");
      lines.add("}");
      lines.add(name + " = Map.copyOf(" + copyName + ");");
    } else if (map.array()) {
      String copyName = name + "Copy";
      lines.add(
          "var "
              + copyName
              + " = new java.util.LinkedHashMap<String, "
              + mapValueType(map)
              + ">();");
      lines.add(
          "for (Map.Entry<String, "
              + mapValueType(map)
              + "> entry : Objects.requireNonNull("
              + name
              + ", \""
              + name
              + "\").entrySet()) {");
      lines.add(
          "  "
              + copyName
              + ".put(Objects.requireNonNull(entry.getKey(), \""
              + name
              + " key\"), List.copyOf(Objects.requireNonNull(entry.getValue(), \""
              + name
              + " value\")));");
      lines.add("}");
      lines.add(name + " = Map.copyOf(" + copyName + ");");
    } else {
      lines.add(name + " = Map.copyOf(Objects.requireNonNull(" + name + ", \"" + name + "\"));");
    }
    List<String> reservedNames = reservedJsonPropertyNames(object);
    if (!reservedNames.isEmpty()) {
      lines.add("if (!java.util.Collections.disjoint(" + name + ".keySet(), Set.of(");
      for (int index = 0; index < reservedNames.size(); index++) {
        String suffix = index == reservedNames.size() - 1 ? "))) {" : ",";
        lines.add("    " + stringLiteral(reservedNames.get(index)) + suffix);
      }
      lines.add(
          "  throw new IllegalArgumentException(\""
              + map.sourceKeyword()
              + " must not contain declared property names\");");
      lines.add("}");
    }
    if (map.patternProperties()) {
      lines.add(
          "if (!"
              + name
              + ".keySet().stream().allMatch(key -> "
              + patternConstants.name(map.pattern())
              + ".matcher(key).find())) {");
      lines.add(
          "  throw new IllegalArgumentException(\"patternProperties keys must match the configured pattern\");");
      lines.add("}");
    } else {
      object
          .patternProperties()
          .ifPresent(
              patternMap -> {
                lines.add(
                    "if ("
                        + name
                        + ".keySet().stream().anyMatch(key -> "
                        + patternConstants.name(patternMap.pattern())
                        + ".matcher(key).find())) {");
                lines.add(
                    "  throw new IllegalArgumentException(\"additionalProperties must not contain patternProperties keys\");");
                lines.add("}");
              });
    }
    return lines;
  }

  private static List<String> reservedJsonPropertyNames(ObjectBinding object) {
    ArrayList<String> names = new ArrayList<>();
    object.fields().stream().map(FieldBinding::jsonPropertyName).forEach(names::add);
    names.addAll(object.reservedJsonPropertyNames());
    return List.copyOf(names);
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
      case STRING -> "Optional.of(" + stringLiteral(literal.value()) + ")";
      case INTEGER -> "Optional.of(" + literal.value() + "L)";
      case NUMBER -> "Optional.of(Double.parseDouble(" + stringLiteral(literal.value()) + "))";
      case BOOLEAN -> "Optional.of(" + literal.value() + ")";
      case NULL -> "Optional.empty()";
    };
  }

  private static String capitalized(String value) {
    return value.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + value.substring(1);
  }
}
