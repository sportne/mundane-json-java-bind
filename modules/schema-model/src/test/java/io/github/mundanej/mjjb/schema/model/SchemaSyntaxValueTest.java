package io.github.mundanej.mjjb.schema.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.ArrayValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.BooleanValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.Member;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.NullValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.NumberValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.ObjectValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.StringValue;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SchemaSyntaxValueTest {
  private static final JsonPointer STRING_POINTER = JsonPointer.ROOT.property("name");
  private static final JsonPointer NUMBER_POINTER = JsonPointer.ROOT.property("count");
  private static final JsonPointer BOOLEAN_POINTER = JsonPointer.ROOT.property("active");
  private static final JsonPointer NULL_POINTER = JsonPointer.ROOT.property("empty");

  @Test
  void objectValuePreservesPointerAndMembers() {
    Member member = new Member("name", new StringValue(STRING_POINTER, "value"));
    ObjectValue value = new ObjectValue(JsonPointer.ROOT, List.of(member));

    assertEquals(JsonPointer.ROOT, value.pointer());
    assertEquals(List.of(member), value.members());
    assertSame(member, value.members().getFirst());
  }

  @Test
  void objectValueDefensivelyCopiesMembersAndExposesImmutableList() {
    Member first = new Member("name", new StringValue(STRING_POINTER, "value"));
    ArrayList<Member> members = new ArrayList<>();
    members.add(first);

    ObjectValue value = new ObjectValue(JsonPointer.ROOT, members);
    members.add(new Member("count", new NumberValue(NUMBER_POINTER, "1")));

    assertEquals(List.of(first), value.members());
    assertThrows(
        UnsupportedOperationException.class,
        () -> value.members().add(new Member("active", new BooleanValue(BOOLEAN_POINTER, true))));
  }

  @Test
  void arrayValuePreservesPointerAndItems() {
    StringValue item = new StringValue(JsonPointer.ROOT.index(0), "value");
    ArrayValue value = new ArrayValue(JsonPointer.ROOT, List.of(item));

    assertEquals(JsonPointer.ROOT, value.pointer());
    assertEquals(List.of(item), value.items());
    assertSame(item, value.items().getFirst());
  }

  @Test
  void arrayValueDefensivelyCopiesItemsAndExposesImmutableList() {
    StringValue first = new StringValue(JsonPointer.ROOT.index(0), "value");
    ArrayList<SchemaSyntaxValue> items = new ArrayList<>();
    items.add(first);

    ArrayValue value = new ArrayValue(JsonPointer.ROOT, items);
    items.add(new NumberValue(JsonPointer.ROOT.index(1), "1"));

    assertEquals(List.of(first), value.items());
    assertThrows(
        UnsupportedOperationException.class,
        () -> value.items().add(new BooleanValue(JsonPointer.ROOT.index(2), true)));
  }

  @Test
  void memberPreservesNameValueAndValuePointer() {
    StringValue stringValue = new StringValue(STRING_POINTER, "value");
    Member member = new Member("name", stringValue);

    assertEquals("name", member.name());
    assertSame(stringValue, member.value());
    assertEquals(STRING_POINTER, member.pointer());
  }

  @Test
  void scalarValuesPreservePointersAndValues() {
    StringValue stringValue = new StringValue(STRING_POINTER, "value");
    NumberValue numberValue = new NumberValue(NUMBER_POINTER, "-1.25e2");
    BooleanValue booleanValue = new BooleanValue(BOOLEAN_POINTER, true);
    NullValue nullValue = new NullValue(NULL_POINTER);

    assertEquals(STRING_POINTER, stringValue.pointer());
    assertEquals("value", stringValue.value());
    assertEquals(NUMBER_POINTER, numberValue.pointer());
    assertEquals("-1.25e2", numberValue.literal());
    assertEquals(BOOLEAN_POINTER, booleanValue.pointer());
    assertTrue(booleanValue.value());
    assertEquals(NULL_POINTER, nullValue.pointer());
  }

  @Test
  void constructorsRejectNullFields() {
    assertAll(
        () -> assertThrows(NullPointerException.class, () -> new ObjectValue(null, List.of())),
        () ->
            assertThrows(NullPointerException.class, () -> new ObjectValue(JsonPointer.ROOT, null)),
        () ->
            assertThrows(
                NullPointerException.class, () -> new Member(null, new NullValue(NULL_POINTER))),
        () -> assertThrows(NullPointerException.class, () -> new Member("empty", null)),
        () -> assertThrows(NullPointerException.class, () -> new ArrayValue(null, List.of())),
        () ->
            assertThrows(NullPointerException.class, () -> new ArrayValue(JsonPointer.ROOT, null)),
        () -> assertThrows(NullPointerException.class, () -> new StringValue(null, "value")),
        () -> assertThrows(NullPointerException.class, () -> new StringValue(STRING_POINTER, null)),
        () -> assertThrows(NullPointerException.class, () -> new NumberValue(null, "1")),
        () -> assertThrows(NullPointerException.class, () -> new NumberValue(NUMBER_POINTER, null)),
        () -> assertThrows(NullPointerException.class, () -> new NullValue(null)),
        () -> assertThrows(NullPointerException.class, () -> new BooleanValue(null, true)));
  }

  @Test
  void objectAndArrayValuesRejectNullListElements() {
    assertThrows(
        NullPointerException.class, () -> new ObjectValue(JsonPointer.ROOT, listWithNull()));
    assertThrows(
        NullPointerException.class, () -> new ArrayValue(JsonPointer.ROOT, listWithNull()));
  }

  @Test
  void numberValueRejectsBlankLiteral() {
    assertThrows(IllegalArgumentException.class, () -> new NumberValue(NUMBER_POINTER, ""));
    assertThrows(IllegalArgumentException.class, () -> new NumberValue(NUMBER_POINTER, " "));
    assertThrows(IllegalArgumentException.class, () -> new NumberValue(NUMBER_POINTER, "\t\n"));
  }

  private static <T> List<T> listWithNull() {
    ArrayList<T> values = new ArrayList<>();
    values.add(null);
    return values;
  }
}
