import io.github.mundanej.mjjb.generated.GeneratedBindings;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonReader;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonValidator;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonWriter;
import io.github.mundanej.mjjb.generator.core.generated.GeneratedSourceBehaviorProbe;
import io.github.mundanej.mjjb.parser.JsonStreamReader;
import io.github.mundanej.mjjb.parser.JsonStringWriter;
import io.github.mundanej.mjjb.runtime.JsonField;
import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.JsonWriteException;
import io.github.mundanej.mjjb.runtime.ValidationResult;
import java.util.List;
import java.util.Map;

public final class BindingBehaviorProbe implements GeneratedSourceBehaviorProbe {
  @Override
  public void run() throws JsonReadException, JsonWriteException {
    GeneratedBindings value =
        GeneratedBindingsJsonReader.read(
            new JsonStreamReader("{\"id\":\"root\",\"b\":null,\"a\":[1.5,2.0]}"));

    assertEquals(
        new GeneratedBindings(
            "root",
            Map.of(
                "a", JsonField.value(List.of(1.5D, 2.0D)),
                "b", JsonField.explicitNull())),
        value);

    JsonStringWriter writer = new JsonStringWriter();
    GeneratedBindingsJsonWriter.write(writer, value);
    assertEquals("{\"id\":\"root\",\"a\":[1.5,2.0],\"b\":null}", writer.json());

    ValidationResult invalid =
        GeneratedBindingsJsonValidator.validate(
            new GeneratedBindings("root", Map.of("bad", JsonField.value(List.of(-1.0D)))));
    assertFalse(invalid.isValid());
    assertEquals("$.bad[0]", invalid.errors().getFirst().path().value());

    List<Double> mutable = new java.util.ArrayList<>(List.of(1.0D));
    GeneratedBindings copied =
        new GeneratedBindings("root", Map.of("copy", JsonField.value(mutable)));
    mutable.add(2.0D);
    assertEquals(List.of(1.0D), copied.additionalProperties().get("copy").requireValue());

    try {
      new GeneratedBindings("root", Map.of("bad", JsonField.<List<Double>>absent()));
      throw new AssertionError("expected absent map entry rejection");
    } catch (IllegalArgumentException expected) {
      // Expected.
    }
  }

  private static void assertEquals(Object expected, Object actual) {
    if (!java.util.Objects.equals(expected, actual)) {
      throw new AssertionError("expected " + expected + " but got " + actual);
    }
  }

  private static void assertFalse(boolean value) {
    if (value) {
      throw new AssertionError("expected false");
    }
  }
}
