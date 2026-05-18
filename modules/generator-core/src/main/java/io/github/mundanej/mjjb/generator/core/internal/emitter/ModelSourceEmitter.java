package io.github.mundanej.mjjb.generator.core.internal.emitter;

import io.github.mundanej.mjjb.generator.core.internal.binding.BindingModel;
import io.github.mundanej.mjjb.generator.core.internal.binding.FieldBinding;
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
    lines.addAll(recordLines(model));
    lines.add("");
    return String.join("\n", lines);
  }

  private static List<String> imports(BindingModel model) {
    Set<String> imports = new TreeSet<>();
    for (FieldBinding field : model.rootObject().fields()) {
      if (requiresNullCheck(field)) {
        imports.add("java.util.Objects");
      }
      if (!field.required()) {
        imports.add("java.util.Optional");
      }
    }
    return List.copyOf(imports);
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
    List<FieldBinding> checkedFields =
        fields.stream().filter(ModelSourceEmitter::requiresNullCheck).toList();
    if (checkedFields.isEmpty()) {
      lines.add("}");
      return lines;
    }
    lines.add("  public " + model.rootTypeName() + " {");
    for (FieldBinding field : checkedFields) {
      lines.add(
          "    "
              + field.javaFieldName()
              + " = Objects.requireNonNull("
              + field.javaFieldName()
              + ", \""
              + field.javaFieldName()
              + "\");");
    }
    lines.add("  }");
    lines.add("}");
    return lines;
  }

  private static String javaType(FieldBinding field) {
    if (field.required()) {
      return field.scalarType().requiredJavaType();
    }
    return field.scalarType().optionalJavaType();
  }

  private static boolean requiresNullCheck(FieldBinding field) {
    return !field.required() || "String".equals(field.scalarType().requiredJavaType());
  }
}
