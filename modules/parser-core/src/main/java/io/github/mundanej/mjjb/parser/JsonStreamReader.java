package io.github.mundanej.mjjb.parser;

import io.github.mundanej.mjjb.runtime.JsonDiagnostic;
import io.github.mundanej.mjjb.runtime.JsonLocation;
import io.github.mundanej.mjjb.runtime.JsonPath;
import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.JsonReader;
import io.github.mundanej.mjjb.runtime.JsonToken;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Objects;

/** Dependency-free streaming JSON reader for generated bindings. */
public final class JsonStreamReader implements JsonReader {
  private static final int READ_BUFFER_SIZE = 4096;

  private final String sourceName;
  private final StringBuilder input = new StringBuilder();
  private final Reader reader;
  private final char[] readBuffer;
  private final ArrayDeque<Context> stack = new ArrayDeque<>();
  private boolean endOfInput;
  private int index;
  private int line = 1;
  private int column = 1;

  public JsonStreamReader(String input) {
    this("<string>", input);
  }

  public JsonStreamReader(String sourceName, String input) {
    this.sourceName = Objects.requireNonNull(sourceName, "sourceName");
    this.reader = null;
    this.readBuffer = null;
    this.input.append(Objects.requireNonNull(input, "input"));
    this.endOfInput = true;
  }

  private JsonStreamReader(String sourceName, Reader reader) {
    this.sourceName = Objects.requireNonNull(sourceName, "sourceName");
    this.reader = Objects.requireNonNull(reader, "reader");
    this.readBuffer = new char[READ_BUFFER_SIZE];
  }

  public static JsonStreamReader fromStringReader(String input) throws IOException {
    return fromReader("<string>", new StringReader(input));
  }

  public static JsonStreamReader fromReader(String sourceName, Reader reader) {
    return new JsonStreamReader(sourceName, reader);
  }

  @Override
  public JsonToken peek() throws JsonReadException {
    skipWhitespace();
    if (!hasChar(index)) {
      return JsonToken.END_DOCUMENT;
    }
    if (!stack.isEmpty() && stack.peek().kind == ContextKind.OBJECT && stack.peek().expectingName) {
      if (charAt(index) == '}') {
        return JsonToken.END_OBJECT;
      }
      return JsonToken.NAME;
    }
    return switch (charAt(index)) {
      case '{' -> JsonToken.BEGIN_OBJECT;
      case '}' -> JsonToken.END_OBJECT;
      case '[' -> JsonToken.BEGIN_ARRAY;
      case ']' -> JsonToken.END_ARRAY;
      case '"' -> JsonToken.STRING;
      case 't', 'f' -> JsonToken.BOOLEAN;
      case 'n' -> JsonToken.NULL;
      default -> {
        if (isNumberStart(charAt(index))) {
          yield JsonToken.NUMBER;
        }
        throw error("MJJBP-001", "Unexpected JSON token.");
      }
    };
  }

  @Override
  public void beginObject() throws JsonReadException {
    beforeValue();
    expect('{');
    stack.push(new Context(ContextKind.OBJECT));
  }

  @Override
  public void endObject() throws JsonReadException {
    skipWhitespace();
    Context context = requireContext(ContextKind.OBJECT);
    if (!context.expectingName) {
      throw error("MJJBP-002", "Expected object property value before object end.");
    }
    expect('}');
    stack.pop();
    afterValue();
  }

  @Override
  public void beginArray() throws JsonReadException {
    beforeValue();
    expect('[');
    stack.push(new Context(ContextKind.ARRAY));
  }

  @Override
  public void endArray() throws JsonReadException {
    skipWhitespace();
    requireContext(ContextKind.ARRAY);
    expect(']');
    stack.pop();
    afterValue();
  }

  @Override
  public boolean hasNext() throws JsonReadException {
    Context context = requireAnyContext();
    if (context.prepared) {
      return true;
    }
    skipWhitespace();
    char end = context.kind == ContextKind.OBJECT ? '}' : ']';
    if (peekChar(end)) {
      return false;
    }
    if (!context.first) {
      expect(',');
      skipWhitespace();
      if (peekChar(end)) {
        throw error("MJJBP-003", "Trailing commas are not valid JSON.");
      }
    }
    context.prepared = true;
    return true;
  }

