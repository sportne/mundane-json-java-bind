package io.github.mundanej.mjjb.examples.basicrecord.generated;

import io.github.mundanej.mjjb.runtime.JsonDiagnostic;
import io.github.mundanej.mjjb.runtime.JsonPath;
import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.JsonReader;
import io.github.mundanej.mjjb.runtime.JsonToken;
import java.util.Objects;
import java.util.Optional;

public final class BasicRecordJsonReader {
  private BasicRecordJsonReader() {}

  public static BasicRecord read(JsonReader reader) throws JsonReadException {
    Objects.requireNonNull(reader, "reader");
    if (reader.peek() != JsonToken.BEGIN_OBJECT) {
      throw error("MJJBR-001", "Expected root JSON object.", JsonPath.ROOT, reader.location());
    }
    reader.beginObject();
    String id = null;
    boolean idSeen = false;
    long count = 0L;
    boolean countSeen = false;
    Optional<String> displayName = Optional.empty();
    boolean displayNameSeen = false;
    Optional<Double> score = Optional.empty();
    boolean scoreSeen = false;
    Optional<Boolean> active = Optional.empty();
    boolean activeSeen = false;
    while (reader.hasNext()) {
      String name = reader.nextName();
      switch (name) {
        case "id" -> {
          if (idSeen) {
            throw duplicateProperty("id", reader.location());
          }
          id = readString(reader, propertyPath("id"));
          idSeen = true;
        }
        case "count" -> {
          if (countSeen) {
            throw duplicateProperty("count", reader.location());
          }
          count = readInteger(reader, propertyPath("count"));
          countSeen = true;
        }
        case "displayName" -> {
          if (displayNameSeen) {
            throw duplicateProperty("displayName", reader.location());
          }
          displayName = Optional.of(readString(reader, propertyPath("displayName")));
          displayNameSeen = true;
        }
        case "score" -> {
          if (scoreSeen) {
            throw duplicateProperty("score", reader.location());
          }
          score = Optional.of(readNumber(reader, propertyPath("score")));
          scoreSeen = true;
        }
        case "active" -> {
          if (activeSeen) {
            throw duplicateProperty("active", reader.location());
          }
          active = Optional.of(readBoolean(reader, propertyPath("active")));
          activeSeen = true;
        }
        default ->
            throw error("MJJBR-004", "Unknown JSON property '" + name + "'.", propertyPath(name), reader.location());
      }
    }
    reader.endObject();
    if (!idSeen) {
      throw missingRequired("id", propertyPath("id"), reader.location());
    }
    if (!countSeen) {
      throw missingRequired("count", propertyPath("count"), reader.location());
    }
    if (reader.peek() != JsonToken.END_DOCUMENT) {
      throw error("MJJBR-002", "Unexpected JSON content after root value.", JsonPath.ROOT, reader.location());
    }
    return new BasicRecord(id, count, displayName, score, active);
  }

  private static JsonReadException duplicateProperty(
      String name, io.github.mundanej.mjjb.runtime.JsonLocation location) {
    return error("MJJBR-003", "Duplicate JSON property '" + name + "'.", propertyPath(name), location);
  }

  private static JsonReadException missingRequired(
      String name, JsonPath path, io.github.mundanej.mjjb.runtime.JsonLocation location) {
    return error("MJJBR-005", "Missing required JSON property '" + name + "'.", path, location);
  }

  private static String readString(JsonReader reader, JsonPath path)
      throws JsonReadException {
    requireToken(reader, JsonToken.STRING, "MJJBR-006", "Expected JSON string.", path);
    try {
      return reader.nextString();
    } catch (JsonReadException exception) {
      throw atPath(exception, path);
    }
  }

  private static long readInteger(JsonReader reader, JsonPath path)
      throws JsonReadException {
    String literal =
        readNumberLiteral(reader, path, "MJJBR-007", "Expected JSON integer.");
    if (!isIntegerLiteral(literal)) {
      throw error(
          "MJJBR-007", "Expected JSON integer.", path, reader.location());
    }
    try {
      return Long.parseLong(literal);
    } catch (NumberFormatException exception) {
      throw error(
          "MJJBR-007", "Expected JSON integer.", path, reader.location());
    }
  }

  private static boolean isIntegerLiteral(String literal) {
    return literal.indexOf('.') < 0 && literal.indexOf('e') < 0 && literal.indexOf('E') < 0;
  }

  private static double readNumber(JsonReader reader, JsonPath path)
      throws JsonReadException {
    String literal =
        readNumberLiteral(reader, path, "MJJBR-008", "Expected JSON number.");
    double value;
    try {
      value = Double.parseDouble(literal);
    } catch (NumberFormatException exception) {
      throw error("MJJBR-008", "Expected JSON number.", path, reader.location());
    }
    if (!Double.isFinite(value)) {
      throw error("MJJBR-008", "Expected finite JSON number.", path, reader.location());
    }
    return value;
  }

  private static boolean readBoolean(JsonReader reader, JsonPath path)
      throws JsonReadException {
    requireToken(
        reader, JsonToken.BOOLEAN, "MJJBR-009", "Expected JSON boolean.", path);
    try {
      return reader.nextBoolean();
    } catch (JsonReadException exception) {
      throw atPath(exception, path);
    }
  }

  private static String readNumberLiteral(
      JsonReader reader, JsonPath path, String code, String message)
      throws JsonReadException {
    requireToken(reader, JsonToken.NUMBER, code, message, path);
    try {
      return reader.nextNumberLiteral();
    } catch (JsonReadException exception) {
      throw atPath(exception, path);
    }
  }

  private static void requireToken(
      JsonReader reader, JsonToken expected, String code, String message, JsonPath path)
      throws JsonReadException {
    JsonToken actual;
    try {
      actual = reader.peek();
    } catch (JsonReadException exception) {
      throw atPath(exception, path);
    }
    if (actual != expected) {
      throw error(code, message, path, reader.location());
    }
  }

  private static JsonReadException atPath(JsonReadException exception, JsonPath path) {
    JsonDiagnostic diagnostic = exception.diagnostic();
    return error(diagnostic.code(), diagnostic.message(), path, diagnostic.location());
  }

  private static JsonReadException error(
      String code, String message, JsonPath path, io.github.mundanej.mjjb.runtime.JsonLocation location) {
    return new JsonReadException(JsonDiagnostic.error(code, message, path, location));
  }

  private static JsonPath propertyPath(String name) {
    return JsonPath.ROOT.property(name);
  }
}
