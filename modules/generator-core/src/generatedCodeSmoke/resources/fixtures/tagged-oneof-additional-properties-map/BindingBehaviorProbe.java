import io.github.mundanej.mjjb.generated.GeneratedBindings;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonReader;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonValidator;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonWriter;
import io.github.mundanej.mjjb.generator.core.generated.GeneratedSourceBehaviorProbe;
import io.github.mundanej.mjjb.parser.JsonStreamReader;
import io.github.mundanej.mjjb.parser.JsonStringWriter;
import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.JsonWriteException;
import io.github.mundanej.mjjb.runtime.ValidationResult;
import java.util.Map;

public final class BindingBehaviorProbe implements GeneratedSourceBehaviorProbe {
  @Override
  public void run() throws JsonReadException, JsonWriteException {
    GeneratedBindings value =
        GeneratedBindingsJsonReader.read(
            new JsonStreamReader("{\"kind\":\"card\",\"last4\":\"1234\",\"note\":\"ok\"}"));

    assertEquals(new GeneratedBindings.Card("1234", Map.of("note", "ok")), value);

    JsonStringWriter writer = new JsonStringWriter();
    GeneratedBindingsJsonWriter.write(writer, value);
    assertEquals("{\"kind\":\"card\",\"last4\":\"1234\",\"note\":\"ok\"}", writer.json());

    ValidationResult invalid =
        GeneratedBindingsJsonValidator.validate(
            new GeneratedBindings.Card("1234", Map.of("bad", "x")));
    assertFalse(invalid.isValid());
    assertEquals("$.bad", invalid.errors().getFirst().path().value());

    try {
      new GeneratedBindings.Card("1234", Map.of("kind", "duplicate"));
      throw new AssertionError("expected tag-property collision");
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
