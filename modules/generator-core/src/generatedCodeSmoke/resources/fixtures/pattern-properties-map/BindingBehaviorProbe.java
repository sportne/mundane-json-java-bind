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
            new JsonStreamReader("{\"x-b\":2,\"id\":\"root\",\"other\":\"ok\",\"x-a\":1}"));

    assertEquals(
        new GeneratedBindings("root", Map.of("x-a", 1L, "x-b", 2L), Map.of("other", "ok")), value);

    JsonStringWriter writer = new JsonStringWriter();
    GeneratedBindingsJsonWriter.write(writer, value);
    assertEquals("{\"id\":\"root\",\"x-a\":1,\"x-b\":2,\"other\":\"ok\"}", writer.json());

    ValidationResult invalidPattern =
        GeneratedBindingsJsonValidator.validate(
            new GeneratedBindings("root", Map.of("x-bad", 0L), Map.of()));
    assertFalse(invalidPattern.isValid());
    assertEquals("$[\"x-bad\"]", invalidPattern.errors().getFirst().path().value());

    ValidationResult invalidAdditional =
        GeneratedBindingsJsonValidator.validate(
            new GeneratedBindings("root", Map.of(), Map.of("other", "x")));
    assertFalse(invalidAdditional.isValid());
    assertEquals("$.other", invalidAdditional.errors().getFirst().path().value());

    try {
      new GeneratedBindings("root", Map.of("bad", 1L), Map.of());
      throw new AssertionError("expected pattern key rejection");
    } catch (IllegalArgumentException expected) {
      // Expected.
    }

    try {
      new GeneratedBindings("root", Map.of(), Map.of("x-c", "pattern key"));
      throw new AssertionError("expected additional map pattern-key rejection");
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
