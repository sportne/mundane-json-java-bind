package io.github.mundanej.mjjb.generator.core.internal.emitter;

import io.github.mundanej.mjjb.generator.core.internal.binding.BindingModel;
import io.github.mundanej.mjjb.generator.core.internal.binding.DependentRequired;
import io.github.mundanej.mjjb.generator.core.internal.binding.FacetConstraints;
import io.github.mundanej.mjjb.generator.core.internal.binding.FieldBinding;
import io.github.mundanej.mjjb.generator.core.internal.binding.JavaScalarType;
import io.github.mundanej.mjjb.generator.core.internal.binding.LiteralConstraints;
import io.github.mundanej.mjjb.generator.core.internal.binding.LiteralValue;
import io.github.mundanej.mjjb.generator.core.internal.binding.MapBinding;
import io.github.mundanej.mjjb.generator.core.internal.binding.ObjectBinding;
import io.github.mundanej.mjjb.generator.core.internal.binding.TaggedUnionBranch;
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
    if (model.taggedUnion().isPresent()) {
      lines.addAll(taggedUnionDispatchLines(model));
    } else {
      lines.addAll(validateObjectConstraintLines(model.rootObject(), "value", "JsonPath.ROOT"));
      for (FieldBinding field : model.rootObject().fields()) {
        lines.addAll(validateFieldLines(field, "value", "JsonPath.ROOT"));
      }
      model
          .rootObject()
          .patternProperties()
          .ifPresent(
              map ->
                  lines.addAll(
                      validateMapLines(model.rootTypeName(), map, "value", "JsonPath.ROOT")));
      model
          .rootObject()
          .additionalProperties()
          .ifPresent(
              map ->
                  lines.addAll(
                      validateMapLines(model.rootTypeName(), map, "value", "JsonPath.ROOT")));
    }
    lines.add("    return errors.toResult();");
    lines.add("  }");
    if (model.taggedUnion().isPresent()) {
      for (TaggedUnionBranch branch : model.taggedUnion().orElseThrow().branches()) {
        lines.addAll(branchValidatorLines(model, branch));
      }
    }
    for (ObjectBinding object : nestedObjects(model)) {
      lines.addAll(objectValidatorLines(model.rootTypeName(), object));
    }
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
    if (hasMinPropertiesConstraint(model)) {
      lines.addAll(validateMinPropertiesHelper());
    }
    if (hasMaxPropertiesConstraint(model)) {
      lines.addAll(validateMaxPropertiesHelper());
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
    if (hasEnumConstraint(model)) {
      lines.addAll(validateEnumHelper());
    }
    if (hasConstConstraint(model)) {
      lines.addAll(validateConstHelper());
    }
    lines.add("}");
    lines.add("");
    return String.join("\n", lines);
  }

  public static String validatorTypeName(BindingModel model) {
    Objects.requireNonNull(model, "model");
    return model.rootTypeName() + "JsonValidator";
  }

  private static List<String> taggedUnionDispatchLines(BindingModel model) {
    ArrayList<String> lines = new ArrayList<>();
    for (TaggedUnionBranch branch : model.taggedUnion().orElseThrow().branches()) {
      String typeName = model.rootTypeName() + "." + branch.object().javaTypeName();
      lines.add("    if (value instanceof " + typeName + " branch) {");
      lines.add("      validate" + branch.object().javaTypeName() + "(branch, errors, mode);");
      lines.add("      return errors.toResult();");
      lines.add("    }");
    }
    return lines;
  }

  private static List<String> branchValidatorLines(BindingModel model, TaggedUnionBranch branch) {
    ArrayList<String> lines = new ArrayList<>();
    String typeName = model.rootTypeName() + "." + branch.object().javaTypeName();
    lines.add("");
    lines.add(
        "  private static ValidationResult validate"
            + branch.object().javaTypeName()
            + "("
            + typeName
            + " value, ValidationErrors errors, ValidationMode mode) {");
    lines.addAll(validateObjectConstraintLines(branch.object(), "value", "JsonPath.ROOT"));
    for (FieldBinding field : branch.object().fields()) {
      lines.addAll(validateFieldLines(field, "value", "JsonPath.ROOT"));
    }
    branch
        .object()
        .patternProperties()
        .ifPresent(
            map ->
                lines.addAll(
                    validateMapLines(model.rootTypeName(), map, "value", "JsonPath.ROOT")));
    branch
        .object()
        .additionalProperties()
        .ifPresent(
            map ->
                lines.addAll(
                    validateMapLines(model.rootTypeName(), map, "value", "JsonPath.ROOT")));
    lines.add("    return errors.toResult();");
    lines.add("  }");
    return lines;
  }

  private static List<String> objectValidatorLines(String rootTypeName, ObjectBinding object) {
    ArrayList<String> lines = new ArrayList<>();
    lines.add("");
    lines.add(
        "  private static ValidationResult validate"
            + object.javaTypeName()
            + "("
            + rootTypeName
            + "."
            + object.javaTypeName()
            + " value, ValidationErrors errors, JsonPath basePath, ValidationMode mode) {");
    lines.addAll(validateObjectConstraintLines(object, "value", "basePath"));
    for (FieldBinding field : object.fields()) {
      lines.addAll(validateFieldLines(field, "value", "basePath"));
    }
    object
        .patternProperties()
        .ifPresent(map -> lines.addAll(validateMapLines(rootTypeName, map, "value", "basePath")));
    object
        .additionalProperties()
        .ifPresent(map -> lines.addAll(validateMapLines(rootTypeName, map, "value", "basePath")));
    lines.add("    return errors.toResult();");
    lines.add("  }");
    return lines;
  }

  private static List<String> imports(BindingModel model) {
    Set<String> imports = new TreeSet<>();
    imports.add("io.github.mundanej.mjjb.runtime.JsonPath");
    imports.add("io.github.mundanej.mjjb.runtime.ValidationError");
    imports.add("io.github.mundanej.mjjb.runtime.ValidationErrors");
    imports.add("io.github.mundanej.mjjb.runtime.ValidationMode");
    imports.add("io.github.mundanej.mjjb.runtime.ValidationResult");
    if (hasNumericFacet(model) || hasNumberLiteralConstraint(model)) {
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
    List<MapBinding> maps = allMaps(model);
    if (!maps.isEmpty()) {
      imports.add("java.util.Map");
    }
    if (maps.stream().anyMatch(map -> map.valueType().nullable())) {
      imports.add("io.github.mundanej.mjjb.runtime.JsonField");
    }
    if (maps.stream().anyMatch(MapBinding::array)) {
      imports.add("java.util.List");
    }
    return List.copyOf(imports);
  }

  private static List<String> validateMapLines(
      String rootTypeName, MapBinding map, String ownerExpression, String basePathExpression) {
    ArrayList<String> lines = new ArrayList<>();
    String accessor = ownerExpression + "." + map.javaFieldName() + "()";
    lines.add("    if (" + accessor + " == null) {");
    lines.add(
        "      if (!errors.add(ValidationError.of(\"MJJBV-002\", \""
            + map.sourceKeyword()
            + " map must not be null.\", "
            + basePathExpression
            + "))) {");
    lines.add("        return errors.toResult();");
    lines.add("      }");
    lines.add("    }");
    lines.add("    if (" + accessor + " != null) {");
    lines.add(
        "      for (Map.Entry<String, "
            + mapValueType(rootTypeName, map)
            + "> entry : "
            + accessor
            + ".entrySet()) {");
    String pathExpression = basePathExpression + ".property(entry.getKey())";
    if (map.object()) {
      lines.add(
          "        validate"
              + map.valueType().objectBinding().orElseThrow().javaTypeName()
              + "(entry.getValue(), errors, "
              + pathExpression
              + ", mode);");
      lines.add("        if (mode == ValidationMode.FAIL_FAST && !errors.errors().isEmpty()) {");
      lines.add("          return errors.toResult();");
      lines.add("        }");
    } else if (map.valueType().nullable()) {
      lines.add("        if (entry.getValue() == null) {");
      lines.add(
          "          if (!errors.add(ValidationError.of(\"MJJBV-002\", \""
              + map.sourceKeyword()
              + " value must not be null.\", "
              + pathExpression
              + "))) {");
      lines.add("            return errors.toResult();");
      lines.add("          }");
      lines.add("        }");
      lines.add("        if (entry.getValue() != null && entry.getValue().isAbsent()) {");
      lines.add(
          "          if (!errors.add(ValidationError.of(\"MJJBV-017\", \""
              + map.sourceKeyword()
              + " nullable value must be present or explicit null.\", "
              + pathExpression
              + "))) {");
      lines.add("            return errors.toResult();");
      lines.add("          }");
      lines.add("        }");
      if (map.array()) {
        lines.addAll(
            validateMapArrayLines(
                map,
                "entry.getValue().requireValue()",
                "entry.getValue() != null && entry.getValue().hasValue()",
                pathExpression,
                "        "));
      } else {
        lines.addAll(
            validateMapScalarLines(
                map,
                "entry.getValue().requireValue()",
                "entry.getValue() != null && entry.getValue().hasValue()",
                pathExpression,
                "        "));
        lines.addAll(
            validateNullableLiteralLines(map, "entry.getValue()", pathExpression, "        "));
      }
    } else if (map.array()) {
      lines.addAll(
          validateMapArrayLines(map, "entry.getValue()", "true", pathExpression, "        "));
    } else {
      lines.addAll(
          validateMapScalarLines(map, "entry.getValue()", "true", pathExpression, "        "));
      lines.addAll(
          indent(validateLiteralLines(map, "entry.getValue()", "true", pathExpression), "    "));
    }
    lines.add("      }");
    lines.add("    }");
    return lines;
  }

  private static List<String> validateObjectConstraintLines(
      ObjectBinding object, String ownerExpression, String basePathExpression) {
    if (!object.validationConstraints().hasConstraints()) {
      return List.of();
    }
    ArrayList<String> lines = new ArrayList<>();
    String propertyCount = propertyCountExpression(object, ownerExpression);
    object
        .validationConstraints()
        .minProperties()
        .ifPresent(
            minProperties -> {
              lines.add(
                  "    if (!validateMinProperties(errors, "
                      + propertyCount
                      + ", "
                      + minProperties
                      + "L, "
                      + basePathExpression
                      + ")) {");
              lines.add("      return errors.toResult();");
              lines.add("    }");
            });
    object
        .validationConstraints()
        .maxProperties()
        .ifPresent(
            maxProperties -> {
              lines.add(
                  "    if (!validateMaxProperties(errors, "
                      + propertyCount
                      + ", "
                      + maxProperties
                      + "L, "
                      + basePathExpression
                      + ")) {");
              lines.add("      return errors.toResult();");
              lines.add("    }");
            });
    object
        .validationConstraints()
        .propertyNames()
        .ifPresent(
            facets -> {
              lines.addAll(
                  validateDeclaredPropertyNameLines(
                      object, facets, ownerExpression, basePathExpression));
              object
                  .patternProperties()
                  .ifPresent(
                      map ->
                          lines.addAll(
                              validateMapPropertyNameLines(
                                  map, facets, ownerExpression, basePathExpression)));
              object
                  .additionalProperties()
                  .ifPresent(
                      map ->
                          lines.addAll(
                              validateMapPropertyNameLines(
                                  map, facets, ownerExpression, basePathExpression)));
            });
    for (DependentRequired dependency : object.validationConstraints().dependentRequired()) {
      String dependencyPresent =
          propertyPresentExpression(object, ownerExpression, dependency.propertyName());
      for (String requiredProperty : dependency.requiredProperties()) {
        lines.add(
            "    if ("
                + dependencyPresent
                + " && !"
                + propertyPresentExpression(object, ownerExpression, requiredProperty)
                + ") {");
        lines.add(
            "      if (!errors.add(ValidationError.of(\"MJJBV-020\", "
                + javaStringLiteral(
                    "Property '"
                        + dependency.propertyName()
                        + "' requires property '"
                        + requiredProperty
                        + "'.")
                + ", "
                + basePathExpression
                + ".property("
                + javaStringLiteral(requiredProperty)
                + ")))) {");
        lines.add("        return errors.toResult();");
        lines.add("      }");
        lines.add("    }");
      }
    }
    return lines;
  }

  private static List<String> validateDeclaredPropertyNameLines(
      ObjectBinding object,
      FacetConstraints facets,
      String ownerExpression,
      String basePathExpression) {
    ArrayList<String> lines = new ArrayList<>();
    for (String reservedName : object.reservedJsonPropertyNames()) {
      lines.addAll(
          validateStringFacetLines(
              facets,
              javaStringLiteral(reservedName),
              "true",
              basePathExpression + ".property(" + javaStringLiteral(reservedName) + ")"));
    }
    for (FieldBinding field : object.fields()) {
      String presentExpression = fieldPresentExpression(field, ownerExpression);
      lines.addAll(
          validateStringFacetLines(
              facets,
              javaStringLiteral(field.jsonPropertyName()),
              presentExpression,
              basePathExpression
                  + ".property("
                  + javaStringLiteral(field.jsonPropertyName())
                  + ")"));
    }
    return lines;
  }

  private static List<String> validateMapPropertyNameLines(
      MapBinding map, FacetConstraints facets, String ownerExpression, String basePathExpression) {
    ArrayList<String> lines = new ArrayList<>();
    String accessor = ownerExpression + "." + map.javaFieldName() + "()";
    lines.add("    if (" + accessor + " != null) {");
    lines.add("      for (String key : " + accessor + ".keySet()) {");
    lines.addAll(
        indent(
            validateStringFacetLines(facets, "key", "true", basePathExpression + ".property(key)"),
            "    "));
    lines.add("      }");
    lines.add("    }");
    return lines;
  }

  private static String propertyCountExpression(ObjectBinding object, String ownerExpression) {
    ArrayList<String> terms = new ArrayList<>();
    terms.add(Long.toString(object.reservedJsonPropertyNames().size()) + "L");
    for (FieldBinding field : object.fields()) {
      terms.add("(" + fieldPresentExpression(field, ownerExpression) + " ? 1L : 0L)");
    }
    object.patternProperties().ifPresent(map -> terms.add(mapSizeExpression(map, ownerExpression)));
    object
        .additionalProperties()
        .ifPresent(map -> terms.add(mapSizeExpression(map, ownerExpression)));
    return "(" + String.join(" + ", terms) + ")";
  }

  private static String mapSizeExpression(MapBinding map, String ownerExpression) {
    String accessor = ownerExpression + "." + map.javaFieldName() + "()";
    return "(" + accessor + " != null ? " + accessor + ".size() : 0L)";
  }

  private static String propertyPresentExpression(
      ObjectBinding object, String ownerExpression, String propertyName) {
    if (object.reservedJsonPropertyNames().contains(propertyName)) {
      return "true";
    }
    ArrayList<String> expressions = new ArrayList<>();
    object.fields().stream()
        .filter(field -> field.jsonPropertyName().equals(propertyName))
        .findFirst()
        .ifPresent(field -> expressions.add(fieldPresentExpression(field, ownerExpression)));
    object
        .patternProperties()
        .ifPresent(
            map -> expressions.add(mapContainsExpression(map, ownerExpression, propertyName)));
    object
        .additionalProperties()
        .ifPresent(
            map -> expressions.add(mapContainsExpression(map, ownerExpression, propertyName)));
    if (expressions.isEmpty()) {
      return "false";
    }
    return "(" + String.join(" || ", expressions) + ")";
  }

  private static String fieldPresentExpression(FieldBinding field, String ownerExpression) {
    String accessor = accessor(field, ownerExpression);
    if (field.valueType().nullable()) {
      return "(" + accessor + " != null && !" + accessor + ".isAbsent())";
    }
    if (field.required()) {
      return "true";
    }
    return "(" + accessor + " != null && " + accessor + ".isPresent())";
  }

  private static String mapContainsExpression(
      MapBinding map, String ownerExpression, String propertyName) {
    String accessor = ownerExpression + "." + map.javaFieldName() + "()";
    return "("
        + accessor
        + " != null && "
        + accessor
        + ".containsKey("
        + javaStringLiteral(propertyName)
        + "))";
  }

  private static String mapValueType(String rootTypeName, MapBinding map) {
    if (map.object()) {
      return rootTypeName + "." + map.valueType().objectBinding().orElseThrow().javaTypeName();
    }
    if (map.valueType().nullable() || map.array()) {
      return map.valueType().requiredJavaType();
    }
    return map.scalarType().boxedJavaType();
  }

  private static List<String> validateFieldLines(
      FieldBinding field, String ownerExpression, String basePathExpression) {
    if (field.object()) {
      return validateObjectFieldLines(field, ownerExpression, basePathExpression);
    }
    if (field.valueType().nullable()) {
      return validateNullableFieldLines(field, ownerExpression, basePathExpression);
    }
    ArrayList<String> lines = new ArrayList<>();
    String accessor = accessor(field, ownerExpression);
    String pathExpression = propertyPathExpression(field, basePathExpression);
    if (field.required()
        && (field.array() || "String".equals(field.scalarType().requiredJavaType()))) {
      lines.add("    if (" + accessor + " == null) {");
      lines.add(
          "      if (!errors.add("
              + "ValidationError.of(\"MJJBV-002\", \"Required property '"
              + field.jsonPropertyName()
              + "' must not be null.\", "
              + pathExpression
              + "))) {");
      lines.add("        return errors.toResult();");
      lines.add("      }");
      lines.add("    }");
    }
    if (!field.required()) {
      lines.add("    if (" + accessor + " == null) {");
      lines.add(
          "      if (!errors.add("
              + "ValidationError.of(\"MJJBV-003\", \"Optional property container '"
              + field.jsonPropertyName()
              + "' must not be null.\", "
              + pathExpression
              + "))) {");
      lines.add("        return errors.toResult();");
      lines.add("      }");
      lines.add("    }");
    }
    if (field.array()) {
      lines.addAll(validateArrayLines(field, ownerExpression, basePathExpression));
      return lines;
    }
    if (field.scalarType() == JavaScalarType.STRING) {
      lines.addAll(
          validateStringFacetLines(
              field,
              scalarValueExpression(field, ownerExpression),
              scalarGuard(field, ownerExpression),
              pathExpression));
    }
    if (field.scalarType() == JavaScalarType.NUMBER) {
      if (field.required()) {
        lines.add("    if (!validateFinite(errors, " + accessor + ", " + pathExpression + ")) {");
        lines.add("      return errors.toResult();");
        lines.add("    }");
      } else {
        lines.add("    if (" + accessor + " != null && " + accessor + ".isPresent()) {");
        lines.add(
            "      if (!validateFinite(errors, "
                + accessor
                + ".orElseThrow(), "
                + pathExpression
                + ")) {");
        lines.add("        return errors.toResult();");
        lines.add("      }");
        lines.add("    }");
      }
    }
    if (field.scalarType() == JavaScalarType.INTEGER
        || field.scalarType() == JavaScalarType.NUMBER) {
      lines.addAll(
          validateNumericFacetLines(
              field,
              scalarValueExpression(field, ownerExpression),
              numericGuard(field, scalarValueExpression(field, ownerExpression), ownerExpression),
              pathExpression));
    }
    lines.addAll(
        validateLiteralLines(
            field,
            scalarValueExpression(field, ownerExpression),
            literalGuard(field, scalarValueExpression(field, ownerExpression), ownerExpression),
            pathExpression));
    return lines;
  }

  private static List<String> validateObjectFieldLines(
      FieldBinding field, String ownerExpression, String basePathExpression) {
    ArrayList<String> lines = new ArrayList<>();
    String accessor = accessor(field, ownerExpression);
    String pathExpression = propertyPathExpression(field, basePathExpression);
    String methodName = "validate" + field.valueType().objectBinding().orElseThrow().javaTypeName();
    if (field.required()) {
      lines.add("    if (" + accessor + " == null) {");
      lines.add(
          "      if (!errors.add(ValidationError.of(\"MJJBV-002\", \"Required property '"
              + field.jsonPropertyName()
              + "' must not be null.\", "
              + pathExpression
              + "))) {");
      lines.add("        return errors.toResult();");
      lines.add("      }");
      lines.add("    }");
      lines.add("    if (" + accessor + " != null) {");
      lines.add(
          "      " + methodName + "(" + accessor + ", errors, " + pathExpression + ", mode);");
      lines.add("      if (mode == ValidationMode.FAIL_FAST && !errors.errors().isEmpty()) {");
      lines.add("        return errors.toResult();");
      lines.add("      }");
      lines.add("    }");
      return lines;
    }
    lines.add("    if (" + accessor + " == null) {");
    lines.add(
        "      if (!errors.add(ValidationError.of(\"MJJBV-003\", \"Optional property container '"
            + field.jsonPropertyName()
            + "' must not be null.\", "
            + pathExpression
            + "))) {");
    lines.add("        return errors.toResult();");
    lines.add("      }");
    lines.add("    }");
    lines.add("    if (" + accessor + " != null && " + accessor + ".isPresent()) {");
    lines.add(
        "      "
            + methodName
            + "("
            + accessor
            + ".orElseThrow(), errors, "
            + pathExpression
            + ", mode);");
    lines.add("      if (mode == ValidationMode.FAIL_FAST && !errors.errors().isEmpty()) {");
    lines.add("        return errors.toResult();");
    lines.add("      }");
    lines.add("    }");
    return lines;
  }

  private static List<String> validateNullableFieldLines(
      FieldBinding field, String ownerExpression, String basePathExpression) {
    ArrayList<String> lines = new ArrayList<>();
    String fieldExpression = accessor(field, ownerExpression);
    String pathExpression = propertyPathExpression(field, basePathExpression);
    lines.add("    if (" + fieldExpression + " == null) {");
    String code = field.required() ? "MJJBV-002" : "MJJBV-003";
    String messagePrefix = field.required() ? "Required" : "Optional";
    lines.add(
        "      if (!errors.add("
            + "ValidationError.of(\""
            + code
            + "\", \""
            + messagePrefix
            + " nullable property '"
            + field.jsonPropertyName()
            + "' field state must not be null.\", "
            + pathExpression
            + "))) {");
    lines.add("        return errors.toResult();");
    lines.add("      }");
    lines.add("    }");
    if (field.required()) {
      lines.add("    if (" + fieldExpression + " != null && " + fieldExpression + ".isAbsent()) {");
      lines.add(
          "      if (!errors.add("
              + "ValidationError.of(\"MJJBV-017\", \"Required nullable property '"
              + field.jsonPropertyName()
              + "' must be present.\", "
              + pathExpression
              + "))) {");
      lines.add("        return errors.toResult();");
      lines.add("      }");
      lines.add("    }");
    }
    if (field.array()) {
      lines.addAll(validateArrayLines(field, ownerExpression, basePathExpression));
      return lines;
    }
    if (field.scalarType() == JavaScalarType.STRING) {
      lines.addAll(
          validateStringFacetLines(
              field,
              scalarValueExpression(field, ownerExpression),
              scalarGuard(field, ownerExpression),
              pathExpression));
    }
    if (field.scalarType() == JavaScalarType.NUMBER) {
      lines.add("    if (" + scalarGuard(field, ownerExpression) + ") {");
      lines.add(
          "      if (!validateFinite(errors, "
              + scalarValueExpression(field, ownerExpression)
              + ", "
              + pathExpression
              + ")) {");
      lines.add("        return errors.toResult();");
      lines.add("      }");
      lines.add("    }");
    }
    if (field.scalarType() == JavaScalarType.INTEGER
        || field.scalarType() == JavaScalarType.NUMBER) {
      lines.addAll(
          validateNumericFacetLines(
              field,
              scalarValueExpression(field, ownerExpression),
              numericGuard(field, scalarValueExpression(field, ownerExpression), ownerExpression),
              pathExpression));
    }
    lines.addAll(validateNullableLiteralLines(field, ownerExpression, pathExpression));
    return lines;
  }

  private static List<String> validateArrayLines(
      FieldBinding field, String ownerExpression, String basePathExpression) {
    ArrayList<String> lines = new ArrayList<>();
    if (field.valueType().minItems().isEmpty()
        && field.valueType().maxItems().isEmpty()
        && field.scalarType() != JavaScalarType.NUMBER
        && !field.valueType().facets().hasStringFacets()
        && !field.valueType().facets().hasNumericFacets()
        && !field.valueType().literals().hasEnum()
        && !field.valueType().literals().hasConst()) {
      return lines;
    }
    String valueExpression = arrayValueExpression(field, ownerExpression);
    String guard = arrayGuard(field, ownerExpression);
    String pathExpression = propertyPathExpression(field, basePathExpression);
    String itemPathExpression = pathExpression + ".index(index)";
    lines.add("    if (" + guard + ") {");
    if (field.valueType().minItems().isPresent()) {
      lines.add(
          "      if (!validateMinItems(errors, "
              + valueExpression
              + ".size(), "
              + field.valueType().minItems().getAsLong()
              + "L, "
              + pathExpression
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
              + pathExpression
              + ")) {");
      lines.add("        return errors.toResult();");
      lines.add("      }");
    }
    if (field.scalarType() == JavaScalarType.NUMBER) {
      lines.add("      int index = 0;");
      lines.add("      for (Double item : " + valueExpression + ") {");
      lines.add("        if (!validateFinite(errors, item, " + itemPathExpression + ")) {");
      lines.add("          return errors.toResult();");
      lines.add("        }");
      lines.addAll(
          indent(
              validateNumericFacetLines(field, "item", "Double.isFinite(item)", itemPathExpression),
              "      "));
      lines.addAll(
          indent(
              validateLiteralLines(field, "item", "Double.isFinite(item)", itemPathExpression),
              "      "));
      lines.add("        index++;");
      lines.add("      }");
    } else if (field.scalarType() == JavaScalarType.INTEGER
        && (field.valueType().facets().hasNumericFacets()
            || field.valueType().literals().hasEnum()
            || field.valueType().literals().hasConst())) {
      lines.add("      int index = 0;");
      lines.add("      for (Long item : " + valueExpression + ") {");
      lines.addAll(
          indent(validateNumericFacetLines(field, "item", "true", itemPathExpression), "      "));
      lines.addAll(
          indent(validateLiteralLines(field, "item", "true", itemPathExpression), "      "));
      lines.add("        index++;");
      lines.add("      }");
    } else if (field.scalarType() == JavaScalarType.STRING
        && (field.valueType().facets().hasStringFacets()
            || field.valueType().literals().hasEnum()
            || field.valueType().literals().hasConst())) {
      lines.add("      int index = 0;");
      lines.add("      for (String item : " + valueExpression + ") {");
      lines.addAll(
          indent(validateStringFacetLines(field, "item", "true", itemPathExpression), "      "));
      lines.addAll(
          indent(validateLiteralLines(field, "item", "true", itemPathExpression), "      "));
      lines.add("        index++;");
      lines.add("      }");
    } else if (field.scalarType() == JavaScalarType.BOOLEAN
        && (field.valueType().literals().hasEnum() || field.valueType().literals().hasConst())) {
      lines.add("      int index = 0;");
      lines.add("      for (Boolean item : " + valueExpression + ") {");
      lines.addAll(
          indent(validateLiteralLines(field, "item", "true", itemPathExpression), "      "));
      lines.add("        index++;");
      lines.add("      }");
    }
    lines.add("    }");
    return lines;
  }

  private static List<String> validateMapArrayLines(
      MapBinding map,
      String valueExpression,
      String guard,
      String pathExpression,
      String indentPrefix) {
    ArrayList<String> lines = new ArrayList<>();
    if (map.valueType().minItems().isEmpty()
        && map.valueType().maxItems().isEmpty()
        && map.scalarType() != JavaScalarType.NUMBER
        && !map.valueType().facets().hasStringFacets()
        && !map.valueType().facets().hasNumericFacets()
        && !map.valueType().literals().hasEnum()
        && !map.valueType().literals().hasConst()) {
      return lines;
    }
    String itemPathExpression = pathExpression + ".index(index)";
    lines.add(indentPrefix + "if (" + guard + ") {");
    if (map.valueType().minItems().isPresent()) {
      lines.add(
          indentPrefix
              + "  if (!validateMinItems(errors, "
              + valueExpression
              + ".size(), "
              + map.valueType().minItems().getAsLong()
              + "L, "
              + pathExpression
              + ")) {");
      lines.add(indentPrefix + "    return errors.toResult();");
      lines.add(indentPrefix + "  }");
    }
    if (map.valueType().maxItems().isPresent()) {
      lines.add(
          indentPrefix
              + "  if (!validateMaxItems(errors, "
              + valueExpression
              + ".size(), "
              + map.valueType().maxItems().getAsLong()
              + "L, "
              + pathExpression
              + ")) {");
      lines.add(indentPrefix + "    return errors.toResult();");
      lines.add(indentPrefix + "  }");
    }
    if (map.scalarType() == JavaScalarType.NUMBER) {
      lines.add(indentPrefix + "  int index = 0;");
      lines.add(indentPrefix + "  for (Double item : " + valueExpression + ") {");
      lines.add(
          indentPrefix + "    if (!validateFinite(errors, item, " + itemPathExpression + ")) {");
      lines.add(indentPrefix + "      return errors.toResult();");
      lines.add(indentPrefix + "    }");
      lines.addAll(
          indent(
              validateNumericFacetLines(map, "item", "Double.isFinite(item)", itemPathExpression),
              indentPrefix));
      lines.addAll(
          indent(
              validateLiteralLines(map, "item", "Double.isFinite(item)", itemPathExpression),
              indentPrefix));
      lines.add(indentPrefix + "    index++;");
      lines.add(indentPrefix + "  }");
    } else if (map.scalarType() == JavaScalarType.INTEGER
        && (map.valueType().facets().hasNumericFacets()
            || map.valueType().literals().hasEnum()
            || map.valueType().literals().hasConst())) {
      lines.add(indentPrefix + "  int index = 0;");
      lines.add(indentPrefix + "  for (Long item : " + valueExpression + ") {");
      lines.addAll(
          indent(validateNumericFacetLines(map, "item", "true", itemPathExpression), indentPrefix));
      lines.addAll(
          indent(validateLiteralLines(map, "item", "true", itemPathExpression), indentPrefix));
      lines.add(indentPrefix + "    index++;");
      lines.add(indentPrefix + "  }");
    } else if (map.scalarType() == JavaScalarType.STRING
        && (map.valueType().facets().hasStringFacets()
            || map.valueType().literals().hasEnum()
            || map.valueType().literals().hasConst())) {
      lines.add(indentPrefix + "  int index = 0;");
      lines.add(indentPrefix + "  for (String item : " + valueExpression + ") {");
      lines.addAll(
          indent(validateStringFacetLines(map, "item", "true", itemPathExpression), indentPrefix));
      lines.addAll(
          indent(validateLiteralLines(map, "item", "true", itemPathExpression), indentPrefix));
      lines.add(indentPrefix + "    index++;");
      lines.add(indentPrefix + "  }");
    } else if (map.scalarType() == JavaScalarType.BOOLEAN
        && (map.valueType().literals().hasEnum() || map.valueType().literals().hasConst())) {
      lines.add(indentPrefix + "  int index = 0;");
      lines.add(indentPrefix + "  for (Boolean item : " + valueExpression + ") {");
      lines.addAll(
          indent(validateLiteralLines(map, "item", "true", itemPathExpression), indentPrefix));
      lines.add(indentPrefix + "    index++;");
      lines.add(indentPrefix + "  }");
    }
    lines.add(indentPrefix + "}");
    return lines;
  }

  private static List<String> validateMapScalarLines(
      MapBinding map,
      String valueExpression,
      String guard,
      String pathExpression,
      String indentPrefix) {
    ArrayList<String> lines = new ArrayList<>();
    if (map.scalarType() == JavaScalarType.STRING) {
      lines.addAll(
          indent(validateStringFacetLines(map, valueExpression, guard, pathExpression), "    "));
    }
    if (map.scalarType() == JavaScalarType.NUMBER) {
      lines.add(indentPrefix + "if (" + guard + ") {");
      lines.add(
          indentPrefix
              + "  if (!validateFinite(errors, "
              + valueExpression
              + ", "
              + pathExpression
              + ")) {");
      lines.add(indentPrefix + "    return errors.toResult();");
      lines.add(indentPrefix + "  }");
      lines.add(indentPrefix + "}");
    }
    if (map.scalarType() == JavaScalarType.INTEGER || map.scalarType() == JavaScalarType.NUMBER) {
      String numericGuard =
          map.scalarType() == JavaScalarType.NUMBER
              ? guard + " && Double.isFinite(" + valueExpression + ")"
              : guard;
      lines.addAll(
          indent(
              validateNumericFacetLines(map, valueExpression, numericGuard, pathExpression),
              "    "));
    }
    return lines;
  }

  private static boolean hasNumberField(BindingModel model) {
    return allFields(model).stream().anyMatch(field -> field.scalarType() == JavaScalarType.NUMBER)
        || allMaps(model).stream()
            .anyMatch(map -> !map.object() && map.scalarType() == JavaScalarType.NUMBER);
  }

  private static List<String> validateStringFacetLines(
      FieldBinding field, String valueExpression, String guard, String pathExpression) {
    return validateStringFacetLines(
        field.valueType().facets(), valueExpression, guard, pathExpression);
  }

  private static List<String> validateStringFacetLines(
      MapBinding map, String valueExpression, String guard, String pathExpression) {
    return validateStringFacetLines(
        map.valueType().facets(), valueExpression, guard, pathExpression);
  }

  private static List<String> validateStringFacetLines(
      FacetConstraints facets, String valueExpression, String guard, String pathExpression) {
    ArrayList<String> lines = new ArrayList<>();
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
      FieldBinding field, String valueExpression, String guard, String pathExpression) {
    return validateNumericFacetLines(
        field.valueType().facets(), field.scalarType(), valueExpression, guard, pathExpression);
  }

  private static List<String> validateNumericFacetLines(
      MapBinding map, String valueExpression, String guard, String pathExpression) {
    return validateNumericFacetLines(
        map.valueType().facets(), map.scalarType(), valueExpression, guard, pathExpression);
  }

  private static List<String> validateNumericFacetLines(
      FacetConstraints facets,
      JavaScalarType scalarType,
      String valueExpression,
      String guard,
      String pathExpression) {
    ArrayList<String> lines = new ArrayList<>();
    if (!facets.hasNumericFacets()) {
      return lines;
    }
    String numericValue = numericValueExpression(scalarType, valueExpression);
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

  private static List<String> validateLiteralLines(
      FieldBinding field, String valueExpression, String guard, String pathExpression) {
    return validateLiteralLines(
        field.valueType().literals(), field.scalarType(), valueExpression, guard, pathExpression);
  }

  private static List<String> validateLiteralLines(
      MapBinding map, String valueExpression, String guard, String pathExpression) {
    return validateLiteralLines(
        map.valueType().literals(), map.scalarType(), valueExpression, guard, pathExpression);
  }

  private static List<String> validateLiteralLines(
      LiteralConstraints literals,
      JavaScalarType scalarType,
      String valueExpression,
      String guard,
      String pathExpression) {
    if (!literals.hasEnum() && !literals.hasConst()) {
      return List.of();
    }
    ArrayList<String> lines = new ArrayList<>();
    lines.add("    if (" + guard + ") {");
    if (literals.hasEnum()) {
      lines.add(
          "      if (!validateEnum(errors, "
              + enumMatchExpression(scalarType, valueExpression, literals.enumValues())
              + ", "
              + pathExpression
              + ")) {");
      lines.add("        return errors.toResult();");
      lines.add("      }");
    }
    if (literals.hasConst()) {
      lines.add(
          "      if (!validateConst(errors, "
              + literalMatchExpression(
                  scalarType, valueExpression, literals.constValue().orElseThrow())
              + ", "
              + pathExpression
              + ")) {");
      lines.add("        return errors.toResult();");
      lines.add("      }");
    }
    lines.add("    }");
    return lines;
  }

  private static List<String> validateNullableLiteralLines(
      FieldBinding field, String ownerExpression, String pathExpression) {
    return validateNullableLiteralLines(
        field.valueType().literals(),
        field.scalarType(),
        accessor(field, ownerExpression),
        pathExpression,
        "    ");
  }

  private static List<String> validateNullableLiteralLines(
      MapBinding map, String fieldExpression, String pathExpression, String indentPrefix) {
    return validateNullableLiteralLines(
        map.valueType().literals(),
        map.scalarType(),
        fieldExpression,
        pathExpression,
        indentPrefix);
  }

  private static List<String> validateNullableLiteralLines(
      LiteralConstraints literals,
      JavaScalarType scalarType,
      String fieldExpression,
      String pathExpression,
      String indentPrefix) {
    if (!literals.hasEnum() && !literals.hasConst()) {
      return List.of();
    }
    ArrayList<String> lines = new ArrayList<>();
    lines.add(
        indentPrefix
            + "if ("
            + fieldExpression
            + " != null && !"
            + fieldExpression
            + ".isAbsent()) {");
    if (literals.hasEnum()) {
      lines.add(
          indentPrefix
              + "  if (!validateEnum(errors, "
              + nullableEnumMatchExpression(scalarType, fieldExpression, literals.enumValues())
              + ", "
              + pathExpression
              + ")) {");
      lines.add(indentPrefix + "    return errors.toResult();");
      lines.add(indentPrefix + "  }");
    }
    if (literals.hasConst()) {
      lines.add(
          indentPrefix
              + "  if (!validateConst(errors, "
              + nullableLiteralMatchExpression(
                  scalarType, fieldExpression, literals.constValue().orElseThrow())
              + ", "
              + pathExpression
              + ")) {");
      lines.add(indentPrefix + "    return errors.toResult();");
      lines.add(indentPrefix + "  }");
    }
    lines.add(indentPrefix + "}");
    return lines;
  }

  private static boolean hasArrayWithMinItems(BindingModel model) {
    return allFields(model).stream()
            .anyMatch(field -> field.array() && field.valueType().minItems().isPresent())
        || allMaps(model).stream()
            .anyMatch(map -> map.array() && map.valueType().minItems().isPresent());
  }

  private static boolean hasArrayWithMaxItems(BindingModel model) {
    return allFields(model).stream()
            .anyMatch(field -> field.array() && field.valueType().maxItems().isPresent())
        || allMaps(model).stream()
            .anyMatch(map -> map.array() && map.valueType().maxItems().isPresent());
  }

  private static boolean hasMinPropertiesConstraint(BindingModel model) {
    return allObjects(model).stream()
        .anyMatch(object -> object.validationConstraints().minProperties().isPresent());
  }

  private static boolean hasMaxPropertiesConstraint(BindingModel model) {
    return allObjects(model).stream()
        .anyMatch(object -> object.validationConstraints().maxProperties().isPresent());
  }

  private static String arrayGuard(FieldBinding field, String ownerExpression) {
    String fieldExpression = accessor(field, ownerExpression);
    if (field.valueType().nullable()) {
      return fieldExpression + " != null && " + fieldExpression + ".hasValue()";
    }
    if (field.required()) {
      return fieldExpression + " != null";
    }
    return fieldExpression + " != null && " + fieldExpression + ".isPresent()";
  }

  private static String arrayValueExpression(FieldBinding field, String ownerExpression) {
    String fieldExpression = accessor(field, ownerExpression);
    if (field.valueType().nullable()) {
      return fieldExpression + ".requireValue()";
    }
    if (field.required()) {
      return fieldExpression;
    }
    return fieldExpression + ".orElseThrow()";
  }

  private static String scalarValueExpression(FieldBinding field, String ownerExpression) {
    String fieldExpression = accessor(field, ownerExpression);
    if (field.valueType().nullable()) {
      return fieldExpression + ".requireValue()";
    }
    if (field.required()) {
      return fieldExpression;
    }
    return fieldExpression + ".orElseThrow()";
  }

  private static String scalarGuard(FieldBinding field, String ownerExpression) {
    String fieldExpression = accessor(field, ownerExpression);
    if (field.valueType().nullable()) {
      return fieldExpression + " != null && " + fieldExpression + ".hasValue()";
    }
    if (field.required() && field.scalarType() == JavaScalarType.STRING) {
      return fieldExpression + " != null";
    }
    if (field.required()) {
      return "true";
    }
    return fieldExpression + " != null && " + fieldExpression + ".isPresent()";
  }

  private static String numericGuard(
      FieldBinding field, String valueExpression, String ownerExpression) {
    if (field.scalarType() == JavaScalarType.NUMBER) {
      String finiteGuard = "Double.isFinite(" + valueExpression + ")";
      if (field.required()) {
        return finiteGuard;
      }
      return scalarGuard(field, ownerExpression) + " && " + finiteGuard;
    }
    return scalarGuard(field, ownerExpression);
  }

  private static String literalGuard(
      FieldBinding field, String valueExpression, String ownerExpression) {
    if (field.scalarType() == JavaScalarType.NUMBER) {
      return numericGuard(field, valueExpression, ownerExpression);
    }
    return scalarGuard(field, ownerExpression);
  }

  private static String numericValueExpression(JavaScalarType scalarType, String valueExpression) {
    return switch (scalarType) {
      case INTEGER, NUMBER -> "BigDecimal.valueOf(" + valueExpression + ")";
      case STRING, BOOLEAN ->
          throw new IllegalArgumentException("numeric facets are not valid for " + scalarType);
    };
  }

  private static String propertyPathExpression(FieldBinding field, String basePathExpression) {
    return basePathExpression + ".property(" + javaStringLiteral(field.jsonPropertyName()) + ")";
  }

  private static String accessor(FieldBinding field, String ownerExpression) {
    return ownerExpression + "." + field.javaFieldName() + "()";
  }

  private static List<String> indent(List<String> lines, String indent) {
    return lines.stream().map(line -> indent + line).toList();
  }

  private static boolean hasMinLengthFacet(BindingModel model) {
    return allFields(model).stream()
            .anyMatch(field -> field.valueType().facets().minLength().isPresent())
        || allMaps(model).stream().anyMatch(map -> map.valueType().facets().minLength().isPresent())
        || allObjects(model).stream()
            .anyMatch(
                object ->
                    object
                        .validationConstraints()
                        .propertyNames()
                        .map(facets -> facets.minLength().isPresent())
                        .orElse(false));
  }

  private static boolean hasMaxLengthFacet(BindingModel model) {
    return allFields(model).stream()
            .anyMatch(field -> field.valueType().facets().maxLength().isPresent())
        || allMaps(model).stream().anyMatch(map -> map.valueType().facets().maxLength().isPresent())
        || allObjects(model).stream()
            .anyMatch(
                object ->
                    object
                        .validationConstraints()
                        .propertyNames()
                        .map(facets -> facets.maxLength().isPresent())
                        .orElse(false));
  }

  private static boolean hasPatternFacet(BindingModel model) {
    return allFields(model).stream()
            .anyMatch(field -> field.valueType().facets().pattern().isPresent())
        || allMaps(model).stream().anyMatch(map -> map.valueType().facets().pattern().isPresent())
        || allObjects(model).stream()
            .anyMatch(
                object ->
                    object
                        .validationConstraints()
                        .propertyNames()
                        .map(facets -> facets.pattern().isPresent())
                        .orElse(false));
  }

  private static boolean hasFormatFacet(BindingModel model) {
    return allFields(model).stream()
            .anyMatch(field -> field.valueType().facets().format().isPresent())
        || allMaps(model).stream().anyMatch(map -> map.valueType().facets().format().isPresent())
        || allObjects(model).stream()
            .anyMatch(
                object ->
                    object
                        .validationConstraints()
                        .propertyNames()
                        .map(facets -> facets.format().isPresent())
                        .orElse(false));
  }

  private static boolean hasMinimumFacet(BindingModel model) {
    return allFields(model).stream()
            .anyMatch(field -> field.valueType().facets().minimum().isPresent())
        || allMaps(model).stream().anyMatch(map -> map.valueType().facets().minimum().isPresent());
  }

  private static boolean hasMaximumFacet(BindingModel model) {
    return allFields(model).stream()
            .anyMatch(field -> field.valueType().facets().maximum().isPresent())
        || allMaps(model).stream().anyMatch(map -> map.valueType().facets().maximum().isPresent());
  }

  private static boolean hasExclusiveMinimumFacet(BindingModel model) {
    return allFields(model).stream()
            .anyMatch(field -> field.valueType().facets().exclusiveMinimum().isPresent())
        || allMaps(model).stream()
            .anyMatch(map -> map.valueType().facets().exclusiveMinimum().isPresent());
  }

  private static boolean hasExclusiveMaximumFacet(BindingModel model) {
    return allFields(model).stream()
            .anyMatch(field -> field.valueType().facets().exclusiveMaximum().isPresent())
        || allMaps(model).stream()
            .anyMatch(map -> map.valueType().facets().exclusiveMaximum().isPresent());
  }

  private static boolean hasNumericFacet(BindingModel model) {
    return allFields(model).stream()
            .anyMatch(field -> field.valueType().facets().hasNumericFacets())
        || allMaps(model).stream().anyMatch(map -> map.valueType().facets().hasNumericFacets());
  }

  private static boolean hasEnumConstraint(BindingModel model) {
    return allFields(model).stream().anyMatch(field -> field.valueType().literals().hasEnum())
        || allMaps(model).stream().anyMatch(map -> map.valueType().literals().hasEnum());
  }

  private static boolean hasConstConstraint(BindingModel model) {
    return allFields(model).stream().anyMatch(field -> field.valueType().literals().hasConst())
        || allMaps(model).stream().anyMatch(map -> map.valueType().literals().hasConst());
  }

  private static boolean hasNumberLiteralConstraint(BindingModel model) {
    return allFields(model).stream()
            .anyMatch(
                field ->
                    field.scalarType() == JavaScalarType.NUMBER
                        && (field.valueType().literals().hasEnum()
                            || field.valueType().literals().hasConst()))
        || allMaps(model).stream()
            .anyMatch(
                map ->
                    !map.object()
                        && map.scalarType() == JavaScalarType.NUMBER
                        && (map.valueType().literals().hasEnum()
                            || map.valueType().literals().hasConst()));
  }

  private static List<FieldBinding> allFields(BindingModel model) {
    ArrayList<FieldBinding> fields = new ArrayList<>();
    if (model.taggedUnion().isEmpty()) {
      collectFields(model.rootObject(), fields);
      return List.copyOf(fields);
    }
    for (TaggedUnionBranch branch : model.taggedUnion().orElseThrow().branches()) {
      collectFields(branch.object(), fields);
    }
    return List.copyOf(fields);
  }

  private static List<MapBinding> allMaps(BindingModel model) {
    ArrayList<MapBinding> maps = new ArrayList<>();
    if (model.taggedUnion().isEmpty()) {
      collectMaps(model.rootObject(), maps);
      return List.copyOf(maps);
    }
    for (TaggedUnionBranch branch : model.taggedUnion().orElseThrow().branches()) {
      collectMaps(branch.object(), maps);
    }
    return List.copyOf(maps);
  }

  private static void collectMaps(ObjectBinding object, List<MapBinding> maps) {
    object.patternProperties().ifPresent(maps::add);
    object.additionalProperties().ifPresent(maps::add);
    for (FieldBinding field : object.fields()) {
      field.valueType().objectBinding().ifPresent(nested -> collectMaps(nested, maps));
    }
    object
        .patternProperties()
        .flatMap(map -> map.valueType().objectBinding())
        .ifPresent(nested -> collectMaps(nested, maps));
    object
        .additionalProperties()
        .flatMap(map -> map.valueType().objectBinding())
        .ifPresent(nested -> collectMaps(nested, maps));
  }

  private static void collectFields(ObjectBinding object, List<FieldBinding> fields) {
    for (FieldBinding field : object.fields()) {
      fields.add(field);
      field.valueType().objectBinding().ifPresent(nested -> collectFields(nested, fields));
    }
    object
        .patternProperties()
        .flatMap(map -> map.valueType().objectBinding())
        .ifPresent(nested -> collectFields(nested, fields));
    object
        .additionalProperties()
        .flatMap(map -> map.valueType().objectBinding())
        .ifPresent(nested -> collectFields(nested, fields));
  }

  private static List<ObjectBinding> nestedObjects(BindingModel model) {
    ArrayList<ObjectBinding> objects = new ArrayList<>();
    if (model.taggedUnion().isEmpty()) {
      collectNestedObjects(model.rootObject(), objects);
    } else {
      for (TaggedUnionBranch branch : model.taggedUnion().orElseThrow().branches()) {
        collectNestedObjects(branch.object(), objects);
      }
    }
    return List.copyOf(objects);
  }

  private static List<ObjectBinding> allObjects(BindingModel model) {
    ArrayList<ObjectBinding> objects = new ArrayList<>();
    if (model.taggedUnion().isEmpty()) {
      objects.add(model.rootObject());
      collectNestedObjects(model.rootObject(), objects);
    } else {
      for (TaggedUnionBranch branch : model.taggedUnion().orElseThrow().branches()) {
        objects.add(branch.object());
        collectNestedObjects(branch.object(), objects);
      }
    }
    return List.copyOf(objects);
  }

  private static void collectNestedObjects(ObjectBinding object, List<ObjectBinding> objects) {
    for (FieldBinding field : object.fields()) {
      field
          .valueType()
          .objectBinding()
          .ifPresent(
              nested -> {
                objects.add(nested);
                collectNestedObjects(nested, objects);
              });
    }
    object
        .patternProperties()
        .flatMap(map -> map.valueType().objectBinding())
        .ifPresent(
            nested -> {
              objects.add(nested);
              collectNestedObjects(nested, objects);
            });
    object
        .additionalProperties()
        .flatMap(map -> map.valueType().objectBinding())
        .ifPresent(
            nested -> {
              objects.add(nested);
              collectNestedObjects(nested, objects);
            });
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

  private static List<String> validateMinPropertiesHelper() {
    return List.of(
        "",
        "  private static boolean validateMinProperties(",
        "      ValidationErrors errors, long propertyCount, long minProperties, JsonPath path) {",
        "    if (propertyCount >= minProperties) {",
        "      return true;",
        "    }",
        "    return errors.add(",
        "        ValidationError.of(",
        "            \"MJJBV-018\",",
        "            \"Expected at least \" + minProperties + \" object properties.\",",
        "            path));",
        "  }");
  }

  private static List<String> validateMaxPropertiesHelper() {
    return List.of(
        "",
        "  private static boolean validateMaxProperties(",
        "      ValidationErrors errors, long propertyCount, long maxProperties, JsonPath path) {",
        "    if (propertyCount <= maxProperties) {",
        "      return true;",
        "    }",
        "    return errors.add(",
        "        ValidationError.of(",
        "            \"MJJBV-019\",",
        "            \"Expected at most \" + maxProperties + \" object properties.\",",
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

  private static List<String> validateEnumHelper() {
    return List.of(
        "",
        "  private static boolean validateEnum(",
        "      ValidationErrors errors, boolean matches, JsonPath path) {",
        "    if (matches) {",
        "      return true;",
        "    }",
        "    return errors.add(",
        "        ValidationError.of(\"MJJBV-015\", \"Expected value to match enum.\", path));",
        "  }");
  }

  private static List<String> validateConstHelper() {
    return List.of(
        "",
        "  private static boolean validateConst(",
        "      ValidationErrors errors, boolean matches, JsonPath path) {",
        "    if (matches) {",
        "      return true;",
        "    }",
        "    return errors.add(",
        "        ValidationError.of(\"MJJBV-016\", \"Expected value to match const.\", path));",
        "  }");
  }

  private static String enumMatchExpression(
      JavaScalarType scalarType, String valueExpression, List<LiteralValue> enumValues) {
    List<String> expressions =
        enumValues.stream()
            .filter(literal -> literal.kind() != LiteralValue.Kind.NULL)
            .map(literal -> literalMatchExpression(scalarType, valueExpression, literal))
            .toList();
    if (expressions.isEmpty()) {
      return "false";
    }
    return String.join(" || ", expressions);
  }

  private static String literalMatchExpression(
      JavaScalarType scalarType, String valueExpression, LiteralValue literal) {
    if (literal.kind() == LiteralValue.Kind.NULL) {
      return "false";
    }
    return switch (scalarType) {
      case STRING -> valueExpression + ".equals(" + javaStringLiteral(literal.value()) + ")";
      case INTEGER -> valueExpression + " == " + literal.value() + "L";
      case NUMBER ->
          "BigDecimal.valueOf("
              + valueExpression
              + ").compareTo(new BigDecimal("
              + javaStringLiteral(literal.value())
              + ")) == 0";
      case BOOLEAN -> valueExpression + " == " + literal.value();
    };
  }

  private static String nullableEnumMatchExpression(
      JavaScalarType scalarType, String fieldExpression, List<LiteralValue> enumValues) {
    boolean nullAllowed =
        enumValues.stream().anyMatch(literal -> literal.kind() == LiteralValue.Kind.NULL);
    List<String> expressions =
        enumValues.stream()
            .filter(literal -> literal.kind() != LiteralValue.Kind.NULL)
            .map(
                literal ->
                    literalMatchExpression(
                        scalarType, fieldExpression + ".requireValue()", literal))
            .toList();
    String valueMatches = expressions.isEmpty() ? "false" : String.join(" || ", expressions);
    return "("
        + fieldExpression
        + ".isExplicitNull() && "
        + nullAllowed
        + ") || ("
        + fieldExpression
        + ".hasValue() && ("
        + valueMatches
        + "))";
  }

  private static String nullableLiteralMatchExpression(
      JavaScalarType scalarType, String fieldExpression, LiteralValue literal) {
    if (literal.kind() == LiteralValue.Kind.NULL) {
      return fieldExpression + ".isExplicitNull()";
    }
    return fieldExpression
        + ".hasValue() && "
        + literalMatchExpression(scalarType, fieldExpression + ".requireValue()", literal);
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
