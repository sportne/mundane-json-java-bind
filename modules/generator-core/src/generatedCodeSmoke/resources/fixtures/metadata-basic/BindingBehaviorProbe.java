import io.github.mundanej.mjjb.generated.GeneratedBindings;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonReader;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonSchemaMetadata;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonValidator;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonWriter;
import io.github.mundanej.mjjb.generator.core.generated.GeneratedSourceBehaviorProbe;
import io.github.mundanej.mjjb.parser.JsonStreamReader;
import io.github.mundanej.mjjb.parser.JsonStringWriter;
import io.github.mundanej.mjjb.runtime.JsonField;
import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.JsonWriteException;
import io.github.mundanej.mjjb.runtime.SchemaPropertyMetadata;
import io.github.mundanej.mjjb.runtime.SchemaRootMetadata;
import java.util.List;
import java.util.Optional;

public final class BindingBehaviorProbe implements GeneratedSourceBehaviorProbe {
  @Override
  public void run() throws JsonReadException, JsonWriteException {
    SchemaRootMetadata root = GeneratedBindingsJsonSchemaMetadata.root();
    assertEquals("GeneratedBindings", root.rootTypeName());
    assertEquals("Metadata Root", root.rootObject().annotations().title().orElseThrow());
    assertEquals("Root description", root.rootObject().annotations().description().orElseThrow());
    assertEquals("Root comment", root.rootObject().annotations().comment().orElseThrow());
    assertEquals(
        "{\"id\":\"abc\",\"count\":3}", root.rootObject().annotations().examplesJson().getFirst());
    assertEquals(Optional.of(false), root.rootObject().annotations().deprecated());
    assertEquals(Optional.of(true), root.rootObject().annotations().readOnly());
    assertEquals(Optional.of(false), root.rootObject().annotations().writeOnly());
    assertEquals(3, GeneratedBindingsJsonSchemaMetadata.properties().size());
    assertEquals(List.of(), GeneratedBindingsJsonSchemaMetadata.branches());

    SchemaPropertyMetadata id = GeneratedBindingsJsonSchemaMetadata.property("id").orElseThrow();
    assertEquals("id", id.jsonName());
    assertEquals("id", id.javaFieldName());
    assertEquals("/properties/id", id.schemaPointer());
    assertEquals("String", id.javaType());
    assertEquals("Identifier", id.annotations().title().orElseThrow());
    assertEquals("\"abc\"", id.annotations().examplesJson().getFirst());
    assertEquals("\"abc\"", id.annotations().defaultJson().orElseThrow());
    assertEquals(Optional.empty(), GeneratedBindingsJsonSchemaMetadata.property("missing"));

    SchemaPropertyMetadata count =
        GeneratedBindingsJsonSchemaMetadata.property("count").orElseThrow();
    assertEquals("Optional<Long>", count.javaType());
    assertEquals("3", count.annotations().defaultJson().orElseThrow());

    SchemaPropertyMetadata tags =
        GeneratedBindingsJsonSchemaMetadata.property("tags").orElseThrow();
    assertEquals("JsonField<List<String>>", tags.javaType());
    assertEquals("[\"red\",\"blue\"]", tags.annotations().examplesJson().getFirst());

    GeneratedBindings value =
        GeneratedBindingsJsonReader.read(
            new JsonStreamReader("{\"id\":\"abc\",\"count\":3,\"tags\":[\"red\"]}"));
    assertValid(GeneratedBindingsJsonValidator.validate(value));
    JsonStringWriter writer = new JsonStringWriter();
    GeneratedBindingsJsonWriter.write(writer, value);
    assertEquals("{\"id\":\"abc\",\"count\":3,\"tags\":[\"red\"]}", writer.json());
    assertEquals(
        new GeneratedBindings("abc", Optional.of(3L), JsonField.value(List.of("red"))), value);
  }

  private static void assertValid(io.github.mundanej.mjjb.runtime.ValidationResult result) {
    if (!result.isValid()) {
      throw new AssertionError("expected valid result but got " + result.errors());
    }
  }

  private static void assertEquals(Object expected, Object actual) {
    if (!expected.equals(actual)) {
      throw new AssertionError("expected " + expected + " but was " + actual);
    }
  }
}
