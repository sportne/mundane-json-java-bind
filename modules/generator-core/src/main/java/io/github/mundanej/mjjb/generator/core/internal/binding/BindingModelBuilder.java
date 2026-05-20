package io.github.mundanej.mjjb.generator.core.internal.binding;

import io.github.mundanej.mjjb.schema.model.JsonPointer;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.ArrayValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.BooleanValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.Member;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.NullValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.NumberValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.ObjectValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.StringValue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

/** Builds the first-slice binding IR from profile-validated schema syntax. */
public final class BindingModelBuilder {
  private static final Set<String> JAVA_KEYWORDS =
      Set.of(
          "abstract",
          "assert",
          "boolean",
          "break",
          "byte",
          "case",
          "catch",
          "char",
          "class",
          "const",
          "continue",
          "default",
          "do",
          "double",
          "else",
          "enum",
          "extends",
          "final",
          "finally",
          "float",
          "for",
          "goto",
          "if",
          "implements",
          "import",
          "instanceof",
          "int",
          "interface",
          "long",
          "native",
          "new",
          "package",
          "private",
          "protected",
          "public",
          "return",
          "short",
          "static",
          "strictfp",
          "super",
          "switch",
          "synchronized",
          "this",
          "throw",
          "throws",
          "transient",
          "try",
          "void",
          "volatile",
          "while",
          "_");

  public BindingBuildResult build(SchemaSyntaxValue root, String packageName, String rootTypeName) {
    Objects.requireNonNull(root, "root");
    Objects.requireNonNull(packageName, "packageName");
    Objects.requireNonNull(rootTypeName, "rootTypeName");
    ArrayList<BindingDiagnostic> diagnostics = new ArrayList<>();
    if (!(root instanceof ObjectValue rootObject)) {
      diagnostics.add(
          rootTypeDiagnostic("The root schema must be an object schema.", root.pointer()));
      return BindingBuildResult.failure(sorted(diagnostics));
    }
    Optional<Member> oneOfMember = member(rootObject, "oneOf");
    if (oneOfMember.isPresent()) {
      return buildTaggedUnion(rootObject, oneOfMember.get(), packageName, rootTypeName);
    }

    validateRootType(rootObject, diagnostics);
    validateAdditionalProperties(rootObject, diagnostics);
    validateRootLiteralConstraints(rootObject, diagnostics);
    ObjectValue properties = propertiesObject(rootObject);
    RequiredNames requiredNames = requiredNames(rootObject);
    diagnostics.addAll(requiredNames.diagnostics());
    ArrayList<FieldBinding> fields = new ArrayList<>();
    HashSet<String> declaredProperties = new HashSet<>();
    HashMap<String, JsonPointer> javaNames = new HashMap<>();

    if (properties != null) {
      for (Member property : properties.members()) {
        declaredProperties.add(property.name());
        if (!(property.value() instanceof ObjectValue propertySchema)) {
          diagnostics.add(
              unsupportedPropertyType(
                  "Property '" + property.name() + "' must be described by a schema object.",
                  property.pointer()));
          continue;
        }
        Optional<Member> typeMember = member(propertySchema, "type");
        if (typeMember.isEmpty()) {
          diagnostics.add(
              missingPropertyType(
                  "Property '" + property.name() + "' must declare a scalar 'type'.",
                  property.pointer()));
          continue;
        }
        Optional<FieldValueType> valueType =
            valueType(property.name(), propertySchema, typeMember.get(), diagnostics);
        if (valueType.isEmpty()) {
          continue;
        }
        String javaFieldName = toJavaFieldName(property.name());
        JsonPointer existingPointer = javaNames.putIfAbsent(javaFieldName, property.pointer());
        if (existingPointer != null) {
          diagnostics.add(
              nameCollision(
                  "Property '"
                      + property.name()
                      + "' maps to Java field name '"
                      + javaFieldName
                      + "', which is already used by another property.",
                  property.pointer()));
          continue;
        }
        fields.add(
            new FieldBinding(
                property.name(),
                javaFieldName,
                valueType.get(),
                requiredNames.names().contains(property.name()),
                property.pointer(),
                annotations(propertySchema)));
      }
    }

    for (Map.Entry<String, JsonPointer> requiredName : requiredNames.pointers().entrySet()) {
      if (!declaredProperties.contains(requiredName.getKey())) {
        diagnostics.add(
            unknownRequired(
                "Required property '" + requiredName.getKey() + "' is not declared in properties.",
                requiredName.getValue()));
      }
    }

    List<BindingDiagnostic> sortedDiagnostics = sorted(diagnostics);
    if (!sortedDiagnostics.isEmpty()) {
      return BindingBuildResult.failure(sortedDiagnostics);
    }
    ObjectBinding rootBinding =
        new ObjectBinding(rootTypeName, root.pointer(), fields, annotations(rootObject));
    return BindingBuildResult.success(new BindingModel(packageName, rootTypeName, rootBinding));
  }