  @Override
  public String nextName() throws JsonReadException {
    Context context = requireContext(ContextKind.OBJECT);
    if (!context.expectingName) {
      throw error("MJJBP-004", "Expected property value, not another property name.");
    }
    if (!hasNext()) {
      throw error("MJJBP-005", "Expected object property name.");
    }
    String name = parseString();
    skipWhitespace();
    expect(':');
    context.prepared = false;
    context.expectingName = false;
    return name;
  }

  @Override
  public String nextString() throws JsonReadException {
    beforeValue();
    String value = parseString();
    afterValue();
    return value;
  }

  @Override
  public String nextNumberLiteral() throws JsonReadException {
    beforeValue();
    int start = index;
    if (peekChar('-')) {
      advance();
    }
    parseDigits(true);
    if (peekChar('.')) {
      advance();
      parseDigits(false);
    }
    if (peekChar('e') || peekChar('E')) {
      advance();
      if (peekChar('+') || peekChar('-')) {
        advance();
      }
      parseDigits(false);
    }
    String number = input.substring(start, index);
    afterValue();
    return number;
  }

  @Override
  public boolean nextBoolean() throws JsonReadException {
    beforeValue();
    boolean value;
    if (startsWith("true")) {
      consumeLiteral("true");
      value = true;
    } else if (startsWith("false")) {
      consumeLiteral("false");
      value = false;
    } else {
      throw error("MJJBP-006", "Expected JSON boolean.");
    }
    afterValue();
    return value;
  }

  @Override
  public void nextNull() throws JsonReadException {
    beforeValue();
    consumeLiteral("null");
    afterValue();
  }

  @Override
  public void skipValue() throws JsonReadException {
    switch (peek()) {
      case BEGIN_OBJECT -> {
        beginObject();
        while (hasNext()) {
          nextName();
          skipValue();
        }
        endObject();
      }
      case BEGIN_ARRAY -> {
        beginArray();
        while (hasNext()) {
          skipValue();
        }
        endArray();
      }
      case STRING -> nextString();
      case NUMBER -> nextNumberLiteral();
      case BOOLEAN -> nextBoolean();
      case NULL -> nextNull();
      default -> throw error("MJJBP-007", "Expected JSON value.");
    }
  }

  @Override
  public JsonLocation location() {
    return new JsonLocation(sourceName, index, line, column);
  }

  private void beforeValue() throws JsonReadException {
    skipWhitespace();
    if (stack.isEmpty()) {
      return;
    }
    Context context = stack.peek();
    if (context.kind == ContextKind.OBJECT) {
      if (context.expectingName) {
        throw error("MJJBP-008", "Expected object property name.");
      }
      return;
    }
    if (context.prepared) {
      context.prepared = false;
    } else if (!hasNext()) {
      throw error("MJJBP-009", "Expected array value.");
    } else {
      context.prepared = false;
    }
  }

  private void afterValue() {
    if (stack.isEmpty()) {
      return;
    }
    Context context = stack.peek();
    context.first = false;
    if (context.kind == ContextKind.OBJECT) {
      context.expectingName = true;
    }
  }

  private String parseString() throws JsonReadException {
    skipWhitespace();
    expect('"');
    StringBuilder builder = new StringBuilder();
    while (hasChar(index)) {
      char current = advance();
      if (current == '"') {
        return builder.toString();
      }
      if (current == '\\') {
        builder.append(parseEscape());
      } else {
        if (current < 0x20) {
          throw error("MJJBP-010", "Unescaped control character in JSON string.");
        }
        builder.append(current);
      }
    }
    throw error("MJJBP-011", "Unterminated JSON string.");
  }

  private char parseEscape() throws JsonReadException {
    if (!hasChar(index)) {
      throw error("MJJBP-012", "Unterminated JSON escape.");
    }
    char escaped = advance();
    return switch (escaped) {
      case '"', '\\', '/' -> escaped;
      case 'b' -> '\b';
      case 'f' -> '\f';
      case 'n' -> '\n';
      case 'r' -> '\r';
      case 't' -> '\t';
      case 'u' -> parseUnicodeEscape();
      default -> throw error("MJJBP-013", "Invalid JSON escape.");
    };
  }

