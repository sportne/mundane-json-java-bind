package io.github.mundanej.mjjb.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class JsonPointerTest {
  @Test
  void rootPointerUsesEmptyValue() {
    assertEquals("", JsonPointer.ROOT.value());
    assertEquals(JsonPointer.ROOT, new JsonPointer(""));
  }

  @Test
  void propertyTokensEscapeTildeAndSlash() {
    JsonPointer pointer = JsonPointer.ROOT.property("a~b/c").property("~").property("/");

    assertEquals("/a~0b~1c/~0/~1", pointer.value());
  }

  @Test
  void propertyAcceptsEmptyToken() {
    assertEquals("/", JsonPointer.ROOT.property("").value());
    assertEquals("/properties/", JsonPointer.ROOT.property("properties").property("").value());
  }

  @Test
  void indexComposesWithPropertiesAndOtherIndexes() {
    JsonPointer pointer =
        JsonPointer.ROOT.property("properties").property("items").index(0).index(12);

    assertEquals("/properties/items/0/12", pointer.value());
  }

  @Test
  void propertyRejectsNullToken() {
    NullPointerException exception =
        assertThrows(NullPointerException.class, JsonPointerTest::propertyWithNullToken);

    assertEquals("token", exception.getMessage());
  }

  @Test
  void indexRejectsNegativeValues() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, JsonPointerTest::indexWithNegativeValue);

    assertEquals("index must be non-negative", exception.getMessage());
  }

  @Test
  void constructorRejectsNullValue() {
    NullPointerException exception =
        assertThrows(NullPointerException.class, () -> new JsonPointer(null));

    assertEquals("value", exception.getMessage());
  }

  @Test
  void constructorRejectsNonRootValuesThatDoNotStartWithSlash() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> new JsonPointer("properties/id"));

    assertEquals("value must be empty or start with /", exception.getMessage());
  }

  @Test
  void equalityHashCodeAndToStringReflectPointerValue() {
    JsonPointer left = JsonPointer.ROOT.property("properties").property("id");
    JsonPointer right = new JsonPointer("/properties/id");

    assertEquals(left, right);
    assertEquals(left.hashCode(), right.hashCode());
    assertNotEquals(left, JsonPointer.ROOT.property("properties").property("name"));
    assertEquals("JsonPointer[value=/properties/id]", left.toString());
  }

  private static JsonPointer propertyWithNullToken() {
    return JsonPointer.ROOT.property(null);
  }

  private static JsonPointer indexWithNegativeValue() {
    return JsonPointer.ROOT.index(-1);
  }
}