  private static BindingBuildResult buildTaggedUnion(
      ObjectValue rootObject, Member oneOfMember, String packageName, String rootTypeName) {
    ArrayList<BindingDiagnostic> diagnostics = new ArrayList<>();
    if (member(rootObject, "type").isPresent() || member(rootObject, "properties").isPresent()) {
      diagnostics.add(
          unsupportedOneOf(
              "Root tagged oneOf schemas must not also declare root object binding keywords.",
              oneOfMember.pointer()));
      return BindingBuildResult.failure(sorted(diagnostics));
    }
    if (!(oneOfMember.value() instanceof ArrayValue oneOf) || oneOf.items().size() < 2) {
      diagnostics.add(
          unsupportedOneOf(
              "Tagged oneOf binding requires at least two object schema branches.",
              oneOfMember.pointer()));
      return BindingBuildResult.failure(sorted(diagnostics));
    }
    ArrayList<TaggedUnionBranch> branches = new ArrayList<>();
    HashSet<String> tagValues = new HashSet<>();
    HashSet<String> branchTypeNames = new HashSet<>();
    String tagPropertyName = null;
    for (int index = 0; index < oneOf.items().size(); index++) {
      SchemaSyntaxValue item = oneOf.items().get(index);
      if (!(item instanceof ObjectValue branchSchema)) {
        diagnostics.add(
            unsupportedOneOf("Tagged oneOf branches must be object schemas.", item.pointer()));
        continue;
      }
      validateRootType(branchSchema, diagnostics);
      validateAdditionalProperties(branchSchema, diagnostics);
      validateRootLiteralConstraints(branchSchema, diagnostics);
      ObjectValue properties = propertiesObject(branchSchema);
      RequiredNames requiredNames = requiredNames(branchSchema);
      diagnostics.addAll(requiredNames.diagnostics());
      if (properties == null) {
        diagnostics.add(
            unsupportedOneOf(
                "Tagged oneOf branches must declare object properties.", branchSchema.pointer()));
        continue;
      }
      Optional<TagProperty> tagProperty = tagProperty(properties, requiredNames, diagnostics);
      if (tagProperty.isEmpty()) {
        diagnostics.add(
            unsupportedOneOf(
                "Tagged oneOf branches must have one required string const tag property.",
                branchSchema.pointer()));
        continue;
      }
      if (tagPropertyName == null) {
        tagPropertyName = tagProperty.get().name();
      } else if (!tagPropertyName.equals(tagProperty.get().name())) {
        diagnostics.add(
            unsupportedOneOf(
                "Tagged oneOf branches must use the same tag property name.",
                tagProperty.get().pointer()));
        continue;
      }
      if (!tagValues.add(tagProperty.get().value())) {
        diagnostics.add(
            unsupportedOneOf(
                "Tagged oneOf branch tag values must be unique.", tagProperty.get().pointer()));
        continue;
      }
      String branchTypeName = toJavaTypeName(tagProperty.get().value(), index);
      if (!branchTypeNames.add(branchTypeName)) {
        diagnostics.add(
            nameCollision(
                "Tagged oneOf branch tag value '"
                    + tagProperty.get().value()
                    + "' maps to Java type name '"
                    + branchTypeName
                    + "', which is already used by another branch.",
                tagProperty.get().pointer()));
        continue;
      }
      List<FieldBinding> fields =
          branchFields(properties, requiredNames, tagProperty.get(), diagnostics);
      branches.add(
          new TaggedUnionBranch(
              tagProperty.get().value(),
              new ObjectBinding(
                  branchTypeName, branchSchema.pointer(), fields, annotations(branchSchema))));
    }
    List<BindingDiagnostic> sortedDiagnostics = sorted(diagnostics);
    if (!sortedDiagnostics.isEmpty()) {
      return BindingBuildResult.failure(sortedDiagnostics);
    }
    TaggedUnionBinding union =
        new TaggedUnionBinding(tagPropertyName, oneOfMember.pointer(), branches);
    ObjectBinding rootBinding =
        new ObjectBinding(rootTypeName, rootObject.pointer(), List.of(), annotations(rootObject));
    return BindingBuildResult.success(
        new BindingModel(packageName, rootTypeName, rootBinding, Optional.of(union)));
  }

