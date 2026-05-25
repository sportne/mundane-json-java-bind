package io.github.mundanej.mjjb.generator.core.internal.binding;

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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class SchemaLiteralReader {
  private SchemaLiteralReader() {}

  static SchemaAnnotationsBinding annotations(ObjectValue schema) {
    return new SchemaAnnotationsBinding(
        stringAnnotation(schema, "title"),
        stringAnnotation(schema, "description"),
        stringAnnotation(schema, "$comment"),
        examplesJson(schema),
        booleanAnnotation(schema, "deprecated"),
        booleanAnnotation(schema, "readOnly"),
        booleanAnnotation(schema, "writeOnly"),
        BindingModelBuilder.member(schema, "default").map(member -> compactJson(member.value())));
  }

  static Optional<LiteralConstraints> literalConstraints(
      ObjectValue schema,
      JavaScalarType scalarType,
      String propertyName,
      List<BindingDiagnostic> diagnostics) {
    Optional<Member> enumMember = BindingModelBuilder.member(schema, "enum");
    Optional<Member> constMember = BindingModelBuilder.member(schema, "const");
    Optional<Member> defaultMember = BindingModelBuilder.member(schema, "default");
    ArrayList<LiteralValue> enumValues = new ArrayList<>();
    Set<String> enumKeys = new HashSet<>();
    if (enumMember.isPresent()) {
      if (!(enumMember.get().value() instanceof ArrayValue arrayValue)) {
        diagnostics.add(
            BindingModelBuilder.invalidLiteralConstraint(
                "Property '" + propertyName + "' enum constraint must be an array.",
                enumMember.get().pointer()));
        return Optional.empty();
      }
      if (arrayValue.items().isEmpty()) {
        diagnostics.add(
            BindingModelBuilder.invalidLiteralConstraint(
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
              BindingModelBuilder.invalidLiteralConstraint(
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

  static boolean hasLiteralConstraint(ObjectValue schema) {
    return firstLiteralConstraint(schema).isPresent();
  }

  static Optional<Member> firstLiteralConstraint(ObjectValue schema) {
    for (String name : List.of("enum", "const")) {
      Optional<Member> member = BindingModelBuilder.member(schema, name);
      if (member.isPresent()) {
        return member;
      }
    }
    return Optional.empty();
  }

  static boolean sameJsonValue(SchemaSyntaxValue left, SchemaSyntaxValue right) {
    return canonicalJson(left).equals(canonicalJson(right));
  }

  static String compactJson(SchemaSyntaxValue value) {
    StringBuilder builder = new StringBuilder();
    appendCompactJson(builder, value);
    return builder.toString();
  }

  private static Optional<String> stringAnnotation(ObjectValue schema, String name) {
    return BindingModelBuilder.member(schema, name)
        .filter(member -> member.value() instanceof StringValue)
        .map(member -> ((StringValue) member.value()).value());
  }

  private static Optional<Boolean> booleanAnnotation(ObjectValue schema, String name) {
    return BindingModelBuilder.member(schema, name)
        .filter(member -> member.value() instanceof BooleanValue)
        .map(member -> ((BooleanValue) member.value()).value());
  }

  private static List<String> examplesJson(ObjectValue schema) {
    Optional<Member> examples = BindingModelBuilder.member(schema, "examples");
    if (examples.isEmpty() || !(examples.get().value() instanceof ArrayValue arrayValue)) {
      return List.of();
    }
    return arrayValue.items().stream().map(SchemaLiteralReader::compactJson).toList();
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
        appendObjectMembers(builder, members, true);
      }
      case ArrayValue arrayValue -> appendArray(builder, arrayValue.items(), true);
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
        appendObjectMembers(builder, objectValue.members(), false);
      }
      case ArrayValue arrayValue -> appendArray(builder, arrayValue.items(), false);
      case StringValue stringValue -> appendJsonString(builder, stringValue.value());
      case NumberValue numberValue -> builder.append(numberValue.literal());
      case BooleanValue booleanValue -> builder.append(booleanValue.value());
      case NullValue ignored -> builder.append("null");
    }
  }

  private static void appendObjectMembers(
      StringBuilder builder, List<Member> members, boolean canonical) {
    for (int index = 0; index < members.size(); index++) {
      if (index > 0) {
        builder.append(',');
      }
      Member member = members.get(index);
      appendJsonString(builder, member.name());
      builder.append(':');
      if (canonical) {
        appendCanonicalJson(builder, member.value());
      } else {
        appendCompactJson(builder, member.value());
      }
    }
    builder.append('}');
  }

  private static void appendArray(
      StringBuilder builder, List<SchemaSyntaxValue> items, boolean canonical) {
    builder.append('[');
    for (int index = 0; index < items.size(); index++) {
      if (index > 0) {
        builder.append(',');
      }
      if (canonical) {
        appendCanonicalJson(builder, items.get(index));
      } else {
        appendCompactJson(builder, items.get(index));
      }
    }
    builder.append(']');
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
        BindingModelBuilder.unsupportedLiteralConstraint(
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
}
