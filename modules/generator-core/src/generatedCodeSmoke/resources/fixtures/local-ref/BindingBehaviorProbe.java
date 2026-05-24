import io.github.mundanej.mjjb.generated.GeneratedBindings;
import io.github.mundanej.mjjb.generated.GeneratedBindings.GeneratedBindingsProfile;
import io.github.mundanej.mjjb.generated.GeneratedBindings.GeneratedBindingsProfileAddress;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonReader;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonValidator;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonWriter;
import io.github.mundanej.mjjb.generator.core.generated.GeneratedSourceBehaviorProbe;
import io.github.mundanej.mjjb.parser.JsonStreamReader;
import io.github.mundanej.mjjb.parser.JsonStringWriter;
import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.JsonWriteException;
import io.github.mundanej.mjjb.runtime.ValidationMode;
import io.github.mundanej.mjjb.runtime.ValidationResult;

public final class BindingBehaviorProbe implements GeneratedSourceBehaviorProbe {
  @Override
  public void run() throws JsonReadException, JsonWriteException {
    GeneratedBindings value =
        new GeneratedBindings(
            "id-1",
            new GeneratedBindingsProfile("Ada", new GeneratedBindingsProfileAddress("London")));

    JsonStringWriter writer = new JsonStringWriter();
    GeneratedBindingsJsonWriter.write(writer, value);
    assertEquals(
        "{\"id\":\"id-1\",\"profile\":{\"name\":\"Ada\",\"address\":{\"city\":\"London\"}}}",
        writer.json());
    assertEquals(value, GeneratedBindingsJsonReader.read(new JsonStreamReader(writer.json())));
    assertTrue(GeneratedBindingsJsonValidator.validate(value).isValid());

    ValidationResult diagnostics =
        GeneratedBindingsJsonValidator.validate(
            new GeneratedBindings(
                "", new GeneratedBindingsProfile("Ada", new GeneratedBindingsProfileAddress(""))),
            ValidationMode.ACCUMULATE);
    assertEquals(2, diagnostics.errors().size());
  }

  private static void assertEquals(Object expected, Object actual) {
    if (!java.util.Objects.equals(expected, actual)) {
      throw new AssertionError("expected " + expected + " but got " + actual);
    }
  }

  private static void assertTrue(boolean value) {
    if (!value) {
      throw new AssertionError("expected true");
    }
  }
}