  private static List<FieldBinding> branchFields(
      ObjectValue properties,
      RequiredNames requiredNames,
      TagProperty tagProperty,
      List<BindingDiagnostic> diagnostics) {
    ArrayList<FieldBinding> fields = new ArrayList<>();
    HashSet<String> declaredProperties = new HashSet<>();
    HashMap<String, JsonPointer> javaNames = new HashMap<>();
    for (Member property : properties.members()) {
      declaredProperties.add(property.name());
      if (tagProperty.name().equals(property.name())) {
        continue;
      }
      if (!(property.value() instanceof ObjectValue propertySchema)) {
        diagnostics.add(
            unsupportedPropertyType(
                "Property '" + property.name() + "' must be described by a schema object.",
                property.pointer()));
        continue;
      }
      Optional<Member> typeMember = member(propertySchema, "type");
      if (typeMember.isEmpty()) {
        diagnostics.add(
            missingPropertyType(
                "Property '" + property.name() + "' must declare a scalar 'type'.",
                property.pointer()));
        continue;
      }
      Optional<FieldValueType> valueType =
          valueType(property.name(), propertySchema, typeMember.get(), diagnostics);
      if (valueType.isEmpty()) {
        continue;
      }
      String javaFieldName = toJavaFieldName(property.name());
      JsonPointer existingPointer = javaNames.putIfAbsent(javaFieldName, property.pointer());
      if (existingPointer != null) {
        diagnostics.add(
            nameCollision(
                "Property '"
                    + property.name()
                    + "' maps to Java field name '"
                    + javaFieldName
                    + "', which is already used by another property.",
                property.pointer()));
        continue;
      }
      fields.add(
          new FieldBinding(
              property.name(),
              javaFieldName,
              valueType.get(),
              requiredNames.names().contains(property.name()),
              property.pointer(),
              annotations(propertySchema)));
    }
    for (Map.Entry<String, JsonPointer> requiredName : requiredNames.pointers().entrySet()) {
      if (!declaredProperties.contains(requiredName.getKey())) {
        diagnostics.add(
            unknownRequired(
                "Required property '" + requiredName.getKey() + "' is not declared in properties.",
                requiredName.getValue()));
      }
    }
    return fields;
  }

  private static void validateRootType(
      ObjectValue rootObject, List<BindingDiagnostic> diagnostics) {
    Optional<Member> typeMember = member(rootObject, "type");
    if (typeMember.isEmpty()) {
      diagnostics.add(
          rootTypeDiagnostic(
              "The root schema must declare 'type' as 'object' for generated object binding.",
              rootObject.pointer()));
      return;
    }
    SchemaSyntaxValue value = typeMember.get().value();
    if (value instanceof StringValue stringValue && "object".equals(stringValue.value())) {
      return;
    }
    diagnostics.add(
        rootTypeDiagnostic(
            "The root schema 'type' must be the string value 'object' for generated object binding.",
            value.pointer()));
  }

  private static void validateAdditionalProperties(
      ObjectValue rootObject, List<BindingDiagnostic> diagnostics) {
    Optional<Member> additionalProperties = member(rootObject, "additionalProperties");
    if (additionalProperties.isEmpty()) {
      diagnostics.add(
          additionalPropertiesDiagnostic(
              "The root object schema must declare 'additionalProperties' as false.",
              rootObject.pointer()));
      return;
    }
    SchemaSyntaxValue value = additionalProperties.get().value();
    if (value instanceof BooleanValue booleanValue && !booleanValue.value()) {
      return;
    }
    diagnostics.add(
        additionalPropertiesDiagnostic(
            "The root object schema must declare 'additionalProperties' as false.",
            value.pointer()));
  }

  private static void validateRootLiteralConstraints(
      ObjectValue rootObject, List<BindingDiagnostic> diagnostics) {
    Optional<Member> literalConstraint = firstLiteralConstraint(rootObject);
    if (literalConstraint.isPresent()) {
      diagnostics.add(
          unsupportedLiteralConstraint(
              "Root object enum, const, and default constraints are not supported in this binding slice.",
              literalConstraint.get().pointer()));
    }
  }

  private static ObjectValue propertiesObject(ObjectValue rootObject) {
    Optional<Member> propertiesMember = member(rootObject, "properties");
    if (propertiesMember.isPresent()
        && propertiesMember.get().value() instanceof ObjectValue objectValue) {
      return objectValue;
    }
    return null;
  }

  private static RequiredNames requiredNames(ObjectValue rootObject) {
    Optional<Member> requiredMember = member(rootObject, "required");
    if (requiredMember.isEmpty()
        || !(requiredMember.get().value() instanceof ArrayValue required)) {
      return new RequiredNames(Set.of(), Map.of(), List.of());
    }
    HashSet<String> names = new HashSet<>();
    HashMap<String, JsonPointer> pointers = new HashMap<>();
    for (SchemaSyntaxValue item : required.items()) {
      if (item instanceof StringValue stringValue) {
        names.add(stringValue.value());
        pointers.put(stringValue.value(), item.pointer());
      }
    }
    return new RequiredNames(names, pointers, List.of());
  }

  private static Optional<Member> member(ObjectValue objectValue, String name) {
    for (Member member : objectValue.members()) {
      if (name.equals(member.name())) {
        return Optional.of(member);
      }
    }
    return Optional.empty();
  }

