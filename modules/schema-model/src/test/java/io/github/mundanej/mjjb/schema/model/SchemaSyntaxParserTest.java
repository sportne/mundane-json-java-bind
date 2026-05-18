package io.github.mundanej.mjjb.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.ArrayValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.BooleanValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.NullValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.NumberValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.ObjectValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.StringValue;
import org.junit.jupiter.api.Test;

final class SchemaSyntaxParserTest {
  @Test
  void parsesObjectMembersInSourceOrderWithPointers() {
    SchemaSyntaxParseResult result =
        SchemaSyntaxParser.parse(
            """
            {
              "type": "object",
              "properties": {
                "id": {"type": "string"},
                "count": {"minimum": 1.5e2}
              }
            }
            """);

    assertTrue(result.successful());
    ObjectValue root = assertInstanceOf(ObjectValue.class, result.root());
    assertEquals(JsonPointer.ROOT, root.pointer());
    assertEquals("type", root.members().get(0).name());
    assertEquals("/type", root.members().get(0).pointer().value());
    assertEquals("properties", root.members().get(1).name());

    ObjectValue properties = assertInstanceOf(ObjectValue.class, root.members().get(1).value());
    assertEquals("/properties", properties.pointer().value());
    assertEquals("id", properties.members().get(0).name());
    assertEquals("/properties/id", properties.members().get(0).pointer().value());

    ObjectValue count = assertInstanceOf(ObjectValue.class, properties.members().get(1).value());
    NumberValue minimum = assertInstanceOf(NumberValue.class, count.members().getFirst().value());
    assertEquals("/properties/count/minimum", minimum.pointer().value());
    assertEquals("1.5e2", minimum.literal());
  }

  @Test
  void parsesArraysAndScalarValuesWithIndexedPointers() {
    SchemaSyntaxParseResult result =
        SchemaSyntaxParser.parse("[\"open\", 0, -8.25E-3, true, false, null]");

    assertTrue(result.successful());
    ArrayValue root = assertInstanceOf(ArrayValue.class, result.root());
    assertEquals(JsonPointer.ROOT, root.pointer());
    assertEquals("open", assertInstanceOf(StringValue.class, root.items().get(0)).value());
    assertEquals("/0", root.items().get(0).pointer().value());
    assertEquals("0", assertInstanceOf(NumberValue.class, root.items().get(1)).literal());
    assertEquals("/1", root.items().get(1).pointer().value());
    assertEquals("-8.25E-3", assertInstanceOf(NumberValue.class, root.items().get(2)).literal());
    assertTrue(assertInstanceOf(BooleanValue.class, root.items().get(3)).value());
    assertFalse(assertInstanceOf(BooleanValue.class, root.items().get(4)).value());
    assertInstanceOf(NullValue.class, root.items().get(5));
    assertEquals("/5", root.items().get(5).pointer().value());
  }

  @Test
  void escapesPropertyNamesForJsonPointers() {
    SchemaSyntaxParseResult result =
        SchemaSyntaxParser.parse("{\"a/b\":true,\"c~d\":false,\"e\\\\f\":null}");

    assertTrue(result.successful());
    ObjectValue root = assertInstanceOf(ObjectValue.class, result.root());

    assertEquals("/a~1b", root.members().get(0).pointer().value());
    assertEquals("/c~0d", root.members().get(1).pointer().value());
    assertEquals("/e\\f", root.members().get(2).pointer().value());
  }

  @Test
  void preservesStringValuesThatMentionKeywordNames() {
    SchemaSyntaxParseResult result =
        SchemaSyntaxParser.parse("{\"description\":\"mentions $ref and allOf as plain text\"}");

    assertTrue(result.successful());
    ObjectValue root = assertInstanceOf(ObjectValue.class, result.root());
    StringValue description =
        assertInstanceOf(StringValue.class, root.members().getFirst().value());

    assertEquals("/description", description.pointer().value());
    assertEquals("mentions $ref and allOf as plain text", description.value());
  }

  @Test
  void rejectsInvalidJsonInputsWithDeterministicDiagnostics() {
    assertInvalid("", "");
    assertInvalid("{\"type\":\"object\"", "");
    assertInvalid("{\"minimum\":1.}", "/minimum");
    assertInvalid("{\"minimum\":1e}", "/minimum");
    assertInvalid("{\"minimum\":-}", "/minimum");
    assertInvalid("{\"minimum\":01}", "/minimum");
    assertInvalid("{\"description\":\"bad\\q\"}", "/description");
    assertInvalid("{\"description\":\"bad\nstring\"}", "/description");
    assertInvalid("{\"type\":tru}", "/type");
    assertInvalid("{\"type\":\"string\"} []", "");
    assertInvalid("{\"type\", \"string\"}", "/type");
    assertInvalid("{\"enum\":[1,]}", "/enum/1");
  }

  private void assertInvalid(String source, String pointer) {
    SchemaSyntaxParseResult result = SchemaSyntaxParser.parse(source);

    assertFalse(result.successful());
    assertEquals(SchemaSyntaxParser.INVALID_JSON_CODE, result.diagnostics().getFirst().code());
    assertEquals(pointer, result.diagnostics().getFirst().pointer().value());
  }
}
