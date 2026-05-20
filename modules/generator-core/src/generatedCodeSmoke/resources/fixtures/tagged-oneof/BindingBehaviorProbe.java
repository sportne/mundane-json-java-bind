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
import io.github.mundanej.mjjb.runtime.ValidationMode;
import io.github.mundanej.mjjb.runtime.ValidationResult;
import java.util.List;
import java.util.Optional;

public final class BindingBehaviorProbe implements GeneratedSourceBehaviorProbe {
  @Override
  public void run() throws JsonReadException, JsonWriteException {
    GeneratedBindings card =
        GeneratedBindingsJsonReader.read(
            new JsonStreamReader(
                "{\"kind\":\"card\",\"labels\":[\"aa\"],\"amount\":10.5,\"last4\":\"1234\"}"));
    assertBinding(new GeneratedBindings.Card("1234", 10.5D, Optional.of(List.of("aa"))), card);

    GeneratedBindings bankTransfer =
        GeneratedBindingsJsonReader.read(
            new JsonStreamReader(
                "{\"kind\":\"bank-transfer\",\"memo\":null,\"urgent\":true,\"iban\":\"DE12345678\"}"));
    assertBinding(
        new GeneratedBindings.BankTransfer(
            "DE12345678", Optional.of(true), JsonField.explicitNull()),
        bankTransfer);

    JsonStringWriter cardWriter = new JsonStringWriter();
    GeneratedBindingsJsonWriter.write(cardWriter, card);
    assertJson(
        "{\"kind\":\"card\",\"last4\":\"1234\",\"amount\":10.5,\"labels\":[\"aa\"]}",
        cardWriter.json());

    JsonStringWriter bankWriter = new JsonStringWriter();
    GeneratedBindingsJsonWriter.write(bankWriter, bankTransfer);
    assertJson(
        "{\"kind\":\"bank-transfer\",\"iban\":\"DE12345678\",\"urgent\":true,\"memo\":null}",
        bankWriter.json());

    assertValid(GeneratedBindingsJsonValidator.validate(card));
    assertValid(GeneratedBindingsJsonValidator.validate(bankTransfer));
    assertValidationOrdering();
    assertReadFailures();
  }

  private static void assertValidationOrdering() {
    GeneratedBindings invalid =
        new GeneratedBindings.Card("12", Double.NaN, Optional.of(List.of("x")));

    ValidationResult accumulated = GeneratedBindingsJsonValidator.validate(invalid);
    assertInvalidCodes(accumulated, "MJJBV-007", "MJJBV-004", "MJJBV-007");
    assertInvalidPaths(accumulated, "$.last4", "$.amount", "$.labels[0]");

    ValidationResult failFast =
        GeneratedBindingsJsonValidator.validate(invalid, ValidationMode.FAIL_FAST);
    assertInvalidCodes(failFast, "MJJBV-007");
    assertInvalidPaths(failFast, "$.last4");
  }

  private static void assertReadFailures() {
    assertReadFailure("{}", "MJJBR-005", "$.kind");
    assertReadFailure("{\"last4\":\"1234\",\"kind\":\"card\",\"amount\":1}", "MJJBR-005", "$.kind");
    assertReadFailure(
        "{\"kind\":\"card\",\"kind\":\"card\",\"last4\":\"1234\",\"amount\":1}",
        "MJJBR-003",
        "$.kind");
    assertReadFailure("{\"kind\":1}", "MJJBR-006", "$.kind");
    assertReadFailure("{\"kind\":\"cash\"}", "MJJBR-011", "$.kind");
    assertReadFailure(
        "{\"kind\":\"card\",\"last4\":\"1234\",\"amount\":1,\"iban\":\"x\"}",
        "MJJBR-004",
        "$.iban");
    assertReadFailure("{\"kind\":\"card\",\"last4\":\"1234\"}", "MJJBR-005", "$.amount");
    assertReadFailure("{\"kind\":\"bank-transfer\",\"iban\":true}", "MJJBR-006", "$.iban");
    assertReadFailure(
        "{\"kind\":\"card\",\"last4\":\"1234\",\"amount\":1,\"labels\":true}",
        "MJJBR-010",
        "$.labels");
    assertReadFailure(
        "{\"kind\":\"card\",\"last4\":\"1234\",\"amount\":1,\"labels\":[1]}",
        "MJJBR-006",
        "$.labels[0]");
    assertReadFailure(
        "{\"kind\":\"bank-transfer\",\"iban\":\"DE12345678\"} true", "MJJBR-002", "$");
  }

  private static void assertReadFailure(String json, String code, String path) {
    try {
      GeneratedBindingsJsonReader.read(new JsonStreamReader(json));
      throw new AssertionError("expected read failure " + code);
    } catch (JsonReadException expected) {
      if (!code.equals(expected.diagnostic().code())) {
        throw new AssertionError("expected code " + code + " but was " + expected.diagnostic());
      }
      if (!path.equals(expected.diagnostic().path().value())) {
        throw new AssertionError("expected path " + path + " but was " + expected.diagnostic());
      }
    }
  }

  private static void assertValid(ValidationResult result) {
    if (!result.isValid()) {
      throw new AssertionError("expected valid result but got " + result.errors());
    }
  }

  private static void assertInvalidCodes(ValidationResult result, String... expectedCodes) {
    if (result.isValid()) {
      throw new AssertionError("expected invalid validation result");
    }
    if (result.errors().size() != expectedCodes.length) {
      throw new AssertionError(
          "expected " + expectedCodes.length + " errors but got " + result.errors());
    }
    for (int index = 0; index < expectedCodes.length; index++) {
      if (!expectedCodes[index].equals(result.errors().get(index).code())) {
        throw new AssertionError("unexpected validation errors " + result.errors());
      }
    }
  }

  private static void assertInvalidPaths(ValidationResult result, String... expectedPaths) {
    for (int index = 0; index < expectedPaths.length; index++) {
      if (!expectedPaths[index].equals(result.errors().get(index).path().value())) {
        throw new AssertionError("unexpected validation paths " + result.errors());
      }
    }
  }

  private static void assertBinding(GeneratedBindings expected, GeneratedBindings actual) {
    if (!expected.equals(actual)) {
      throw new AssertionError("expected " + expected + " but was " + actual);
    }
  }

  private static void assertJson(String expected, String actual) {
    if (!expected.equals(actual)) {
      throw new AssertionError("expected " + expected + " but was " + actual);
    }
  }
}