  private static SchemaAnnotationsBinding annotations(ObjectValue schema) {
    return new SchemaAnnotationsBinding(
        stringAnnotation(schema, "title"),
        stringAnnotation(schema, "description"),
        stringAnnotation(schema, "$comment"),
        examplesJson(schema),
        booleanAnnotation(schema, "deprecated"),
        booleanAnnotation(schema, "readOnly"),
        booleanAnnotation(schema, "writeOnly"),
        member(schema, "default").map(member -> compactJson(member.value())));
  }

  private static Optional<String> stringAnnotation(ObjectValue schema, String name) {
    return member(schema, name)
        .filter(member -> member.value() instanceof StringValue)
        .map(member -> ((StringValue) member.value()).value());
  }

  private static Optional<Boolean> booleanAnnotation(ObjectValue schema, String name) {
    return member(schema, name)
        .filter(member -> member.value() instanceof BooleanValue)
        .map(member -> ((BooleanValue) member.value()).value());
  }

  private static List<String> examplesJson(ObjectValue schema) {
    Optional<Member> examples = member(schema, "examples");
    if (examples.isEmpty() || !(examples.get().value() instanceof ArrayValue arrayValue)) {
      return List.of();
    }
    return arrayValue.items().stream().map(BindingModelBuilder::compactJson).toList();
  }

  private static String compactJson(SchemaSyntaxValue value) {
    StringBuilder builder = new StringBuilder();
    appendCompactJson(builder, value);
    return builder.toString();
  }

  private static void appendCompactJson(StringBuilder builder, SchemaSyntaxValue value) {
    switch (value) {
      case ObjectValue objectValue -> {
        builder.append('{');
        for (int index = 0; index < objectValue.members().size(); index++) {
          if (index > 0) {
            builder.append(',');
          }
          Member member = objectValue.members().get(index);
          appendJsonString(builder, member.name());
          builder.append(':');
          appendCompactJson(builder, member.value());
        }
        builder.append('}');
      }
      case ArrayValue arrayValue -> {
        builder.append('[');
        for (int index = 0; index < arrayValue.items().size(); index++) {
          if (index > 0) {
            builder.append(',');
          }
          appendCompactJson(builder, arrayValue.items().get(index));
        }
        builder.append(']');
      }
      case StringValue stringValue -> appendJsonString(builder, stringValue.value());
      case NumberValue numberValue -> builder.append(numberValue.literal());
      case BooleanValue booleanValue -> builder.append(booleanValue.value());
      case NullValue ignored -> builder.append("null");
    }
  }

  private static void appendJsonString(StringBuilder builder, String value) {
    builder.append('"');
    for (int index = 0; index < value.length(); index++) {
      char current = value.charAt(index);
      switch (current) {
        case '"' -> builder.append("\\\"");
        case '\\' -> builder.append("\\\\");
        case '\b' -> builder.append("\\b");
        case '\f' -> builder.append("\\f");
        case '\n' -> builder.append("\\n");
        case '\r' -> builder.append("\\r");
        case '\t' -> builder.append("\\t");
        default -> {
          if (current < 0x20) {
            builder.append(String.format("\\u%04x", (int) current));
          } else {
            builder.append(current);
          }
        }
      }
    }
    builder.append('"');
  }

  private static Optional<JavaScalarType> scalarType(SchemaSyntaxValue value) {
    Optional<String> schemaType = singleTypeName(value);
    if (schemaType.isEmpty()) {
      return Optional.empty();
    }
    for (JavaScalarType candidate : JavaScalarType.values()) {
      if (candidate.schemaType().equals(schemaType.get())) {
        return Optional.of(candidate);
      }
    }
    return Optional.empty();
  }

  private static Optional<JavaScalarType> nullableScalarType(SchemaSyntaxValue value) {
    Optional<String> schemaType = nullableTypeName(value);
    if (schemaType.isEmpty()) {
      return Optional.empty();
    }
    for (JavaScalarType candidate : JavaScalarType.values()) {
      if (candidate.schemaType().equals(schemaType.get())) {
        return Optional.of(candidate);
      }
    }
    return Optional.empty();
  }

  private static Optional<String> singleTypeName(SchemaSyntaxValue value) {
    if (value instanceof StringValue stringValue) {
      return Optional.of(stringValue.value());
    }
    return Optional.empty();
  }

  private static Optional<String> nullableTypeName(SchemaSyntaxValue value) {
    if (!(value instanceof ArrayValue arrayValue) || arrayValue.items().size() != 2) {
      return Optional.empty();
    }
    ArrayList<String> names = new ArrayList<>();
    for (SchemaSyntaxValue item : arrayValue.items()) {
      if (item instanceof StringValue stringValue) {
        names.add(stringValue.value());
      }
    }
    if (names.size() == 2 && names.contains("null")) {
      return names.stream().filter(name -> !"null".equals(name)).findFirst();
    }
    return Optional.empty();
  }

