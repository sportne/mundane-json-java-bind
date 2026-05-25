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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Builds the first-slice binding IR from profile-validated schema syntax. */
public final class BindingModelBuilder {
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

    Optional<ObjectValue> flattenedRoot = flattenedObjectSchema(rootObject, diagnostics);
    if (flattenedRoot.isEmpty()) {
      return BindingBuildResult.failure(sorted(diagnostics));
    }
    ObjectValue bindingRoot = flattenedRoot.get();
    validateRootType(bindingRoot, diagnostics);
    validateRootLiteralConstraints(bindingRoot, diagnostics);
    ObjectValue properties = propertiesObject(bindingRoot);
    RequiredNames requiredNames = requiredNames(bindingRoot);
    diagnostics.addAll(requiredNames.diagnostics());
    HashSet<String> objectTypeNames = new HashSet<>();
    objectTypeNames.add(rootTypeName);
    List<FieldBinding> fields =
        objectFields(properties, requiredNames, null, rootTypeName, objectTypeNames, diagnostics);
    Optional<MapBinding> patternProperties =
        patternPropertiesBinding(bindingRoot, rootTypeName, objectTypeNames, fields, diagnostics);
    Optional<MapBinding> additionalProperties =
        additionalPropertiesBinding(
            bindingRoot, rootTypeName, objectTypeNames, fields, patternProperties, diagnostics);

