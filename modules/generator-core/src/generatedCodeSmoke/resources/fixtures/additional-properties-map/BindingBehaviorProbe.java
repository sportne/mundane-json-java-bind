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
            new JsonStreamReader("{\"z\":\"zz\",\"id\":\"root\",\"a\":\"aa\"}"));

    assertEquals(new GeneratedBindings("root", Map.of("z", "zz", "a", "aa")), value);

    JsonStringWriter writer = new JsonStringWriter();
    GeneratedBindingsJsonWriter.write(writer, value);
    assertEquals("{\"id\":\"root\",\"a\":\"aa\",\"z\":\"zz\"}", writer.json());

    ValidationResult invalid =
        GeneratedBindingsJsonValidator.validate(new GeneratedBindings("root", Map.of("bad", "x")));
    assertFalse(invalid.isValid());
    assertEquals("$.bad", invalid.errors().getFirst().path().value());

    try {
      new GeneratedBindings("root", Map.of("id", "duplicate"));
      throw new AssertionError("expected declared-property collision");
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
