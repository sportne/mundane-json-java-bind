package io.github.mundanej.mjjb.generator.core.internal.binding;

import io.github.mundanej.mjjb.schema.model.JsonPointer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class JavaNameAllocator {
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

  private JavaNameAllocator() {}

  static String uniqueFieldName(
      String propertyName, Map<String, JsonPointer> javaNames, JsonPointer pointer) {
    Objects.requireNonNull(javaNames, "javaNames");
    Objects.requireNonNull(pointer, "pointer");
    String baseName = fieldName(propertyName);
    String candidate = baseName;
    int suffix = 2;
    while (javaNames.containsKey(candidate)) {
      candidate = baseName + suffix;
      suffix++;
    }
    javaNames.put(candidate, pointer);
    return candidate;
  }

  static String uniqueNestedTypeName(
      String parentTypeName, String propertyName, Set<String> typeNames) {
    Objects.requireNonNull(parentTypeName, "parentTypeName");
    Objects.requireNonNull(typeNames, "typeNames");
    String baseName = parentTypeName + branchTypeName(propertyName, 0);
    String candidate = baseName;
    int suffix = 2;
    while (typeNames.contains(candidate)) {
      candidate = baseName + suffix;
      suffix++;
    }
    typeNames.add(candidate);
    return candidate;
  }

  static String branchTypeName(String tagValue, int index) {
    Objects.requireNonNull(tagValue, "tagValue");
    List<String> words = words(tagValue);
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

  static String fieldName(String propertyName) {
    List<String> words = words(propertyName);
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

  private static List<String> words(String value) {
    Objects.requireNonNull(value, "value");
    ArrayList<String> words = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
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
    return words;
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
}
