package io.github.mundanej.mjjb.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.JsonToken;
import io.github.mundanej.mjjb.testkit.DeterministicFuzzConfig;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("fuzz")
final class JsonStreamReaderFuzzTest {
  private static final DeterministicFuzzConfig FUZZ =
      DeterministicFuzzConfig.fromSystemProperties(0xC0FFEE42L, 96, 5, 24);

  @Test
  void parsesGeneratedValidJsonWithStringAndReaderBackedInputs() throws JsonReadException {
    for (int iteration = 0; iteration < FUZZ.iterations(); iteration++) {
      SplittableRandom random = new SplittableRandom(FUZZ.seed() + iteration * 0x9E3779B97F4A7C15L);
      String json = whitespace(random) + value(random, FUZZ.maxDepth()) + whitespace(random);
      String label = FUZZ.caseLabel("parser-valid", iteration, json);

      parseComplete(new JsonStreamReader(json), label);
      parseComplete(JsonStreamReader.fromReader("fuzz.json", new StringReader(json)), label);
    }
  }

  @Test
  void rejectsTargetedInvalidJsonMutations() {
    ArrayList<String> invalidInputs = new ArrayList<>();
    invalidInputs.add("\"\\x\"");
    invalidInputs.add("\"\\u12\"");
    invalidInputs.add("\"bad\nstring\"");
    invalidInputs.add("1.");
    invalidInputs.add("01");
    invalidInputs.add("-");
    invalidInputs.add("[1,]");
    invalidInputs.add("[1 2]");
    invalidInputs.add("{\"a\":1 \"b\":2}");
    invalidInputs.add("{\"a\" 1}");

    for (int iteration = 0; iteration < FUZZ.iterations(); iteration++) {
      SplittableRandom random = new SplittableRandom(FUZZ.seed() ^ iteration * 0xD1B54A32D192ED03L);
      String valid = "[" + value(random, FUZZ.maxDepth()) + "]";
      invalidInputs.add(valid.substring(0, Math.max(0, valid.length() - 1)));
    }

    for (int iteration = 0; iteration < invalidInputs.size(); iteration++) {
      String json = invalidInputs.get(iteration);
      String label = FUZZ.caseLabel("parser-invalid", iteration, json);
      assertThrows(
          JsonReadException.class, () -> parseComplete(new JsonStreamReader(json), label), label);
    }
  }

  private static void parseComplete(JsonStreamReader reader, String label)
      throws JsonReadException {
    reader.skipValue();
    assertEquals(JsonToken.END_DOCUMENT, reader.peek(), label);
  }

  private static String value(SplittableRandom random, int depth) {
    if (depth <= 0) {
      return scalar(random);
    }
    return switch (random.nextInt(6)) {
      case 0 -> object(random, depth - 1);
      case 1 -> array(random, depth - 1);
      default -> scalar(random);
    };
  }

  private static String scalar(SplittableRandom random) {
    return switch (random.nextInt(5)) {
      case 0 -> stringLiteral(random);
      case 1 -> numberLiteral(random);
      case 2 -> "true";
      case 3 -> "false";
      default -> "null";
    };
  }

  private static String object(SplittableRandom random, int depth) {
    int size = random.nextInt(0, 5);
    LinkedHashSet<String> names = new LinkedHashSet<>();
    while (names.size() < size) {
      names.add("p" + random.nextInt(10_000));
    }
    ArrayList<String> members = new ArrayList<>();
    for (String name : names) {
      members.add(
          stringLiteral(name)
              + whitespace(random)
              + ":"
              + whitespace(random)
              + value(random, depth));
    }
    return "{"
        + whitespace(random)
        + String.join("," + whitespace(random), members)
        + whitespace(random)
        + "}";
  }

  private static String array(SplittableRandom random, int depth) {
    int size = random.nextInt(0, 6);
    ArrayList<String> values = new ArrayList<>();
    for (int index = 0; index < size; index++) {
      values.add(value(random, depth));
    }
    return "["
        + whitespace(random)
        + String.join("," + whitespace(random), values)
        + whitespace(random)
        + "]";
  }

  private static String stringLiteral(SplittableRandom random) {
    int length = random.nextInt(0, FUZZ.maxStringLength() + 1);
    StringBuilder value = new StringBuilder();
    for (int index = 0; index < length; index++) {
      switch (random.nextInt(12)) {
        case 0 -> value.append("\\\"");
        case 1 -> value.append("\\\\");
        case 2 -> value.append("\\/");
        case 3 -> value.append("\\b");
        case 4 -> value.append("\\f");
        case 5 -> value.append("\\n");
        case 6 -> value.append("\\r");
        case 7 -> value.append("\\t");
        case 8 -> value.append("\\u").append(hex4(random.nextInt(0x20, 0x7F)));
        default -> {
          char current = (char) random.nextInt(0x20, 0x7F);
          if (current == '"' || current == '\\') {
            value.append('\\');
          }
          value.append(current);
        }
      }
    }
    return "\"" + value + "\"";
  }

  private static String stringLiteral(String value) {
    StringBuilder literal = new StringBuilder("\"");
    for (int index = 0; index < value.length(); index++) {
      char current = value.charAt(index);
      if (current == '"' || current == '\\') {
        literal.append('\\');
      }
      literal.append(current);
    }
    return literal.append('"').toString();
  }

  private static String numberLiteral(SplittableRandom random) {
    StringBuilder number = new StringBuilder();
    if (random.nextBoolean()) {
      number.append('-');
    }
    if (random.nextInt(4) == 0) {
      number.append('0');
    } else {
      number.append(random.nextInt(1, 10));
      int digits = random.nextInt(0, 6);
      for (int index = 0; index < digits; index++) {
        number.append(random.nextInt(10));
      }
    }
    if (random.nextBoolean()) {
      number.append('.');
      int fractionDigits = random.nextInt(1, 5);
      for (int index = 0; index < fractionDigits; index++) {
        number.append(random.nextInt(10));
      }
    }
    if (random.nextInt(3) == 0) {
      number.append(random.nextBoolean() ? 'e' : 'E');
      if (random.nextBoolean()) {
        number.append(random.nextBoolean() ? '+' : '-');
      }
      number.append(random.nextInt(10));
      if (random.nextBoolean()) {
        number.append(random.nextInt(10));
      }
    }
    return number.toString();
  }

  private static String whitespace(SplittableRandom random) {
    List<String> choices = List.of("", " ", "\n", "\r\n", "\t");
    return choices.get(random.nextInt(choices.size()));
  }

  private static String hex4(int value) {
    String hex = Integer.toHexString(value);
    return "0".repeat(4 - hex.length()) + hex;
  }
}
