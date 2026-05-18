import io.github.mundanej.mjjb.generated.GeneratedBindings;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonWriter;
import io.github.mundanej.mjjb.generator.core.generated.GeneratedSourceBehaviorProbe;
import io.github.mundanej.mjjb.parser.JsonStringWriter;
import io.github.mundanej.mjjb.runtime.JsonWriteException;
import java.util.Optional;

public final class WriterBehaviorProbe implements GeneratedSourceBehaviorProbe {
  @Override
  public void run() throws JsonWriteException {
    JsonStringWriter writer = new JsonStringWriter();
    GeneratedBindingsJsonWriter.write(
        writer, new GeneratedBindings(Optional.of("last"), Optional.of(false), 9L));
    assertJson("{\"zeta\":\"last\",\"alpha\":false,\"middle\":9}", writer.json());
  }

  private static void assertJson(String expected, String actual) {
    if (!expected.equals(actual)) {
      throw new AssertionError("expected " + expected + " but was " + actual);
    }
  }
}