  private static Optional<FieldValueType> valueType(
      String propertyName,
      ObjectValue propertySchema,
      Member typeMember,
      List<BindingDiagnostic> diagnostics) {
    Optional<JavaScalarType> scalarType = scalarType(typeMember.value());
    if (scalarType.isPresent()) {
      Optional<LiteralConstraints> literals =
          literalConstraints(propertySchema, scalarType.get(), propertyName, diagnostics);
      return literals.map(
          constraints ->
              FieldValueType.scalar(scalarType.get(), facets(propertySchema), constraints));
    }
    Optional<JavaScalarType> nullableScalarType = nullableScalarType(typeMember.value());
    if (nullableScalarType.isPresent()) {
      Optional<LiteralConstraints> literals =
          literalConstraints(propertySchema, nullableScalarType.get(), propertyName, diagnostics);
      return literals.map(
          constraints ->
              FieldValueType.nullableScalar(
                  nullableScalarType.get(), facets(propertySchema), constraints));
    }
    if (singleTypeName(typeMember.value()).filter("array"::equals).isPresent()) {
      return arrayType(propertyName, propertySchema, false, diagnostics);
    }
    if (nullableTypeName(typeMember.value()).filter("array"::equals).isPresent()) {
      return arrayType(propertyName, propertySchema, true, diagnostics);
    }
    diagnostics.add(
        unsupportedPropertyType(
            "Property '"
                + propertyName
                + "' must use a supported scalar type or homogeneous scalar array.",
            typeMember.pointer()));
    return Optional.empty();
  }

  private static Optional<FieldValueType> arrayType(
      String propertyName,
      ObjectValue propertySchema,
      boolean nullable,
      List<BindingDiagnostic> diagnostics) {
    Optional<Member> itemsMember = member(propertySchema, "items");
    if (itemsMember.isEmpty()) {
      diagnostics.add(
          missingArrayItems(
              "Array property '" + propertyName + "' must declare an 'items' schema.",
              propertySchema.pointer()));
      return Optional.empty();
    }
    if (!(itemsMember.get().value() instanceof ObjectValue itemsSchema)) {
      diagnostics.add(
          unsupportedPropertyType(
              "Array property '" + propertyName + "' must use an object 'items' schema.",
              itemsMember.get().pointer()));
      return Optional.empty();
    }
    Optional<Member> itemTypeMember = member(itemsSchema, "type");
    if (itemTypeMember.isEmpty()) {
      diagnostics.add(
          missingArrayItemType(
              "Array property '" + propertyName + "' items must declare a scalar 'type'.",
              itemsMember.get().pointer()));
      return Optional.empty();
    }
    Optional<JavaScalarType> itemType = scalarType(itemTypeMember.get().value());
    if (itemType.isEmpty()) {
      diagnostics.add(
          unsupportedPropertyType(
              "Array property '"
                  + propertyName
                  + "' items must use one of the scalar types string, integer, number, or boolean.",
              itemTypeMember.get().pointer()));
      return Optional.empty();
    }
    OptionalLong minItems = nonNegativeIntegerMember(propertySchema, "minItems");
    OptionalLong maxItems = nonNegativeIntegerMember(propertySchema, "maxItems");
    if (minItems.isPresent()
        && maxItems.isPresent()
        && minItems.getAsLong() > maxItems.getAsLong()) {
      diagnostics.add(
          invalidArrayBounds(
              "Array property '" + propertyName + "' minItems must not exceed maxItems.",
              member(propertySchema, "maxItems").orElseThrow().pointer()));
      return Optional.empty();
    }
    if (hasLiteralConstraint(propertySchema)) {
      diagnostics.add(
          unsupportedLiteralConstraint(
              "Array property '"
                  + propertyName
                  + "' does not support array-level enum, const, or default in this binding slice.",
              firstLiteralConstraint(propertySchema).orElseThrow().pointer()));
      return Optional.empty();
    }
    Optional<LiteralConstraints> literals =
        literalConstraints(itemsSchema, itemType.get(), propertyName + "[]", diagnostics);
    if (literals.isEmpty()) {
      return Optional.empty();
    }
    if (nullable) {
      return Optional.of(
          FieldValueType.nullableArray(
              itemType.get(), minItems, maxItems, facets(itemsSchema), literals.get()));
    }
    return Optional.of(
        FieldValueType.array(
            itemType.get(), minItems, maxItems, facets(itemsSchema), literals.get()));
  }

  private static OptionalLong nonNegativeIntegerMember(ObjectValue objectValue, String name) {
    Optional<Member> member = member(objectValue, name);
    if (member.isEmpty() || !(member.get().value() instanceof NumberValue numberValue)) {
      return OptionalLong.empty();
    }
    try {
      return OptionalLong.of(Long.parseLong(numberValue.literal()));
    } catch (NumberFormatException exception) {
      return OptionalLong.of(Long.MAX_VALUE);
    }
  }

