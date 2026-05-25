package io.github.mundanej.mjjb.generator.core.internal.emitter;

import static io.github.mundanej.mjjb.generator.core.internal.emitter.JavaSourceText.stringLiteral;

import io.github.mundanej.mjjb.generator.core.internal.binding.BindingModel;
import io.github.mundanej.mjjb.generator.core.internal.binding.FieldBinding;
import io.github.mundanej.mjjb.generator.core.internal.binding.ObjectBinding;
import io.github.mundanej.mjjb.generator.core.internal.binding.SchemaAnnotationsBinding;
import io.github.mundanej.mjjb.generator.core.internal.binding.TaggedUnionBranch;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Emits optional generated schema metadata helper source. */
public final class MetadataSourceEmitter {
  private static final String DIALECT = "https://json-schema.org/draft/2020-12/schema";

  public String emit(BindingModel model) {
    Objects.requireNonNull(model, "model");
    ArrayList<String> lines = new ArrayList<>();
    lines.add("package " + model.packageName() + ";");
    lines.add("");
    lines.add("import io.github.mundanej.mjjb.runtime.SchemaAnnotations;");
    lines.add("import io.github.mundanej.mjjb.runtime.SchemaBranchMetadata;");
    lines.add("import io.github.mundanej.mjjb.runtime.SchemaObjectMetadata;");
    lines.add("import io.github.mundanej.mjjb.runtime.SchemaPropertyMetadata;");
    lines.add("import io.github.mundanej.mjjb.runtime.SchemaRootMetadata;");
    lines.add("import java.util.List;");
    lines.add("import java.util.Objects;");
    lines.add("import java.util.Optional;");
    lines.add("");
    lines.add("public final class " + metadataTypeName(model) + " {");
    lines.add("  private static final SchemaRootMetadata ROOT = rootMetadata();");
    lines.add("");
    lines.add("  private " + metadataTypeName(model) + "() {}");
    lines.add("");
    lines.add("  public static SchemaRootMetadata root() {");
    lines.add("    return ROOT;");
    lines.add("  }");
    lines.add("");
    lines.add("  public static List<SchemaPropertyMetadata> properties() {");
    lines.add("    return ROOT.rootObject().properties();");
    lines.add("  }");
    lines.add("");
    lines.add("  public static Optional<SchemaPropertyMetadata> property(String jsonName) {");
    lines.add("    Objects.requireNonNull(jsonName, \"jsonName\");");
    lines.add("    return properties().stream()");
    lines.add("        .filter(property -> jsonName.equals(property.jsonName()))");
    lines.add("        .findFirst();");
    lines.add("  }");
    lines.add("");
    lines.add("  public static List<SchemaBranchMetadata> branches() {");
    lines.add("    return ROOT.branches();");
    lines.add("  }");
    lines.add("");
    lines.addAll(rootMetadataMethod(model));
    lines.addAll(rootObjectMethod(model));
    if (model.taggedUnion().isPresent()) {
      List<TaggedUnionBranch> branches = model.taggedUnion().orElseThrow().branches();
      for (int branchIndex = 0; branchIndex < branches.size(); branchIndex++) {
        lines.addAll(branchMetadataMethod(branches.get(branchIndex), branchIndex));
        lines.addAll(branchObjectMethod(branches.get(branchIndex), branchIndex));
      }
    }
    lines.add("}");
    lines.add("");
    return String.join("\n", lines);
  }

  public static String metadataTypeName(BindingModel model) {
    Objects.requireNonNull(model, "model");
    return model.rootTypeName() + "JsonSchemaMetadata";
  }

  private static List<String> rootMetadataMethod(BindingModel model) {
    ArrayList<String> lines = new ArrayList<>();
    String tagProperty =
        model
            .taggedUnion()
            .map(union -> "Optional.of(" + stringLiteral(union.tagPropertyName()) + ")")
            .orElse("Optional.empty()");
    List<String> branchExpressions =
        model.taggedUnion().isPresent()
            ? indexedNames("branch", model.taggedUnion().orElseThrow().branches().size())
            : List.of();
    lines.add("  private static SchemaRootMetadata rootMetadata() {");
    lines.add("    return");
    lines.add("        new SchemaRootMetadata(");
    lines.add("            " + stringLiteral(DIALECT) + ",");
    lines.add("            " + stringLiteral(model.rootTypeName()) + ",");
    lines.add("            rootObject(),");
    lines.add("            " + tagProperty + ",");
    lines.add("            " + listExpression(branchExpressions) + ");");
    lines.add("  }");
    return lines;
  }

