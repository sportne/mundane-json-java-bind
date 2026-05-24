package io.github.mundanej.mjjb.schema.model;

import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.ArrayValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.Member;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.ObjectValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.StringValue;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Resolves profile-supported same-document JSON Schema references before binding. */
public final class SchemaReferenceResolver {
  public static final String INVALID_REF_CODE = "MJJBG-SCHEMA-INVALID-REF";
  public static final String REMOTE_REF_CODE = "MJJBG-SCHEMA-REMOTE-REF";
  public static final String MISSING_REF_CODE = "MJJBG-SCHEMA-MISSING-REF";
  public static final String CYCLIC_REF_CODE = "MJJBG-SCHEMA-CYCLIC-REF";
  public static final String UNSUPPORTED_REF_SIBLING_CODE = "MJJBG-SCHEMA-UNSUPPORTED-REF-SIBLING";

  private static final Set<String> ANNOTATION_SIBLINGS =
      Set.of(
          "$ref",
          "$defs",
          "$schema",
          "title",
          "description",
          "$comment",
          "examples",
          "deprecated",
          "readOnly",
          "writeOnly");

  private final Map<String, SchemaSyntaxValue> valuesByPointer = new HashMap<>();
  private final ArrayList<SchemaReferenceDiagnostic> diagnostics = new ArrayList<>();

  private SchemaReferenceResolver(SchemaSyntaxValue root) {
    index(root);
  }

  public static SchemaReferenceResolutionResult resolve(SchemaSyntaxValue root) {
    Objects.requireNonNull(root, "root");
    SchemaReferenceResolver resolver = new SchemaReferenceResolver(root);
    SchemaSyntaxValue resolved = resolver.resolveSchema(root, new ArrayDeque<>());
    if (!resolver.diagnostics.isEmpty()) {
      resolver.diagnostics.sort(
          Comparator.comparing(
                  (SchemaReferenceDiagnostic diagnostic) -> diagnostic.pointer().value())
              .thenComparing(SchemaReferenceDiagnostic::code)
              .thenComparing(SchemaReferenceDiagnostic::message));
      return SchemaReferenceResolutionResult.failure(resolver.diagnostics);
    }
    return SchemaReferenceResolutionResult.success(resolved);
  }

  private void index(SchemaSyntaxValue value) {
    valuesByPointer.put(value.pointer().value(), value);
    switch (value) {
      case ObjectValue objectValue ->
          objectValue.members().forEach(member -> index(member.value()));
      case ArrayValue arrayValue -> arrayValue.items().forEach(this::index);
      default -> {
        // Scalar schema syntax values do not have children.
      }
    }
  }

  private SchemaSyntaxValue resolveSchema(SchemaSyntaxValue value, ArrayDeque<JsonPointer> stack) {
    return switch (value) {
      case ObjectValue objectValue -> resolveSchemaObject(objectValue, stack);
      default -> value;
    };
  }

  private SchemaSyntaxValue resolveSchemaObject(
      ObjectValue objectValue, ArrayDeque<JsonPointer> stack) {
    Optional<Member> ref = member(objectValue, "$ref");
    if (ref.isPresent()) {
      member(objectValue, "$defs").ifPresent(defs -> resolveDefinitions(defs.value(), stack));
      return resolveReferenceObject(objectValue, ref.get(), stack);
    }
    ArrayList<Member> members = new ArrayList<>();
    for (Member member : objectValue.members()) {
      members.add(new Member(member.name(), resolveSchemaMember(member, stack)));
    }
    return new ObjectValue(objectValue.pointer(), members);
  }

  private SchemaSyntaxValue resolveSchemaMember(Member member, ArrayDeque<JsonPointer> stack) {
    return switch (member.name()) {
      case "$defs" -> resolveDefinitions(member.value(), stack);
      case "properties", "patternProperties", "dependentSchemas" ->
          resolveSchemaMap(member.value(), stack);
      case "items",
          "additionalProperties",
          "contains",
          "propertyNames",
          "unevaluatedProperties",
          "unevaluatedItems",
          "not",
          "if",
          "then",
          "else",
          "contentSchema" ->
          resolveSchema(member.value(), stack);
      case "oneOf", "anyOf", "allOf", "prefixItems" -> resolveSchemaArray(member.value(), stack);
      default -> member.value();
    };
  }

  private SchemaSyntaxValue resolveDefinitions(
      SchemaSyntaxValue value, ArrayDeque<JsonPointer> stack) {
    return resolveSchemaMap(value, stack);
  }

  private SchemaSyntaxValue resolveSchemaMap(
      SchemaSyntaxValue value, ArrayDeque<JsonPointer> stack) {
    if (!(value instanceof ObjectValue objectValue)) {
      return value;
    }
    ArrayList<Member> members = new ArrayList<>();
    for (Member member : objectValue.members()) {
      members.add(new Member(member.name(), resolveSchema(member.value(), stack)));
    }
    return new ObjectValue(objectValue.pointer(), members);
  }