  private static FacetConstraints facets(ObjectValue schema) {
    return new FacetConstraints(
        nonNegativeIntegerMember(schema, "minLength"),
        nonNegativeIntegerMember(schema, "maxLength"),
        stringMember(schema, "pattern"),
        stringMember(schema, "format"),
        numberLiteralMember(schema, "minimum"),
        numberLiteralMember(schema, "maximum"),
        numberLiteralMember(schema, "exclusiveMinimum"),
        numberLiteralMember(schema, "exclusiveMaximum"));
  }

  private static Optional<String> stringMember(ObjectValue objectValue, String name) {
    Optional<Member> member = member(objectValue, name);
    if (member.isEmpty() || !(member.get().value() instanceof StringValue stringValue)) {
      return Optional.empty();
    }
    return Optional.of(stringValue.value());
  }

  private static Optional<String> numberLiteralMember(ObjectValue objectValue, String name) {
    Optional<Member> member = member(objectValue, name);
    if (member.isEmpty() || !(member.get().value() instanceof NumberValue numberValue)) {
      return Optional.empty();
    }
    return Optional.of(numberValue.literal());
  }

  private static Optional<LiteralConstraints> literalConstraints(
      ObjectValue schema,
      JavaScalarType scalarType,
      String propertyName,
      List<BindingDiagnostic> diagnostics) {
    Optional<Member> enumMember = member(schema, "enum");
    Optional<Member> constMember = member(schema, "const");
    Optional<Member> defaultMember = member(schema, "default");
    ArrayList<LiteralValue> enumValues = new ArrayList<>();
    HashSet<String> enumKeys = new HashSet<>();
    if (enumMember.isPresent()) {
      if (!(enumMember.get().value() instanceof ArrayValue arrayValue)) {
        diagnostics.add(
            invalidLiteralConstraint(
                "Property '" + propertyName + "' enum constraint must be an array.",
                enumMember.get().pointer()));
        return Optional.empty();
      }
      if (arrayValue.items().isEmpty()) {
        diagnostics.add(
            invalidLiteralConstraint(
                "Property '" + propertyName + "' enum constraint must not be empty.",
                enumMember.get().pointer()));
        return Optional.empty();
      }
      for (SchemaSyntaxValue item : arrayValue.items()) {
        Optional<LiteralValue> literal =
            literalValue(item, scalarType, "enum", propertyName, diagnostics);
        if (literal.isEmpty()) {
          return Optional.empty();
        }
        if (!enumKeys.add(literal.get().normalizedKey())) {
          diagnostics.add(
              invalidLiteralConstraint(
                  "Property '"
                      + propertyName
                      + "' enum constraint must contain unique scalar values.",
                  item.pointer()));
          return Optional.empty();
        }
        enumValues.add(literal.get());
      }
    }
    Optional<LiteralValue> constValue = Optional.empty();
    if (constMember.isPresent()) {
      Optional<LiteralValue> literal =
          literalValue(constMember.get().value(), scalarType, "const", propertyName, diagnostics);
      if (literal.isEmpty()) {
        return Optional.empty();
      }
      constValue = literal;
    }
    Optional<LiteralValue> defaultValue = Optional.empty();
    if (defaultMember.isPresent()) {
      Optional<LiteralValue> literal =
          literalValue(
              defaultMember.get().value(), scalarType, "default", propertyName, diagnostics);
      if (literal.isEmpty()) {
        return Optional.empty();
      }
      defaultValue = literal;
    }
    return Optional.of(new LiteralConstraints(enumValues, constValue, defaultValue));
  }

  private static Optional<LiteralValue> literalValue(
      SchemaSyntaxValue value,
      JavaScalarType scalarType,
      String keyword,
      String propertyName,
      List<BindingDiagnostic> diagnostics) {
    if (value instanceof NullValue) {
      return Optional.of(new LiteralValue(LiteralValue.Kind.NULL, ""));
    }
    Optional<LiteralValue> literal =
        switch (scalarType) {
          case STRING -> stringLiteral(value);
          case INTEGER -> integerLiteral(value);
          case NUMBER -> numberLiteral(value);
          case BOOLEAN -> booleanLiteral(value);
        };
    if (literal.isPresent()) {
      return literal;
    }
    diagnostics.add(
        unsupportedLiteralConstraint(
            "Property '"
                + propertyName
                + "' "
                + keyword
                + " constraint must contain only values compatible with "
                + scalarType.schemaType()
                + " bindings plus null.",
            value.pointer()));
    return Optional.empty();
  }

  private static Optional<LiteralValue> stringLiteral(SchemaSyntaxValue value) {
    if (value instanceof StringValue stringValue) {
      return Optional.of(new LiteralValue(LiteralValue.Kind.STRING, stringValue.value()));
    }
    return Optional.empty();
  }

