package io.github.mundanej.mjjb.generator.core.internal.binding;

import io.github.mundanej.mjjb.schema.model.JsonPointer;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.ArrayValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.BooleanValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.Member;
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

    validateRootType(rootObject, diagnostics);
    validateAdditionalProperties(rootObject, diagnostics);
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
                property.pointer()));
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
    ObjectBinding rootBinding = new ObjectBinding(rootTypeName, root.pointer(), fields);
    return BindingBuildResult.success(new BindingModel(packageName, rootTypeName, rootBinding));
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

  private static Optional<JavaScalarType> scalarType(SchemaSyntaxValue value) {
    if (!(value instanceof StringValue stringValue)) {
      return Optional.empty();
    }
    for (JavaScalarType candidate : JavaScalarType.values()) {
      if (candidate.schemaType().equals(stringValue.value())) {
        return Optional.of(candidate);
      }
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
      return Optional.of(FieldValueType.scalar(scalarType.get()));
    }
    if (typeMember.value() instanceof StringValue stringValue
        && "array".equals(stringValue.value())) {
      return arrayType(propertyName, propertySchema, diagnostics);
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
      String propertyName, ObjectValue propertySchema, List<BindingDiagnostic> diagnostics) {
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
    return Optional.of(FieldValueType.array(itemType.get(), minItems, maxItems));
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
}