  private static List<String> rootObjectMethod(BindingModel model) {
    ArrayList<String> lines = new ArrayList<>();
    lines.add("");
    lines.add("  private static SchemaObjectMetadata rootObject() {");
    lines.add("    return " + objectExpression(model.rootObject(), "rootProperty") + ";");
    lines.add("  }");
    lines.addAll(propertyMethods(model.rootObject(), "rootProperty"));
    return lines;
  }

  private static List<String> branchMetadataMethod(TaggedUnionBranch branch, int branchIndex) {
    ArrayList<String> lines = new ArrayList<>();
    lines.add("");
    lines.add("  private static SchemaBranchMetadata branch" + branchIndex + "() {");
    lines.add(
        "    return new SchemaBranchMetadata("
            + stringLiteral(branch.tagValue())
            + ", "
            + stringLiteral(branch.object().javaTypeName())
            + ", branch"
            + branchIndex
            + "Object());");
    lines.add("  }");
    return lines;
  }

  private static List<String> branchObjectMethod(TaggedUnionBranch branch, int branchIndex) {
    String prefix = "branch" + branchIndex + "Property";
    ArrayList<String> lines = new ArrayList<>();
    lines.add("");
    lines.add("  private static SchemaObjectMetadata branch" + branchIndex + "Object() {");
    lines.add("    return " + objectExpression(branch.object(), prefix) + ";");
    lines.add("  }");
    lines.addAll(propertyMethods(branch.object(), prefix));
    return lines;
  }

  private static List<String> propertyMethods(ObjectBinding object, String prefix) {
    ArrayList<String> lines = new ArrayList<>();
    for (int index = 0; index < object.fields().size(); index++) {
      lines.add("");
      lines.add("  private static SchemaPropertyMetadata " + prefix + index + "() {");
      lines.add("    return " + propertyExpression(object.fields().get(index)) + ";");
      lines.add("  }");
    }
    return lines;
  }

  private static String objectExpression(ObjectBinding object, String propertyPrefix) {
    return "new SchemaObjectMetadata("
        + stringLiteral(object.schemaPointer().value())
        + ", "
        + stringLiteral(object.javaTypeName())
        + ", "
        + annotationsExpression(object.annotations())
        + ", "
        + listExpression(indexedNames(propertyPrefix, object.fields().size()))
        + ")";
  }

  private static List<String> indexedNames(String prefix, int size) {
    ArrayList<String> names = new ArrayList<>();
    for (int index = 0; index < size; index++) {
      names.add(prefix + index + "()");
    }
    return names;
  }

  private static String propertyExpression(FieldBinding field) {
    return "new SchemaPropertyMetadata("
        + stringLiteral(field.jsonPropertyName())
        + ", "
        + stringLiteral(field.javaFieldName())
        + ", "
        + stringLiteral(field.schemaPointer().value())
        + ", "
        + field.required()
        + ", "
        + stringLiteral(fieldJavaType(field))
        + ", "
        + field.valueType().nullable()
        + ", "
        + field.array()
        + ", "
        + annotationsExpression(field.annotations())
        + ")";
  }

  private static String fieldJavaType(FieldBinding field) {
    if (field.required()) {
      return field.valueType().requiredJavaType();
    }
    return field.valueType().optionalJavaType();
  }

  private static String annotationsExpression(SchemaAnnotationsBinding annotations) {
    return "new SchemaAnnotations("
        + optionalString(annotations.title())
        + ", "
        + optionalString(annotations.description())
        + ", "
        + optionalString(annotations.comment())
        + ", "
        + listExpression(
            annotations.examplesJson().stream().map(JavaSourceText::stringLiteral).toList())
        + ", "
        + optionalBoolean(annotations.deprecated())
        + ", "
        + optionalBoolean(annotations.readOnly())
        + ", "
        + optionalBoolean(annotations.writeOnly())
        + ", "
        + optionalString(annotations.defaultJson())
        + ")";
  }

  private static String optionalString(Optional<String> value) {
    return value
        .map(string -> "Optional.of(" + stringLiteral(string) + ")")
        .orElse("Optional.empty()");
  }

  private static String optionalBoolean(Optional<Boolean> value) {
    return value
        .map(booleanValue -> "Optional.of(" + booleanValue + ")")
        .orElse("Optional.empty()");
  }

  private static String listExpression(List<String> expressions) {
    if (expressions.isEmpty()) {
      return "List.of()";
    }
    return "List.of(" + String.join(", ", expressions) + ")";
  }
}