  private SchemaSyntaxValue resolveSchemaArray(
      SchemaSyntaxValue value, ArrayDeque<JsonPointer> stack) {
    if (!(value instanceof ArrayValue arrayValue)) {
      return value;
    }
    ArrayList<SchemaSyntaxValue> items = new ArrayList<>();
    for (SchemaSyntaxValue item : arrayValue.items()) {
      items.add(resolveSchema(item, stack));
    }
    return new ArrayValue(arrayValue.pointer(), items);
  }

  private SchemaSyntaxValue resolveReferenceObject(
      ObjectValue objectValue, Member ref, ArrayDeque<JsonPointer> stack) {
    for (Member member : objectValue.members()) {
      if (!ANNOTATION_SIBLINGS.contains(member.name())) {
        diagnostics.add(
            new SchemaReferenceDiagnostic(
                UNSUPPORTED_REF_SIBLING_CODE,
                "JSP-DATA-2020-12 supports '$ref' only without assertion or applicator siblings.",
                member.pointer()));
      }
    }
    if (!(ref.value() instanceof StringValue refValue)) {
      diagnostics.add(
          new SchemaReferenceDiagnostic(
              INVALID_REF_CODE, "The '$ref' keyword value must be a string.", ref.pointer()));
      return objectValue;
    }
    Optional<JsonPointer> targetPointer = localPointer(refValue.value(), ref.pointer());
    if (targetPointer.isEmpty()) {
      return objectValue;
    }
    SchemaSyntaxValue target = valuesByPointer.get(targetPointer.get().value());
    if (target == null) {
      diagnostics.add(
          new SchemaReferenceDiagnostic(
              MISSING_REF_CODE,
              "The '$ref' value does not resolve to a value in this schema document.",
              ref.pointer()));
      return objectValue;
    }
    if (stack.contains(targetPointer.get())) {
      diagnostics.add(
          new SchemaReferenceDiagnostic(
              CYCLIC_REF_CODE,
              "The '$ref' value creates a same-document reference cycle.",
              ref.pointer()));
      return objectValue;
    }
    stack.addLast(targetPointer.get());
    SchemaSyntaxValue resolved = resolveSchema(target, stack);
    stack.removeLast();
    return resolved;
  }

  private Optional<JsonPointer> localPointer(String value, JsonPointer pointer) {
    if (!value.startsWith("#")) {
      diagnostics.add(
          new SchemaReferenceDiagnostic(
              REMOTE_REF_CODE,
              "JSP-DATA-2020-12 supports '$ref' only as a same-document JSON Pointer fragment.",
              pointer));
      return Optional.empty();
    }
    Optional<String> decoded = percentDecode(value.substring(1), pointer);
    if (decoded.isEmpty()) {
      return Optional.empty();
    }
    String fragment = decoded.get();
    if (fragment.isEmpty()) {
      return Optional.of(JsonPointer.ROOT);
    }
    if (!fragment.startsWith("/")) {
      diagnostics.add(
          new SchemaReferenceDiagnostic(
              INVALID_REF_CODE, "The '$ref' fragment must be empty or begin with '/'.", pointer));
      return Optional.empty();
    }
    if (!validJsonPointerFragment(fragment)) {
      diagnostics.add(
          new SchemaReferenceDiagnostic(
              INVALID_REF_CODE, "The '$ref' fragment must be a valid JSON Pointer.", pointer));
      return Optional.empty();
    }
    return Optional.of(new JsonPointer(fragment));
  }

  private Optional<String> percentDecode(String value, JsonPointer pointer) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    for (int index = 0; index < value.length(); index++) {
      char current = value.charAt(index);
      if (current != '%') {
        bytes.write((byte) current);
        continue;
      }
      if (index + 2 >= value.length()) {
        diagnostics.add(
            new SchemaReferenceDiagnostic(
                INVALID_REF_CODE, "The '$ref' fragment has an invalid percent escape.", pointer));
        return Optional.empty();
      }
      int high = Character.digit(value.charAt(index + 1), 16);
      int low = Character.digit(value.charAt(index + 2), 16);
      if (high < 0 || low < 0) {
        diagnostics.add(
            new SchemaReferenceDiagnostic(
                INVALID_REF_CODE, "The '$ref' fragment has an invalid percent escape.", pointer));
        return Optional.empty();
      }
      bytes.write((high << 4) + low);
      index += 2;
    }
    return Optional.of(bytes.toString(StandardCharsets.UTF_8));
  }

  private static boolean validJsonPointerFragment(String fragment) {
    int index = 0;
    while (index < fragment.length()) {
      if (fragment.charAt(index) != '~') {
        index++;
        continue;
      }
      if (index + 1 >= fragment.length()) {
        return false;
      }
      char escaped = fragment.charAt(index + 1);
      if (escaped != '0' && escaped != '1') {
        return false;
      }
      index += 2;
    }
    return true;
  }

  private static Optional<Member> member(ObjectValue object, String name) {
    for (Member member : object.members()) {
      if (name.equals(member.name())) {
        return Optional.of(member);
      }
    }
    return Optional.empty();
  }
}
