import io.github.mundanej.mjjb.generated.GeneratedBindings;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonReader;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonSchemaMetadata;
import io.github.mundanej.mjjb.generated.GeneratedBindingsJsonValidator;
import io.github.mundanej.mjjb.generator.core.generated.GeneratedSourceBehaviorProbe;
import io.github.mundanej.mjjb.parser.JsonStreamReader;
import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.SchemaBranchMetadata;
import java.util.Optional;

public final class BindingBehaviorProbe implements GeneratedSourceBehaviorProbe {
  @Override
  public void run() throws JsonReadException {
    assertEquals(
        "Payment",
        GeneratedBindingsJsonSchemaMetadata.root()
            .rootObject()
            .annotations()
            .title()
            .orElseThrow());
    assertEquals(Optional.of("kind"), GeneratedBindingsJsonSchemaMetadata.root().tagPropertyName());
    assertEquals(0, GeneratedBindingsJsonSchemaMetadata.properties().size());
    assertEquals(Optional.empty(), GeneratedBindingsJsonSchemaMetadata.property("last4"));
    assertEquals(2, GeneratedBindingsJsonSchemaMetadata.branches().size());

    SchemaBranchMetadata card = GeneratedBindingsJsonSchemaMetadata.branches().getFirst();
    assertEquals("card", card.tagValue());
    assertEquals("Card", card.javaTypeName());
    assertEquals("Card branch", card.object().annotations().title().orElseThrow());
    assertEquals("last4", card.object().properties().getFirst().jsonName());
    assertEquals(
        "\"0000\"",
        card.object().properties().getFirst().annotations().defaultJson().orElseThrow());

    SchemaBranchMetadata bank = GeneratedBindingsJsonSchemaMetadata.branches().get(1);
    assertEquals("bank-transfer", bank.tagValue());
    assertEquals("BankTransfer", bank.javaTypeName());
    assertEquals(
        "false", bank.object().properties().get(1).annotations().defaultJson().orElseThrow());

    GeneratedBindings value =
        GeneratedBindingsJsonReader.read(
            new JsonStreamReader("{\"kind\":\"card\",\"last4\":\"1234\",\"amount\":1.5}"));
    assertValid(GeneratedBindingsJsonValidator.validate(value));
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
