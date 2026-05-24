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
            new JsonStreamReader(
                "{\"id\":\"root\",\"b\":{\"code\":\"bb\"},\"a\":{\"code\":\"aa\"}}"));

    assertEquals(
        new GeneratedBindings(
            "root",
            Map.of(
                "a", new GeneratedBindings.GeneratedBindingsAdditionalProperty("aa"),
                "b", new GeneratedBindings.GeneratedBindingsAdditionalProperty("bb"))),
        value);

    JsonStringWriter writer = new JsonStringWriter();
    GeneratedBindingsJsonWriter.write(writer, value);
    assertEquals(
        "{\"id\":\"root\",\"a\":{\"code\":\"aa\"},\"b\":{\"code\":\"bb\"}}", writer.json());

    ValidationResult invalid =
        GeneratedBindingsJsonValidator.validate(
            new GeneratedBindings(
                "root",
                Map.of("bad", new GeneratedBindings.GeneratedBindingsAdditionalProperty("x"))));
    assertFalse(invalid.isValid());
    assertEquals("$.bad.code", invalid.errors().getFirst().path().value());
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
