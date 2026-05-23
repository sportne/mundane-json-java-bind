package io.github.mundanej.mjjb.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

final class JsonFieldTest {
  @Test
  void absentFieldHasNoValue() {
    JsonField<String> field = JsonField.absent();

    assertTrue(field.isAbsent());
    assertFalse(field.isExplicitNull());
    assertFalse(field.hasValue());
    assertEquals(Optional.empty(), field.value());
  }

  @Test
  void explicitNullFieldHasNoValue() {
    JsonField<String> field = JsonField.explicitNull();

    assertFalse(field.isAbsent());
    assertTrue(field.isExplicitNull());
    assertFalse(field.hasValue());
    assertEquals(Optional.empty(), field.value());
  }

  @Test
  void valueFieldExposesOptionalValue() {
    JsonField<String> field = JsonField.value("value");

    assertFalse(field.isAbsent());
    assertFalse(field.isExplicitNull());
    assertTrue(field.hasValue());
    assertEquals(Optional.of("value"), field.value());
  }

  @Test
  void requireValueReturnsContainedValue() {
    assertEquals("value", JsonField.value("value").requireValue());
  }

  @Test
  void requireValueRejectsAbsentAndExplicitNullFields() {
    IllegalStateException absent =
        assertThrows(IllegalStateException.class, () -> JsonField.absent().requireValue());
    IllegalStateException explicitNull =
        assertThrows(IllegalStateException.class, () -> JsonField.explicitNull().requireValue());

    assertEquals("field does not contain a value", absent.getMessage());
    assertEquals("field does not contain a value", explicitNull.getMessage());
  }

  @Test
  void valueRejectsNull() {
    NullPointerException exception =
        assertThrows(NullPointerException.class, () -> JsonField.value(null));

    assertEquals("value", exception.getMessage());
  }

  @Test
  void equalityAndHashCodeIncludeStateAndValue() {
    assertEquals(JsonField.absent(), JsonField.absent());
    assertEquals(JsonField.absent().hashCode(), JsonField.absent().hashCode());
    assertEquals(JsonField.explicitNull(), JsonField.explicitNull());
    assertEquals(JsonField.explicitNull().hashCode(), JsonField.explicitNull().hashCode());
    assertEquals(JsonField.value("value"), JsonField.value("value"));
    assertEquals(JsonField.value("value").hashCode(), JsonField.value("value").hashCode());

    assertNotEquals(JsonField.absent(), JsonField.explicitNull());
    assertNotEquals(JsonField.absent(), JsonField.value("value"));
    assertNotEquals(JsonField.explicitNull(), JsonField.value("value"));
    assertNotEquals(JsonField.value("value"), JsonField.value("other"));
  }

  @Test
  void fieldDoesNotEqualUnrelatedObject() {
    assertNotEquals(JsonField.value("value"), "value");
  }

  @Test
  void toStringDescribesStateAndValue() {
    assertEquals("JsonField.absent", JsonField.absent().toString());
    assertEquals("JsonField.explicitNull", JsonField.explicitNull().toString());
    assertEquals("JsonField.value[value]", JsonField.value("value").toString());
  }
}
