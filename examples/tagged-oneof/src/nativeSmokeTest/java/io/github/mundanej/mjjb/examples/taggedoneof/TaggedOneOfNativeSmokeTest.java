package io.github.mundanej.mjjb.examples.taggedoneof;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjjb.examples.taggedoneof.generated.Payment;
import io.github.mundanej.mjjb.runtime.JsonField;
import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.JsonWriteException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class TaggedOneOfNativeSmokeTest {
  @Test
  void exercisesGeneratedTaggedOneOfBranches() throws JsonReadException, JsonWriteException {
    Payment card =
        TaggedOneOfExample.read(
            "{\"kind\":\"card\",\"labels\":[\"aa\"],\"amount\":10.5,\"last4\":\"1234\"}");
    Payment bankTransfer =
        TaggedOneOfExample.read(
            "{\"kind\":\"bank-transfer\",\"memo\":null,\"urgent\":true,\"iban\":\"DE12345678\"}");

    assertEquals(new Payment.Card("1234", 10.5D, Optional.of(List.of("aa"))), card);
    assertEquals(
        new Payment.BankTransfer("DE12345678", Optional.of(true), JsonField.explicitNull()),
        bankTransfer);
    assertTrue(TaggedOneOfExample.validate(card).isValid());
    assertTrue(TaggedOneOfExample.validate(bankTransfer).isValid());
    assertEquals(
        "{\"kind\":\"card\",\"last4\":\"1234\",\"amount\":10.5,\"labels\":[\"aa\"]}",
        TaggedOneOfExample.write(card));
    assertEquals(
        "{\"kind\":\"bank-transfer\",\"iban\":\"DE12345678\",\"urgent\":true,\"memo\":null}",
        TaggedOneOfExample.write(bankTransfer));
  }

  @Test
  void reportsGeneratedTagDiagnostic() {
    JsonReadException exception =
        assertThrows(JsonReadException.class, () -> TaggedOneOfExample.read("{\"kind\":\"cash\"}"));

    assertEquals("MJJBR-011", exception.diagnostic().code());
    assertEquals("$.kind", exception.diagnostic().path().value());
  }
}
