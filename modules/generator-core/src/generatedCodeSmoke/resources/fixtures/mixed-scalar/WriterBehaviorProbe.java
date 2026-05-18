import io.github.mundanej.mjjb.generated.GeneratedBindings;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonWriter;
import io.github.mundanej.mjjb.generator.core.generated.GeneratedSourceBehaviorProbe;
import io.github.mundanej.mjjb.parser.JsonStringWriter;
import io.github.mundanej.mjjb.runtime.JsonWriteException;
import java.util.Optional;

public final class WriterBehaviorProbe implements GeneratedSourceBehaviorProbe {
  @Override
  public void run() throws JsonWriteException {
    JsonStringWriter first = new JsonStringWriter();
    GeneratedBindingsJsonWriter.write(
        first,
        new GeneratedBindings("id-1", 7L, Optional.of("Ada"), Optional.of(3.5D), Optional.empty()));
    assertJson("{\"id\":\"id-1\",\"count\":7,\"displayName\":\"Ada\",\"score\":3.5}", first.json());

    JsonStringWriter second = new JsonStringWriter();
    GeneratedBindingsJsonWriter.write(
        second,
        new GeneratedBindings("id-2", 8L, Optional.empty(), Optional.empty(), Optional.of(true)));
    assertJson("{\"id\":\"id-2\",\"count\":8,\"active\":true}", second.json());

    assertNonFiniteNumberRejected();
  }

  private static void assertNonFiniteNumberRejected() {
    try {
      GeneratedBindingsJsonWriter.write(
          new JsonStringWriter(),
          new GeneratedBindings(
              "bad", 1L, Optional.empty(), Optional.of(Double.NaN), Optional.empty()));
      throw new AssertionError("expected non-finite score to be rejected");
    } catch (JsonWriteException expected) {
      if (!expected.getMessage().contains("score")) {
        throw new AssertionError("expected score in message but was " + expected.getMessage());
      }
    }
  }

  private static void assertJson(String expected, String actual) {
    if (!expected.equals(actual)) {
      throw new AssertionError("expected " + expected + " but was " + actual);
    }
  }
}
