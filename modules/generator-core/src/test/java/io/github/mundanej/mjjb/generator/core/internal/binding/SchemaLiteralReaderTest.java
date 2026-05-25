package io.github.mundanej.mjjb.generator.core.internal.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjjb.schema.model.SchemaSyntaxParseResult;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxParser;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxValue.ObjectValue;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SchemaLiteralReaderTest {
  @Test
  void readsAnnotationsWithCompactJsonValues() {
    ObjectValue schema =
        parseObject(
            """
            {
              "title": "Example",
              "description": "Description",
              "$comment": "Comment",
              "examples": [{"b": 2, "a": 1}, ["x", null]],
              "deprecated": true,
              "readOnly": false,
              "writeOnly": true,
              "default": {"z": 0}
            }
            """);

    SchemaAnnotationsBinding annotations = SchemaLiteralReader.annotations(schema);

    assertEquals("Example", annotations.title().orElseThrow());
    assertEquals("Description", annotations.description().orElseThrow());
    assertEquals("Comment", annotations.comment().orElseThrow());
    assertEquals(List.of("{\"b\":2,\"a\":1}", "[\"x\",null]"), annotations.examplesJson());
    assertEquals(true, annotations.deprecated().orElseThrow());
    assertEquals(false, annotations.readOnly().orElseThrow());
    assertEquals(true, annotations.writeOnly().orElseThrow());
    assertEquals("{\"z\":0}", annotations.defaultJson().orElseThrow());
  }

  @Test
  void comparesJsonValuesUsingCanonicalObjectMemberOrder() {
    SchemaSyntaxValue left = memberValue(parseObject("{\"value\":{\"b\":2,\"a\":1}}"), "value");
    SchemaSyntaxValue right = memberValue(parseObject("{\"value\":{\"a\":1,\"b\":2}}"), "value");

    assertTrue(SchemaLiteralReader.sameJsonValue(left, right));
  }

  @Test
  void readsNullCompatibleLiteralConstraints() {
    ArrayList<BindingDiagnostic> diagnostics = new ArrayList<>();

    LiteralConstraints constraints =
        SchemaLiteralReader.literalConstraints(
                parseObject("{\"enum\":[\"open\",null],\"const\":\"open\",\"default\":null}"),
                JavaScalarType.STRING,
                "status",
                diagnostics)
            .orElseThrow();

    assertTrue(diagnostics.isEmpty());
    assertEquals(
        List.of(LiteralValue.Kind.STRING, LiteralValue.Kind.NULL),
        constraints.enumValues().stream().map(LiteralValue::kind).toList());
    assertEquals("open", constraints.constValue().orElseThrow().value());
    assertEquals(LiteralValue.Kind.NULL, constraints.defaultValue().orElseThrow().kind());
  }

  @Test
  void rejectsDuplicateNumberEnumValuesWithStablePointer() {
    ArrayList<BindingDiagnostic> diagnostics = new ArrayList<>();

    assertFalse(
        SchemaLiteralReader.literalConstraints(
                parseObject("{\"enum\":[1,1.0]}"), JavaScalarType.NUMBER, "amount", diagnostics)
            .isPresent());

    assertEquals(1, diagnostics.size());
    assertEquals(BindingDiagnostic.INVALID_LITERAL_CONSTRAINT_CODE, diagnostics.getFirst().code());
    assertEquals("/enum/1", diagnostics.getFirst().pointer().value());
  }

  private static ObjectValue parseObject(String schema) {
    SchemaSyntaxParseResult parseResult = SchemaSyntaxParser.parse(schema);
    assertTrue(parseResult.diagnostics().isEmpty());
    assertTrue(parseResult.root() instanceof ObjectValue);
    return (ObjectValue) parseResult.root();
  }

  private static SchemaSyntaxValue memberValue(ObjectValue objectValue, String name) {
    return BindingModelBuilder.member(objectValue, name).orElseThrow().value();
  }
}
