package io.github.mundanej.mjjb.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class JsonPathTest {
  @Test
  void rootComposesPropertiesAndArrayIndexes() {
    assertEquals("$", JsonPath.ROOT.value());
    assertEquals(
        "$.user.addresses[0].city",
        JsonPath.ROOT.property("user").property("addresses").index(0).property("city").value());
  }

  @Test
  void simpleIdentifierPropertyNamesUseDotSyntax() {
    assertEquals("$._meta.user2", JsonPath.ROOT.property("_meta").property("user2").value());
  }

  @Test
  void nonIdentifierPropertyNamesUseBracketSyntax() {
    assertEquals(
        "$[\"full-name\"][\"postal code\"][\"a.b\"]",
        JsonPath.ROOT.property("full-name").property("postal code").property("a.b").value());
  }

  @Test
  void propertyNamesStartingWithDigitsUseBracketSyntax() {
    assertEquals("$[\"0\"][\"1name\"]", JsonPath.ROOT.property("0").property("1name").value());
  }

  @Test
  void emptyPropertyNamesUseBracketSyntax() {
    assertEquals("$[\"\"]", JsonPath.ROOT.property("").value());
  }

  @Test
  void bracketPropertyNamesEscapeJsonStringCharacters() {
    assertEquals(
        "$[\"quote\\\"backslash\\\\\"][\"back\\bform\\fline\\nreturn\\rtab\\t\"][\"control\\u0001\"]",
        JsonPath.ROOT
            .property("quote\"backslash\\")
            .property("back\bform\fline\nreturn\rtab\t")
            .property("control\u0001")
            .value());
  }

  @Test
  void propertyRejectsNullNames() {
    assertThrows(NullPointerException.class, JsonPathTest::nullProperty);
  }

  @Test
  void indexRejectsNegativeValues() {
    assertThrows(IllegalArgumentException.class, JsonPathTest::negativeIndex);
  }

  @Test
  void constructorRejectsNullValue() {
    assertThrows(NullPointerException.class, () -> new JsonPath(null));
  }

  @Test
  void constructorRejectsBlankOrUnrootedValues() {
    assertThrows(IllegalArgumentException.class, () -> new JsonPath(""));
    assertThrows(IllegalArgumentException.class, () -> new JsonPath(" "));
    assertThrows(IllegalArgumentException.class, () -> new JsonPath("user"));
    assertThrows(IllegalArgumentException.class, () -> new JsonPath(" $.user"));
  }

  private static void nullProperty() {
    JsonPath ignored = JsonPath.ROOT.property(null);
    throw new AssertionError("Expected null property to be rejected, got " + ignored.value());
  }

  private static void negativeIndex() {
    JsonPath ignored = JsonPath.ROOT.index(-1);
    throw new AssertionError("Expected negative index to be rejected, got " + ignored.value());
  }
}
