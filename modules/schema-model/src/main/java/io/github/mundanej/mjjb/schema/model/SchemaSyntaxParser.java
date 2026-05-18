package io.github.mundanej.mjjb.schema.model;

import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.ArrayValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.BooleanValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.Member;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.NullValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.NumberValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.ObjectValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.StringValue;
import java.util.ArrayList;
import java.util.Objects;

/** Dependency-free JSON syntax parser for JSON Schema documents. */
public final class SchemaSyntaxParser {
  public static final String INVALID_JSON_CODE = "MJJBG-SCHEMA-INVALID-JSON";

  private final String source;
  private final ArrayList<SchemaSyntaxDiagnostic> diagnostics = new ArrayList<>();
  private int index;

  private SchemaSyntaxParser(String source) {
    this.source = Objects.requireNonNull(source, "source");
  }

  public static SchemaSyntaxParseResult parse(String source) {
    SchemaSyntaxParser parser = new SchemaSyntaxParser(source);
    return parser.parse();
  }

  private SchemaSyntaxParseResult parse() {
    skipWhitespace();
    SchemaSyntaxValue root = parseValue(JsonPointer.ROOT);
    skipWhitespace();
    if (diagnostics.isEmpty() && index < source.length()) {
      addError("Unexpected content after JSON Schema document.", JsonPointer.ROOT);
    }
    if (!diagnostics.isEmpty()) {
      return SchemaSyntaxParseResult.failure(diagnostics);
    }
    return SchemaSyntaxParseResult.success(root);
  }

  private SchemaSyntaxValue parseValue(JsonPointer pointer) {
    skipWhitespace();
    if (diagnosticsPresent()) {
      return null;
    }
    if (index >= source.length()) {
      addError("Unexpected end of JSON Schema document.", pointer);
      return null;
    }
    char current = source.charAt(index);
    return switch (current) {
      case '{' -> parseObject(pointer);
      case '[' -> parseArray(pointer);
      case '"' -> new StringValue(pointer, parseString(pointer));
      case 't' -> parseTrue(pointer);
      case 'f' -> parseFalse(pointer);
      case 'n' -> parseNull(pointer);
      default -> {
        if (current == '-' || isDigit(current)) {
          yield parseNumber(pointer);
        }
        addError("Unexpected JSON token in schema.", pointer);
        yield null;
      }
    };
  }

  private ObjectValue parseObject(JsonPointer pointer) {
    index++;
    ArrayList<Member> members = new ArrayList<>();
    skipWhitespace();
    if (consumeIf('}')) {
      return new ObjectValue(pointer, members);
    }
    boolean first = true;
    while (!diagnosticsPresent()) {
      if (!first && !consumeIf(',')) {
        addError("Expected comma between object properties.", pointer);
        return null;
      }
      skipWhitespace();
      if (index >= source.length() || source.charAt(index) != '"') {
        addError("Expected object property name.", pointer);
        return null;
      }
      String name = parseString(pointer);
      if (diagnosticsPresent()) {
        return null;
      }
      JsonPointer propertyPointer = pointer.property(name);
      skipWhitespace();
      if (!consumeIf(':')) {
        addError("Expected colon after object property name.", propertyPointer);
        return null;
      }
      SchemaSyntaxValue value = parseValue(propertyPointer);
      if (diagnosticsPresent()) {
        return null;
      }
      members.add(new Member(name, value));
      skipWhitespace();
      if (consumeIf('}')) {
        return new ObjectValue(pointer, members);
      }
      first = false;
    }
    return null;
  }

  private ArrayValue parseArray(JsonPointer pointer) {
    index++;
    ArrayList<SchemaSyntaxValue> items = new ArrayList<>();
    skipWhitespace();
    if (consumeIf(']')) {
      return new ArrayValue(pointer, items);
    }
    int itemIndex = 0;
    boolean first = true;
    while (!diagnosticsPresent()) {
      if (!first && !consumeIf(',')) {
        addError("Expected comma between array items.", pointer);
        return null;
      }
      SchemaSyntaxValue value = parseValue(pointer.index(itemIndex));
      if (diagnosticsPresent()) {
        return null;
      }
      items.add(value);
      itemIndex++;
      skipWhitespace();
      if (consumeIf(']')) {
        return new ArrayValue(pointer, items);
      }
      first = false;
    }
    return null;
  }