  private static Optional<LiteralValue> integerLiteral(SchemaSyntaxValue value) {
    if (!(value instanceof NumberValue numberValue)) {
      return Optional.empty();
    }
    try {
      return Optional.of(
          new LiteralValue(
              LiteralValue.Kind.INTEGER, Long.toString(Long.parseLong(numberValue.literal()))));
    } catch (NumberFormatException exception) {
      return Optional.empty();
    }
  }

  private static Optional<LiteralValue> numberLiteral(SchemaSyntaxValue value) {
    if (value instanceof NumberValue numberValue) {
      return Optional.of(new LiteralValue(LiteralValue.Kind.NUMBER, numberValue.literal()));
    }
    return Optional.empty();
  }

  private static Optional<LiteralValue> booleanLiteral(SchemaSyntaxValue value) {
    if (value instanceof BooleanValue booleanValue) {
      return Optional.of(
          new LiteralValue(LiteralValue.Kind.BOOLEAN, Boolean.toString(booleanValue.value())));
    }
    return Optional.empty();
  }

  private static boolean hasLiteralConstraint(ObjectValue schema) {
    return firstLiteralConstraint(schema).isPresent();
  }

  private static Optional<Member> firstLiteralConstraint(ObjectValue schema) {
    for (String name : List.of("enum", "const", "default")) {
      Optional<Member> member = member(schema, name);
      if (member.isPresent()) {
        return member;
      }
    }
    return Optional.empty();
  }

  private static Optional<TagProperty> tagProperty(
      ObjectValue properties, RequiredNames requiredNames, List<BindingDiagnostic> diagnostics) {
    ArrayList<TagProperty> candidates = new ArrayList<>();
    for (Member property : properties.members()) {
      if (!requiredNames.names().contains(property.name())) {
        continue;
      }
      if (!(property.value() instanceof ObjectValue propertySchema)) {
        continue;
      }
      Optional<Member> typeMember = member(propertySchema, "type");
      Optional<Member> constMember = member(propertySchema, "const");
      if (typeMember.isEmpty() || constMember.isEmpty()) {
        continue;
      }
      if (!(typeMember.get().value() instanceof StringValue typeValue)
          || !"string".equals(typeValue.value())) {
        diagnostics.add(
            unsupportedOneOf(
                "Tagged oneOf tag properties must use type string.", typeMember.get().pointer()));
        continue;
      }
      if (!(constMember.get().value() instanceof StringValue constValue)) {
        diagnostics.add(
            unsupportedOneOf(
                "Tagged oneOf tag properties must use string const values.",
                constMember.get().pointer()));
        continue;
      }
      candidates.add(new TagProperty(property.name(), constValue.value(), property.pointer()));
    }
    if (candidates.size() == 1) {
      return Optional.of(candidates.getFirst());
    }
    if (candidates.size() > 1) {
      diagnostics.add(
          unsupportedOneOf(
              "Tagged oneOf branches must have exactly one required string const tag property.",
              candidates.get(1).pointer()));
    }
    return Optional.empty();
  }

  private static String toJavaFieldName(String propertyName) {
    ArrayList<String> words = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    for (int index = 0; index < propertyName.length(); index++) {
      char character = propertyName.charAt(index);
      if (isAsciiLetterOrDigit(character)) {
        current.append(character);
      } else if (current.length() > 0) {
        words.add(current.toString());
        current.setLength(0);
      }
    }
    if (current.length() > 0) {
      words.add(current.toString());
    }
    if (words.isEmpty()) {
      words.add("value");
    }
    StringBuilder result = new StringBuilder(firstWord(words.getFirst()));
    for (int index = 1; index < words.size(); index++) {
      result.append(subsequentWord(words.get(index)));
    }
    if (Character.isDigit(result.charAt(0))) {
      result.insert(0, "value");
    }
    String fieldName = result.toString();
    if (JAVA_KEYWORDS.contains(fieldName)) {
      return fieldName + "Value";
    }
    return fieldName;
  }

  private static String toJavaTypeName(String tagValue, int index) {
    ArrayList<String> words = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    for (int characterIndex = 0; characterIndex < tagValue.length(); characterIndex++) {
      char character = tagValue.charAt(characterIndex);
      if (isAsciiLetterOrDigit(character)) {
        current.append(character);
      } else if (current.length() > 0) {
        words.add(current.toString());
        current.setLength(0);
      }
    }
    if (current.length() > 0) {
      words.add(current.toString());
    }
    if (words.isEmpty()) {
      return "Variant" + (index + 1);
    }
    StringBuilder result = new StringBuilder(subsequentWord(words.getFirst()));
    for (int wordIndex = 1; wordIndex < words.size(); wordIndex++) {
      result.append(subsequentWord(words.get(wordIndex)));
    }
    if (Character.isDigit(result.charAt(0))) {
      result.insert(0, "Variant");
    }
    String typeName = result.toString();
    if (JAVA_KEYWORDS.contains(typeName.toLowerCase(Locale.ROOT))) {
      return typeName + "Variant";
    }
    return typeName;
  }