    List<BindingDiagnostic> sortedDiagnostics = sorted(diagnostics);
    if (!sortedDiagnostics.isEmpty()) {
      return BindingBuildResult.failure(sortedDiagnostics);
    }
    ObjectBinding rootBinding =
        new ObjectBinding(
            rootTypeName,
            root.pointer(),
            fields,
            patternProperties,
            additionalProperties,
            objectValidationConstraints(bindingRoot),
            annotations(bindingRoot));
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
    HashSet<String> objectTypeNames = new HashSet<>();
    objectTypeNames.add(rootTypeName);
    String tagPropertyName = null;
    for (int index = 0; index < oneOf.items().size(); index++) {
      SchemaSyntaxValue item = oneOf.items().get(index);
      if (!(item instanceof ObjectValue branchSchema)) {
        diagnostics.add(
            unsupportedOneOf("Tagged oneOf branches must be object schemas.", item.pointer()));
        continue;
      }
      Optional<ObjectValue> flattenedBranch = flattenedObjectSchema(branchSchema, diagnostics);
      if (flattenedBranch.isEmpty()) {
        continue;
      }
      ObjectValue bindingBranch = flattenedBranch.get();
      validateRootType(bindingBranch, diagnostics);
      validateRootLiteralConstraints(bindingBranch, diagnostics);
      ObjectValue properties = propertiesObject(bindingBranch);
      RequiredNames requiredNames = requiredNames(bindingBranch);
      diagnostics.addAll(requiredNames.diagnostics());
      if (properties == null) {
        diagnostics.add(
            unsupportedOneOf(
                "Tagged oneOf branches must declare object properties.", bindingBranch.pointer()));
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
      String branchTypeName = JavaNameAllocator.branchTypeName(tagProperty.get().value(), index);
      if (!branchTypeNames.add(branchTypeName) || !objectTypeNames.add(branchTypeName)) {
        diagnostics.add(
            nameCollision(
                "Tagged oneOf branch tag value '"
                    + tagProperty.get().value()
                    + "' maps to Java type name '"
                    + branchTypeName
                    + "', which is already used by another branch or nested object.",
                tagProperty.get().pointer()));
        continue;
      }
      List<FieldBinding> fields =
          objectFields(
              properties,
              requiredNames,
              tagProperty.get().name(),
              branchTypeName,
              objectTypeNames,
              diagnostics);
      Optional<MapBinding> patternProperties =
          patternPropertiesBinding(
              bindingBranch, branchTypeName, objectTypeNames, fields, diagnostics);
      Optional<MapBinding> additionalProperties =
          additionalPropertiesBinding(
              bindingBranch,
              branchTypeName,
              objectTypeNames,
              fields,
              patternProperties,
              diagnostics);
      branches.add(
          new TaggedUnionBranch(
              tagProperty.get().value(),
              new ObjectBinding(
                  branchTypeName,
                  branchSchema.pointer(),
                  fields,
                  List.of(tagProperty.get().name()),
                  patternProperties,
                  additionalProperties,
                  objectValidationConstraints(bindingBranch),
                  annotations(bindingBranch))));
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

  private static List<FieldBinding> objectFields(
      ObjectValue properties,
      RequiredNames requiredNames,
      String skippedPropertyName,
      String parentTypeName,
      Set<String> objectTypeNames,
      List<BindingDiagnostic> diagnostics) {
    ArrayList<FieldBinding> fields = new ArrayList<>();
    HashSet<String> declaredProperties = new HashSet<>();
    HashMap<String, JsonPointer> javaNames = new HashMap<>();
    if (properties != null) {
      for (Member property : properties.members()) {
        declaredProperties.add(property.name());
        if (property.name().equals(skippedPropertyName)) {
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
            valueType(
                property.name(),
                propertySchema,
                typeMember.get(),
                parentTypeName,
                objectTypeNames,
                diagnostics);
        if (valueType.isEmpty()) {
          continue;
        }
        String javaFieldName =
            JavaNameAllocator.uniqueFieldName(property.name(), javaNames, property.pointer());
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

  private static Optional<MapBinding> additionalPropertiesBinding(
      ObjectValue rootObject,
      String parentTypeName,
      Set<String> objectTypeNames,
      List<FieldBinding> fields,
      Optional<MapBinding> patternProperties,
      List<BindingDiagnostic> diagnostics) {
    Optional<Member> additionalProperties = member(rootObject, "additionalProperties");
    if (additionalProperties.isEmpty()) {
      diagnostics.add(
          additionalPropertiesDiagnostic(
              "The object schema must declare 'additionalProperties' as false or a supported schema object.",
              rootObject.pointer()));
      return Optional.empty();
    }
    SchemaSyntaxValue value = additionalProperties.get().value();
    if (value instanceof BooleanValue booleanValue && !booleanValue.value()) {
      return Optional.empty();
    }
    if (value instanceof ObjectValue rawValueSchema) {
      Optional<FieldValueType> valueType =
          mapValueType(
              "additionalProperties",
              "additionalProperty",
              rawValueSchema,
              additionalProperties.get().pointer(),
              parentTypeName,
              objectTypeNames,
              diagnostics);
      if (valueType.isEmpty()) {
        return Optional.empty();
      }
      HashMap<String, JsonPointer> javaNames = new HashMap<>();
      for (FieldBinding field : fields) {
        javaNames.put(field.javaFieldName(), field.schemaPointer());
      }
      patternProperties.ifPresent(map -> javaNames.put(map.javaFieldName(), map.schemaPointer()));
      String javaFieldName =
          JavaNameAllocator.uniqueFieldName(
              "additionalProperties", javaNames, additionalProperties.get().pointer());
      return Optional.of(
          MapBinding.additionalProperties(javaFieldName, valueType.get(), value.pointer()));
    }
    diagnostics.add(
        additionalPropertiesDiagnostic(
            "The object schema must declare 'additionalProperties' as false or a supported schema object.",
            value.pointer()));
    return Optional.empty();
  }

  private static Optional<MapBinding> patternPropertiesBinding(
      ObjectValue rootObject,
      String parentTypeName,
      Set<String> objectTypeNames,
      List<FieldBinding> fields,
      List<BindingDiagnostic> diagnostics) {
    Optional<Member> patternProperties = member(rootObject, "patternProperties");
    if (patternProperties.isEmpty()) {
      return Optional.empty();
    }
    if (!(patternProperties.get().value() instanceof ObjectValue patterns)) {
      diagnostics.add(
          patternPropertiesDiagnostic(
              "The 'patternProperties' keyword value must be an object.",
              patternProperties.get().pointer()));
      return Optional.empty();
    }
    if (patterns.members().size() != 1) {
      diagnostics.add(
          patternPropertiesDiagnostic(
              "JSP-DATA-2020-12 supports exactly one patternProperties entry per object.",
              patternProperties.get().pointer()));
      return Optional.empty();
    }
    Member patternMember = patterns.members().getFirst();
    try {
      Pattern.compile(patternMember.name());
    } catch (PatternSyntaxException exception) {
      diagnostics.add(
          patternPropertiesDiagnostic(
              "The patternProperties member name must be a valid regular expression.",
              patternMember.pointer()));
      return Optional.empty();
    }
    if (!(patternMember.value() instanceof ObjectValue valueSchema)) {
      diagnostics.add(
          patternPropertiesDiagnostic(
              "patternProperties value schemas must be schema objects.", patternMember.pointer()));
      return Optional.empty();
    }
    Optional<FieldValueType> valueType =
        mapValueType(
            "patternProperties",
            "patternProperty",
            valueSchema,
            patternMember.pointer(),
            parentTypeName,
            objectTypeNames,
            diagnostics);
    if (valueType.isEmpty()) {
      return Optional.empty();
    }
    HashMap<String, JsonPointer> javaNames = new HashMap<>();
    for (FieldBinding field : fields) {
      javaNames.put(field.javaFieldName(), field.schemaPointer());
    }
    String javaFieldName =
        JavaNameAllocator.uniqueFieldName("patternProperties", javaNames, patternMember.pointer());
    return Optional.of(
        MapBinding.patternProperties(
            javaFieldName, valueType.get(), valueSchema.pointer(), patternMember.name()));
  }

  private static Optional<FieldValueType> mapValueType(
      String keyword,
      String propertyName,
      ObjectValue rawValueSchema,
      JsonPointer diagnosticPointer,
      String parentTypeName,
      Set<String> objectTypeNames,
      List<BindingDiagnostic> diagnostics) {
    Optional<ObjectValue> flattenedValueSchema = flattenedObjectSchema(rawValueSchema, diagnostics);
    if (flattenedValueSchema.isEmpty()) {
      return Optional.empty();
    }
    ObjectValue valueSchema = flattenedValueSchema.get();
    Optional<Member> typeMember = member(valueSchema, "type");
    if (typeMember.isEmpty()) {
      diagnostics.add(
          missingPropertyType(
              keyword + " schema must declare a supported 'type'.", diagnosticPointer));
      return Optional.empty();
    }
    return valueType(
        propertyName, valueSchema, typeMember.get(), parentTypeName, objectTypeNames, diagnostics);
  }

  private static Optional<ObjectValue> flattenedObjectSchema(
      ObjectValue schema, List<BindingDiagnostic> diagnostics) {
    Optional<Member> allOfMember = member(schema, "allOf");
    if (allOfMember.isEmpty()) {
      return Optional.of(schema);
    }
    if (!(allOfMember.get().value() instanceof ArrayValue allOf) || allOf.items().isEmpty()) {
      diagnostics.add(
          unsupportedAllOf(
              "Constrained allOf flattening requires a non-empty array of object schemas.",
              allOfMember.get().pointer()));
      return Optional.empty();
    }
    FlattenedObjectMembers flattened = new FlattenedObjectMembers(schema.pointer());
    mergeObjectSchema(schema, flattened, diagnostics);
    for (SchemaSyntaxValue item : allOf.items()) {
      if (!(item instanceof ObjectValue branch)) {
        diagnostics.add(
            unsupportedAllOf(
                "Constrained allOf flattening supports only object schema branches.",
                item.pointer()));
        continue;
      }
      Optional<ObjectValue> flattenedBranch = flattenedObjectSchema(branch, diagnostics);
      flattenedBranch.ifPresent(value -> mergeObjectSchema(value, flattened, diagnostics));
    }
    validateAllOfAdditionalProperties(flattened, diagnostics);
    if (diagnostics.stream()
        .anyMatch(
            diagnostic -> BindingDiagnostic.UNSUPPORTED_ALL_OF_CODE.equals(diagnostic.code()))) {
      return Optional.empty();
    }
    return Optional.of(flattened.toObjectValue());
  }

  private static void mergeObjectSchema(
      ObjectValue schema, FlattenedObjectMembers flattened, List<BindingDiagnostic> diagnostics) {
    Optional<Member> additionalProperties = member(schema, "additionalProperties");
    additionalProperties
        .filter(member -> !isBooleanTrue(member.value()))
        .ifPresent(
            member ->
                flattened.additionalPropertiesScopes.add(
                    new AdditionalPropertiesScope(member, declaredPropertyNames(schema))));
    for (Member member : schema.members()) {
      switch (member.name()) {
        case "allOf", "$schema", "$defs" -> {
          // These keywords have already been applied or are compile-time-only for binding.
        }
        case "properties" -> mergeProperties(member, flattened, diagnostics);
        case "required" -> mergeRequired(member, flattened);
        case "additionalProperties" ->
            mergeCompatibleMember(
                member,
                flattened.objectMembers,
                "allOf branches must use compatible additionalProperties schemas.",
                diagnostics);
        default ->
            mergeCompatibleMember(
                member,
                flattened.objectMembers,
                "allOf branches must not declare conflicting '" + member.name() + "' values.",
                diagnostics);
      }
    }
  }

  private static void mergeProperties(
      Member propertiesMember,
      FlattenedObjectMembers flattened,
      List<BindingDiagnostic> diagnostics) {
    if (!(propertiesMember.value() instanceof ObjectValue properties)) {
      mergeCompatibleMember(
          propertiesMember,
          flattened.objectMembers,
          "allOf properties values must be compatible objects.",
          diagnostics);
      return;
    }
    for (Member property : properties.members()) {
      Member existing = flattened.properties.get(property.name());
      if (existing != null && !sameJsonValue(existing.value(), property.value())) {
        diagnostics.add(
            unsupportedAllOf(
                "allOf property '" + property.name() + "' has conflicting schema definitions.",
                property.pointer()));
        continue;
      }
      flattened.properties.putIfAbsent(property.name(), property);
    }
  }

  private static Set<String> declaredPropertyNames(ObjectValue schema) {
    Optional<Member> propertiesMember = member(schema, "properties");
    if (propertiesMember.isEmpty()
        || !(propertiesMember.get().value() instanceof ObjectValue properties)) {
      return Set.of();
    }
    HashSet<String> names = new HashSet<>();
    for (Member property : properties.members()) {
      names.add(property.name());
    }
    return Set.copyOf(names);
  }

  private static boolean isBooleanTrue(SchemaSyntaxValue value) {
    return value instanceof BooleanValue booleanValue && booleanValue.value();
  }

  private static void validateAllOfAdditionalProperties(
      FlattenedObjectMembers flattened, List<BindingDiagnostic> diagnostics) {
    for (AdditionalPropertiesScope scope : flattened.additionalPropertiesScopes) {
      for (String propertyName : flattened.properties.keySet()) {
        if (!scope.declaredProperties().contains(propertyName)) {
          diagnostics.add(
              unsupportedAllOf(
                  "allOf schemas that constrain additionalProperties must declare every merged property in the same schema object.",
                  scope.member().pointer()));
          break;
        }
      }
    }
  }

  private static void mergeRequired(Member requiredMember, FlattenedObjectMembers flattened) {
    if (!(requiredMember.value() instanceof ArrayValue required)) {
      flattened.objectMembers.putIfAbsent(requiredMember.name(), requiredMember);
      return;
    }
    for (SchemaSyntaxValue item : required.items()) {
      if (item instanceof StringValue stringValue) {
        flattened.required.putIfAbsent(stringValue.value(), stringValue);
      }
    }
  }

  private static void mergeCompatibleMember(
      Member member,
      Map<String, Member> members,
      String conflictMessage,
      List<BindingDiagnostic> diagnostics) {
    Member existing = members.get(member.name());
    if (existing != null && !sameJsonValue(existing.value(), member.value())) {
      diagnostics.add(unsupportedAllOf(conflictMessage, member.pointer()));
      return;
    }
    members.putIfAbsent(member.name(), member);
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

  private static boolean sameJsonValue(SchemaSyntaxValue left, SchemaSyntaxValue right) {
    return canonicalJson(left).equals(canonicalJson(right));
  }

  private static String canonicalJson(SchemaSyntaxValue value) {
    StringBuilder builder = new StringBuilder();
    appendCanonicalJson(builder, value);
    return builder.toString();
  }

  private static void appendCanonicalJson(StringBuilder builder, SchemaSyntaxValue value) {
    switch (value) {
      case ObjectValue objectValue -> {
        builder.append('{');
        List<Member> members =
            objectValue.members().stream()
                .sorted(
                    Comparator.comparing(Member::name)
                        .thenComparing(member -> canonicalJson(member.value())))
                .toList();
        for (int index = 0; index < members.size(); index++) {
          if (index > 0) {
            builder.append(',');
          }
          Member member = members.get(index);
          appendJsonString(builder, member.name());
          builder.append(':');
          appendCanonicalJson(builder, member.value());
        }
        builder.append('}');
      }
      case ArrayValue arrayValue -> {
        builder.append('[');
        for (int index = 0; index < arrayValue.items().size(); index++) {
          if (index > 0) {
            builder.append(',');
          }
          appendCanonicalJson(builder, arrayValue.items().get(index));
        }
        builder.append(']');
      }
      case StringValue stringValue -> appendJsonString(builder, stringValue.value());
      case NumberValue numberValue -> builder.append(numberValue.literal());
      case BooleanValue booleanValue -> builder.append(booleanValue.value());
      case NullValue ignored -> builder.append("null");
    }
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
      String parentTypeName,
      Set<String> objectTypeNames,
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
    if (singleTypeName(typeMember.value()).filter("object"::equals).isPresent()) {
      return objectType(propertyName, propertySchema, parentTypeName, objectTypeNames, diagnostics);
    }
    diagnostics.add(
        unsupportedPropertyType(
            "Property '"
                + propertyName
                + "' must use a supported scalar type or homogeneous scalar array.",
            typeMember.pointer()));
    return Optional.empty();
  }

  private static Optional<FieldValueType> objectType(
      String propertyName,
      ObjectValue propertySchema,
      String parentTypeName,
      Set<String> objectTypeNames,
      List<BindingDiagnostic> diagnostics) {
    Optional<ObjectValue> flattenedSchema = flattenedObjectSchema(propertySchema, diagnostics);
    if (flattenedSchema.isEmpty()) {
      return Optional.empty();
    }
    ObjectValue bindingSchema = flattenedSchema.get();
    validateRootType(bindingSchema, diagnostics);
    validateRootLiteralConstraints(bindingSchema, diagnostics);
    ObjectValue properties = propertiesObject(bindingSchema);
    RequiredNames requiredNames = requiredNames(bindingSchema);
    diagnostics.addAll(requiredNames.diagnostics());
    String javaTypeName =
        JavaNameAllocator.uniqueNestedTypeName(parentTypeName, propertyName, objectTypeNames);
    List<FieldBinding> fields =
        objectFields(properties, requiredNames, null, javaTypeName, objectTypeNames, diagnostics);
    Optional<MapBinding> patternProperties =
        patternPropertiesBinding(bindingSchema, javaTypeName, objectTypeNames, fields, diagnostics);
    Optional<MapBinding> additionalProperties =
        additionalPropertiesBinding(
            bindingSchema, javaTypeName, objectTypeNames, fields, patternProperties, diagnostics);
    return Optional.of(
        FieldValueType.object(
            new ObjectBinding(
                javaTypeName,
                propertySchema.pointer(),
                fields,
                patternProperties,
                additionalProperties,
                objectValidationConstraints(bindingSchema),
                annotations(bindingSchema))));
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
    boolean uniqueItems = booleanMember(propertySchema, "uniqueItems").orElse(false);
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
              itemType.get(),
              minItems,
              maxItems,
              uniqueItems,
              facets(itemsSchema),
              literals.get()));
    }
    return Optional.of(
        FieldValueType.array(
            itemType.get(), minItems, maxItems, uniqueItems, facets(itemsSchema), literals.get()));
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
        numberLiteralMember(schema, "exclusiveMaximum"),
        numberLiteralMember(schema, "multipleOf"));
  }

  private static ObjectValidationConstraints objectValidationConstraints(ObjectValue schema) {
    return new ObjectValidationConstraints(
        nonNegativeIntegerMember(schema, "minProperties"),
        nonNegativeIntegerMember(schema, "maxProperties"),
        propertyNamesConstraints(schema),
        dependentRequiredConstraints(schema));
  }

  private static Optional<FacetConstraints> propertyNamesConstraints(ObjectValue schema) {
    Optional<Member> propertyNames = member(schema, "propertyNames");
    if (propertyNames.isEmpty()
        || !(propertyNames.get().value() instanceof ObjectValue objectValue)) {
      return Optional.empty();
    }
    return Optional.of(facets(objectValue));
  }

  private static List<DependentRequired> dependentRequiredConstraints(ObjectValue schema) {
    Optional<Member> dependentRequired = member(schema, "dependentRequired");
    if (dependentRequired.isEmpty()
        || !(dependentRequired.get().value() instanceof ObjectValue objectValue)) {
      return List.of();
    }
    ArrayList<DependentRequired> constraints = new ArrayList<>();
    for (Member dependency : objectValue.members()) {
      if (!(dependency.value() instanceof ArrayValue requiredArray)) {
        continue;
      }
      ArrayList<String> requiredProperties = new ArrayList<>();
      for (SchemaSyntaxValue item : requiredArray.items()) {
        if (item instanceof StringValue stringValue) {
          requiredProperties.add(stringValue.value());
        }
      }
      constraints.add(new DependentRequired(dependency.name(), requiredProperties));
    }
    return List.copyOf(constraints);
  }

  private static Optional<String> stringMember(ObjectValue objectValue, String name) {
    Optional<Member> member = member(objectValue, name);
    if (member.isEmpty() || !(member.get().value() instanceof StringValue stringValue)) {
      return Optional.empty();
    }
    return Optional.of(stringValue.value());
  }

  private static Optional<Boolean> booleanMember(ObjectValue objectValue, String name) {
    Optional<Member> member = member(objectValue, name);
    if (member.isEmpty() || !(member.get().value() instanceof BooleanValue booleanValue)) {
      return Optional.empty();
    }
    return Optional.of(booleanValue.value());
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
    for (String name : List.of("enum", "const")) {
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

  private static final class FlattenedObjectMembers {
    private final JsonPointer pointer;
    private final LinkedHashMap<String, Member> objectMembers = new LinkedHashMap<>();
    private final LinkedHashMap<String, Member> properties = new LinkedHashMap<>();
    private final LinkedHashMap<String, StringValue> required = new LinkedHashMap<>();
    private final List<AdditionalPropertiesScope> additionalPropertiesScopes = new ArrayList<>();

    private FlattenedObjectMembers(JsonPointer pointer) {
      this.pointer = Objects.requireNonNull(pointer, "pointer");
    }

    private ObjectValue toObjectValue() {
      ArrayList<Member> members = new ArrayList<>();
      members.addAll(objectMembers.values());
      if (!properties.isEmpty()) {
        members.add(
            new Member(
                "properties",
                new ObjectValue(pointer.property("properties"), List.copyOf(properties.values()))));
      }
      if (!required.isEmpty()) {
        members.add(
            new Member(
                "required",
                new ArrayValue(pointer.property("required"), List.copyOf(required.values()))));
      }
      return new ObjectValue(pointer, members);
    }
  }

  private record AdditionalPropertiesScope(Member member, Set<String> declaredProperties) {}

  private static BindingDiagnostic rootTypeDiagnostic(String message, JsonPointer pointer) {
    return new BindingDiagnostic(BindingDiagnostic.ROOT_TYPE_CODE, message, pointer);
  }

  private static BindingDiagnostic additionalPropertiesDiagnostic(
      String message, JsonPointer pointer) {
    return new BindingDiagnostic(BindingDiagnostic.ADDITIONAL_PROPERTIES_CODE, message, pointer);
  }

  private static BindingDiagnostic patternPropertiesDiagnostic(
      String message, JsonPointer pointer) {
    return new BindingDiagnostic(BindingDiagnostic.PATTERN_PROPERTIES_CODE, message, pointer);
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

  private static BindingDiagnostic unsupportedAllOf(String message, JsonPointer pointer) {
    return new BindingDiagnostic(BindingDiagnostic.UNSUPPORTED_ALL_OF_CODE, message, pointer);
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