  private char parseUnicodeEscape() throws JsonReadException {
    if (!hasChar(index + 3)) {
      throw error("MJJBP-014", "Incomplete unicode escape.");
    }
    int value = 0;
    for (int i = 0; i < 4; i++) {
      char digit = advance();
      int hex = Character.digit(digit, 16);
      if (hex < 0) {
        throw error("MJJBP-015", "Invalid unicode escape digit.");
      }
      value = (value << 4) + hex;
    }
    return (char) value;
  }

  private void parseDigits(boolean rejectLeadingZeroes) throws JsonReadException {
    if (!hasChar(index) || !Character.isDigit(charAt(index))) {
      throw error("MJJBP-016", "Expected digit in JSON number.");
    }
    if (rejectLeadingZeroes && charAt(index) == '0') {
      advance();
      if (hasChar(index) && Character.isDigit(charAt(index))) {
        throw error("MJJBP-017", "Leading zeroes are not valid JSON numbers.");
      }
      return;
    }
    while (hasChar(index) && Character.isDigit(charAt(index))) {
      advance();
    }
  }

  private void consumeLiteral(String literal) throws JsonReadException {
    if (!startsWith(literal)) {
      throw error("MJJBP-018", "Expected JSON literal " + literal + ".");
    }
    for (int i = 0; i < literal.length(); i++) {
      advance();
    }
  }

  private boolean startsWith(String literal) throws JsonReadException {
    for (int i = 0; i < literal.length(); i++) {
      if (!hasChar(index + i) || charAt(index + i) != literal.charAt(i)) {
        return false;
      }
    }
    return true;
  }

  private void expect(char expected) throws JsonReadException {
    skipWhitespace();
    if (!peekChar(expected)) {
      throw error("MJJBP-019", "Expected '" + expected + "'.");
    }
    advance();
  }

  private Context requireContext(ContextKind kind) throws JsonReadException {
    Context context = requireAnyContext();
    if (context.kind != kind) {
      throw error("MJJBP-020", "Unexpected JSON structure.");
    }
    return context;
  }

  private Context requireAnyContext() throws JsonReadException {
    if (stack.isEmpty()) {
      throw error("MJJBP-021", "No open JSON container.");
    }
    return stack.peek();
  }

  private boolean peekChar(char expected) throws JsonReadException {
    return hasChar(index) && charAt(index) == expected;
  }

  private void skipWhitespace() throws JsonReadException {
    while (hasChar(index)) {
      char current = charAt(index);
      if (current != ' ' && current != '\n' && current != '\r' && current != '\t') {
        return;
      }
      advance();
    }
  }

  private char advance() throws JsonReadException {
    char current = charAt(index++);
    if (current == '\n') {
      line++;
      column = 1;
    } else {
      column++;
    }
    return current;
  }

  private char charAt(int position) throws JsonReadException {
    if (!hasChar(position)) {
      throw error("MJJBP-022", "Unexpected end of JSON input.");
    }
    return input.charAt(position);
  }

  private boolean hasChar(int position) throws JsonReadException {
    while (position >= input.length() && !endOfInput) {
      try {
        int read = reader.read(readBuffer, 0, readBuffer.length);
        if (read == -1) {
          endOfInput = true;
        } else if (read > 0) {
          input.append(readBuffer, 0, read);
        } else {
          throw error("MJJBP-023", "Unable to read JSON input: reader returned no characters.");
        }
      } catch (IOException exception) {
        throw error("MJJBP-023", "Unable to read JSON input: " + exception.getMessage());
      }
    }
    return position < input.length();
  }

  private JsonReadException error(String code, String message) {
    return new JsonReadException(JsonDiagnostic.error(code, message, JsonPath.ROOT, location()));
  }

  private static boolean isNumberStart(char current) {
    return current == '-' || Character.isDigit(current);
  }

  private enum ContextKind {
    OBJECT,
    ARRAY
  }

  private static final class Context {
    private final ContextKind kind;
    private boolean first = true;
    private boolean prepared;
    private boolean expectingName;

    private Context(ContextKind kind) {
      this.kind = kind;
      this.expectingName = kind == ContextKind.OBJECT;
    }
  }
}