  private String parseString(JsonPointer pointer) {
    index++;
    StringBuilder builder = new StringBuilder();
    while (index < source.length()) {
      char current = source.charAt(index++);
      if (current == '"') {
        return builder.toString();
      }
      if (current == '\\') {
        builder.append(parseEscapedCharacter(pointer));
        if (diagnosticsPresent()) {
          return "";
        }
      } else if (current <= 0x1f) {
        addError("Unescaped control character in JSON string.", pointer);
        return "";
      } else {
        builder.append(current);
      }
    }
    addError("Unterminated JSON string.", pointer);
    return "";
  }

  private char parseEscapedCharacter(JsonPointer pointer) {
    if (index >= source.length()) {
      addError("Unterminated JSON escape.", pointer);
      return '\0';
    }
    char escaped = source.charAt(index++);
    if (escaped != 'u') {
      return switch (escaped) {
        case '"', '\\', '/' -> escaped;
        case 'b' -> '\b';
        case 'f' -> '\f';
        case 'n' -> '\n';
        case 'r' -> '\r';
        case 't' -> '\t';
        default -> {
          addError("Invalid JSON escape.", pointer);
          yield '\0';
        }
      };
    }
    if (index + 4 > source.length()) {
      addError("Incomplete unicode escape.", pointer);
      return '\0';
    }
    int value = 0;
    for (int i = 0; i < 4; i++) {
      int hex = Character.digit(source.charAt(index++), 16);
      if (hex < 0) {
        addError("Invalid unicode escape digit.", pointer);
        return '\0';
      }
      value = (value << 4) + hex;
    }
    return (char) value;
  }

  private NumberValue parseNumber(JsonPointer pointer) {
    int start = index;
    consumeIf('-');
    if (index >= source.length() || !isDigit(source.charAt(index))) {
      addError("Expected digit in JSON number.", pointer);
      return null;
    }
    if (consumeIf('0')) {
      if (index < source.length() && isDigit(source.charAt(index))) {
        addError("Leading zeroes are not valid JSON numbers.", pointer);
        return null;
      }
    } else {
      consumeDigits();
    }
    if (consumeIf('.')) {
      if (index >= source.length() || !isDigit(source.charAt(index))) {
        addError("Expected digit after decimal point in JSON number.", pointer);
        return null;
      }
      consumeDigits();
    }
    if (index < source.length() && (source.charAt(index) == 'e' || source.charAt(index) == 'E')) {
      index++;
      if (index < source.length() && (source.charAt(index) == '+' || source.charAt(index) == '-')) {
        index++;
      }
      if (index >= source.length() || !isDigit(source.charAt(index))) {
        addError("Expected exponent digit in JSON number.", pointer);
        return null;
      }
      consumeDigits();
    }
    return new NumberValue(pointer, source.substring(start, index));
  }

  private BooleanValue parseTrue(JsonPointer pointer) {
    if (!source.startsWith("true", index)) {
      addError("Invalid JSON literal.", pointer);
      return null;
    }
    index += "true".length();
    return new BooleanValue(pointer, true);
  }

  private BooleanValue parseFalse(JsonPointer pointer) {
    if (!source.startsWith("false", index)) {
      addError("Invalid JSON literal.", pointer);
      return null;
    }
    index += "false".length();
    return new BooleanValue(pointer, false);
  }

  private NullValue parseNull(JsonPointer pointer) {
    if (!source.startsWith("null", index)) {
      addError("Invalid JSON literal.", pointer);
      return null;
    }
    index += "null".length();
    return new NullValue(pointer);
  }

  private void consumeDigits() {
    while (index < source.length() && isDigit(source.charAt(index))) {
      index++;
    }
  }

  private static boolean isDigit(char value) {
    return value >= '0' && value <= '9';
  }

  private boolean consumeIf(char expected) {
    if (index < source.length() && source.charAt(index) == expected) {
      index++;
      return true;
    }
    return false;
  }

  private void skipWhitespace() {
    while (index < source.length()) {
      char current = source.charAt(index);
      if (current != ' ' && current != '\n' && current != '\r' && current != '\t') {
        return;
      }
      index++;
    }
  }

  private boolean diagnosticsPresent() {
    return !diagnostics.isEmpty();
  }

  private void addError(String message, JsonPointer pointer) {
    if (diagnostics.isEmpty()) {
      diagnostics.add(new SchemaSyntaxDiagnostic(INVALID_JSON_CODE, message, pointer));
    }
  }
}
