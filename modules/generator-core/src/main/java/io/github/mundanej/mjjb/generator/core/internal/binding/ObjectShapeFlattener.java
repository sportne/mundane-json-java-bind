package io.github.mundanej.mjjb.generator.core.internal.binding;

import io.github.mundanej.mjjb.schema.model.JsonPointer;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.ArrayValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.BooleanValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.Member;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.ObjectValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.StringValue;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

final class ObjectShapeFlattener {
  private ObjectShapeFlattener() {}

  static Optional<ObjectValue> flatten(ObjectValue schema, List<BindingDiagnostic> diagnostics) {
    Optional<Member> allOfMember = BindingModelBuilder.member(schema, "allOf");
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
      Optional<ObjectValue> flattenedBranch = flatten(branch, diagnostics);
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
    Optional<Member> additionalProperties =
        BindingModelBuilder.member(schema, "additionalProperties");
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
      if (existing != null
          && !SchemaLiteralReader.sameJsonValue(existing.value(), property.value())) {
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
    Optional<Member> propertiesMember = BindingModelBuilder.member(schema, "properties");
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
    if (existing != null && !SchemaLiteralReader.sameJsonValue(existing.value(), member.value())) {
      diagnostics.add(unsupportedAllOf(conflictMessage, member.pointer()));
      return;
    }
    members.putIfAbsent(member.name(), member);
  }

  private static BindingDiagnostic unsupportedAllOf(String message, JsonPointer pointer) {
    return new BindingDiagnostic(BindingDiagnostic.UNSUPPORTED_ALL_OF_CODE, message, pointer);
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
}
