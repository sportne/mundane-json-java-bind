package io.github.mundanej.mjjb.examples.taggedoneof;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjjb.examples.taggedoneof.generated.Payment;
import io.github.mundanej.mjjb.runtime.JsonField;
import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.JsonWriteException;
import io.github.mundanej.mjjb.runtime.ValidationResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class TaggedOneOfExampleTest {
  @Test
  void readsValidatesAndWritesCardBranch() throws JsonReadException, JsonWriteException {
    Payment payment =
        TaggedOneOfExample.read(
            "{\"kind\":\"card\",\"labels\":[\"aa\"],\"amount\":10.5,\"last4\":\"1234\"}");

    assertEquals(new Payment.Card("1234", 10.5D, Optional.of(List.of("aa"))), payment);
    assertTrue(TaggedOneOfExample.validate(payment).isValid());
    assertEquals(
        "{\"kind\":\"card\",\"last4\":\"1234\",\"amount\":10.5,\"labels\":[\"aa\"]}",
        TaggedOneOfExample.write(payment));
  }

  @Test
  void readsValidatesAndWritesBankTransferBranch() throws JsonReadException, JsonWriteException {
    Payment payment =
        TaggedOneOfExample.read(
            "{\"kind\":\"bank-transfer\",\"memo\":null,\"urgent\":true,\"iban\":\"DE12345678\"}");

    assertEquals(
        new Payment.BankTransfer("DE12345678", Optional.of(true), JsonField.explicitNull()),
        payment);
    assertTrue(TaggedOneOfExample.validate(payment).isValid());
    assertEquals(
        "{\"kind\":\"bank-transfer\",\"iban\":\"DE12345678\",\"urgent\":true,\"memo\":null}",
        TaggedOneOfExample.write(payment));
  }

  @Test
  void normalizesTaggedOneOfBranches() throws JsonReadException, JsonWriteException {
    assertEquals(
        "{\"kind\":\"bank-transfer\",\"iban\":\"DE12345678\"}",
        TaggedOneOfExample.normalize("{\"kind\":\"bank-transfer\",\"iban\":\"DE12345678\"}"));
  }

  @Test
  void reportsDeterministicReadDiagnostics() {
    assertReadDiagnostic(
        "{\"last4\":\"1234\",\"kind\":\"card\",\"amount\":1}", "MJJBR-005", "$.kind");
    assertReadDiagnostic("{\"kind\":\"cash\"}", "MJJBR-011", "$.kind");
    assertReadDiagnostic(
        "{\"kind\":\"card\",\"last4\":\"1234\",\"amount\":1,\"iban\":\"x\"}",
        "MJJBR-004",
        "$.iban");
  }

  @Test
  void reportsDeterministicValidationDiagnostics() {
    ValidationResult result =
        TaggedOneOfExample.validate(
            new Payment.Card("12", Double.POSITIVE_INFINITY, Optional.of(List.of("x"))));

    assertEquals("MJJBV-007", result.errors().get(0).code());
    assertEquals("$.last4", result.errors().get(0).path().value());
    assertEquals("MJJBV-004", result.errors().get(1).code());
    assertEquals("$.amount", result.errors().get(1).path().value());
    assertEquals("MJJBV-007", result.errors().get(2).code());
    assertEquals("$.labels[0]", result.errors().get(2).path().value());
  }

  private static void assertReadDiagnostic(String json, String code, String path) {
    JsonReadException exception =
        assertThrows(JsonReadException.class, () -> TaggedOneOfExample.read(json));
    assertEquals(code, exception.diagnostic().code());
    assertEquals(path, exception.diagnostic().path().value());
  }
}