  private static String firstWord(String word) {
    String normalized = normalizeWord(word);
    return normalized.substring(0, 1).toLowerCase(Locale.ROOT) + normalized.substring(1);
  }

  private static String subsequentWord(String word) {
    String normalized = normalizeWord(word);
    return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
  }

  private static String normalizeWord(String word) {
    if (word.chars().anyMatch(Character::isLowerCase)) {
      return word;
    }
    return word.toLowerCase(Locale.ROOT);
  }

  private static boolean isAsciiLetterOrDigit(char character) {
    return (character >= 'a' && character <= 'z')
        || (character >= 'A' && character <= 'Z')
        || (character >= '0' && character <= '9');
  }

  private static BindingDiagnostic rootTypeDiagnostic(String message, JsonPointer pointer) {
    return new BindingDiagnostic(BindingDiagnostic.ROOT_TYPE_CODE, message, pointer);
  }

  private static BindingDiagnostic additionalPropertiesDiagnostic(
      String message, JsonPointer pointer) {
    return new BindingDiagnostic(BindingDiagnostic.ADDITIONAL_PROPERTIES_CODE, message, pointer);
  }

  private static BindingDiagnostic missingPropertyType(String message, JsonPointer pointer) {
    return new BindingDiagnostic(BindingDiagnostic.MISSING_PROPERTY_TYPE_CODE, message, pointer);
  }

  private static BindingDiagnostic unsupportedPropertyType(String message, JsonPointer pointer) {
    return new BindingDiagnostic(
        BindingDiagnostic.UNSUPPORTED_PROPERTY_TYPE_CODE, message, pointer);
  }

  private static BindingDiagnostic missingArrayItems(String message, JsonPointer pointer) {
    return new BindingDiagnostic(BindingDiagnostic.MISSING_ARRAY_ITEMS_CODE, message, pointer);
  }

  private static BindingDiagnostic missingArrayItemType(String message, JsonPointer pointer) {
    return new BindingDiagnostic(BindingDiagnostic.MISSING_ARRAY_ITEM_TYPE_CODE, message, pointer);
  }

  private static BindingDiagnostic invalidArrayBounds(String message, JsonPointer pointer) {
    return new BindingDiagnostic(BindingDiagnostic.INVALID_ARRAY_BOUNDS_CODE, message, pointer);
  }

  private static BindingDiagnostic unsupportedLiteralConstraint(
      String message, JsonPointer pointer) {
    return new BindingDiagnostic(
        BindingDiagnostic.UNSUPPORTED_LITERAL_CONSTRAINT_CODE, message, pointer);
  }

  private static BindingDiagnostic invalidLiteralConstraint(String message, JsonPointer pointer) {
    return new BindingDiagnostic(
        BindingDiagnostic.INVALID_LITERAL_CONSTRAINT_CODE, message, pointer);
  }

  private static BindingDiagnostic unsupportedOneOf(String message, JsonPointer pointer) {
    return new BindingDiagnostic(BindingDiagnostic.UNSUPPORTED_ONE_OF_CODE, message, pointer);
  }

  private static BindingDiagnostic unknownRequired(String message, JsonPointer pointer) {
    return new BindingDiagnostic(BindingDiagnostic.UNKNOWN_REQUIRED_CODE, message, pointer);
  }

  private static BindingDiagnostic nameCollision(String message, JsonPointer pointer) {
    return new BindingDiagnostic(BindingDiagnostic.NAME_COLLISION_CODE, message, pointer);
  }

  private static List<BindingDiagnostic> sorted(List<BindingDiagnostic> diagnostics) {
    ArrayList<BindingDiagnostic> sortedDiagnostics = new ArrayList<>(diagnostics);
    sortedDiagnostics.sort(
        Comparator.comparing((BindingDiagnostic diagnostic) -> diagnostic.pointer().value())
            .thenComparing(BindingDiagnostic::code)
            .thenComparing(BindingDiagnostic::message));
    return List.copyOf(sortedDiagnostics);
  }

  private record RequiredNames(
      Set<String> names, Map<String, JsonPointer> pointers, List<BindingDiagnostic> diagnostics) {
    RequiredNames {
      names = Set.copyOf(Objects.requireNonNull(names, "names"));
      pointers = Map.copyOf(Objects.requireNonNull(pointers, "pointers"));
      diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }
  }

  private record TagProperty(String name, String value, JsonPointer pointer) {
    private TagProperty {
      Objects.requireNonNull(name, "name");
      Objects.requireNonNull(value, "value");
      Objects.requireNonNull(pointer, "pointer");
    }
  }
}
