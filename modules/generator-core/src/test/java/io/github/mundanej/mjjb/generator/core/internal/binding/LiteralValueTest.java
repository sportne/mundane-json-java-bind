package io.github.mundanej.mjjb.generator.core.internal.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class LiteralValueTest {
  @Test
  void carriesStringNumberBooleanAndNullLiterals() {
    LiteralValue string = new LiteralValue(LiteralValue.Kind.STRING, "open");
    LiteralValue integer = new LiteralValue(LiteralValue.Kind.INTEGER, "42");
    LiteralValue number = new LiteralValue(LiteralValue.Kind.NUMBER, "1.25");
    LiteralValue bool = new LiteralValue(LiteralValue.Kind.BOOLEAN, "true");
    LiteralValue nil = new LiteralValue(LiteralValue.Kind.NULL, "ignored");

    assertEquals(LiteralValue.Kind.STRING, string.kind());
    assertEquals("open", string.value());
    assertEquals(LiteralValue.Kind.INTEGER, integer.kind());
    assertEquals("42", integer.value());
    assertEquals(LiteralValue.Kind.NUMBER, number.kind());
    assertEquals("1.25", number.value());
    assertEquals(LiteralValue.Kind.BOOLEAN, bool.kind());
    assertEquals("true", bool.value());
    assertEquals(LiteralValue.Kind.NULL, nil.kind());
    assertEquals("", nil.value());
  }

  @Test
  void normalizedKeysIncludeLiteralKindPrefixes() {
    assertEquals("s:1", new LiteralValue(LiteralValue.Kind.STRING, "1").normalizedKey());
    assertEquals("i:1", new LiteralValue(LiteralValue.Kind.INTEGER, "1").normalizedKey());
    assertEquals("n:1", new LiteralValue(LiteralValue.Kind.NUMBER, "1").normalizedKey());
    assertEquals("b:false", new LiteralValue(LiteralValue.Kind.BOOLEAN, "false").normalizedKey());
    assertEquals("z:null", new LiteralValue(LiteralValue.Kind.NULL, "").normalizedKey());
  }

  @Test
  void normalizedKeyNormalizesNumberScaleAndExponent() {
    assertEquals(
        new LiteralValue(LiteralValue.Kind.NUMBER, "2").normalizedKey(),
        new LiteralValue(LiteralValue.Kind.NUMBER, "2.0").normalizedKey());
    assertEquals(
        new LiteralValue(LiteralValue.Kind.NUMBER, "2").normalizedKey(),
        new LiteralValue(LiteralValue.Kind.NUMBER, "2e0").normalizedKey());
    assertEquals("n:1.23", new LiteralValue(LiteralValue.Kind.NUMBER, "1.2300").normalizedKey());
    assertEquals("n:0", new LiteralValue(LiteralValue.Kind.NUMBER, "-0.00").normalizedKey());
  }

  @Test
  void equalityUsesRecordKindAndStoredValue() {
    assertEquals(
        new LiteralValue(LiteralValue.Kind.STRING, "open"),
        new LiteralValue(LiteralValue.Kind.STRING, "open"));
    assertNotEquals(
        new LiteralValue(LiteralValue.Kind.STRING, "open"),
        new LiteralValue(LiteralValue.Kind.STRING, "closed"));
    assertNotEquals(
        new LiteralValue(LiteralValue.Kind.STRING, "1"),
        new LiteralValue(LiteralValue.Kind.INTEGER, "1"));
    assertNotEquals(
        new LiteralValue(LiteralValue.Kind.NUMBER, "2"),
        new LiteralValue(LiteralValue.Kind.NUMBER, "2.0"));
    assertEquals(
        new LiteralValue(LiteralValue.Kind.NULL, "left"),
        new LiteralValue(LiteralValue.Kind.NULL, "right"));
  }

  @Test
  void rejectsNullKindAndNullNonNullLiteralValue() {
    assertThrows(NullPointerException.class, () -> new LiteralValue(null, ""));
    assertThrows(
        NullPointerException.class, () -> new LiteralValue(LiteralValue.Kind.STRING, null));
    assertThrows(
        NullPointerException.class, () -> new LiteralValue(LiteralValue.Kind.INTEGER, null));
    assertThrows(
        NullPointerException.class, () -> new LiteralValue(LiteralValue.Kind.NUMBER, null));
    assertThrows(
        NullPointerException.class, () -> new LiteralValue(LiteralValue.Kind.BOOLEAN, null));
  }

  @Test
  void nullLiteralNormalizesNullAndBlankValuesToEmptyValue() {
    assertEquals("", new LiteralValue(LiteralValue.Kind.NULL, null).value());
    assertEquals("", new LiteralValue(LiteralValue.Kind.NULL, "").value());
    assertEquals("", new LiteralValue(LiteralValue.Kind.NULL, " ").value());
  }
}
