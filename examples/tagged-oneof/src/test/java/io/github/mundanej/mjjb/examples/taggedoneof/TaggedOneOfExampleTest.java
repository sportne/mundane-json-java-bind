package io.github.mundanej.mjjb.examples.taggedoneof;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjjb.examples.taggedoneof.generated.Payment;
import io.github.mundanej.mjjb.examples.taggedoneof.generated.PaymentJsonValidator;
import io.github.mundanej.mjjb.examples.taggedoneof.generated.PaymentJsonWriter;
import io.github.mundanej.mjjb.parser.JsonStringWriter;
import io.github.mundanej.mjjb.runtime.JsonField;
import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.JsonWriteException;
import io.github.mundanej.mjjb.runtime.ValidationMode;
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
  void preservesNullableAbsentNullAndValueSemantics() throws JsonReadException, JsonWriteException {
    Payment absent =
        TaggedOneOfExample.read("{\"kind\":\"bank-transfer\",\"iban\":\"DE12345678\"}");
    assertEquals(
        new Payment.BankTransfer("DE12345678", Optional.empty(), JsonField.absent()), absent);
    assertTrue(TaggedOneOfExample.validate(absent).isValid());
    assertEquals(
        "{\"kind\":\"bank-transfer\",\"iban\":\"DE12345678\"}", TaggedOneOfExample.write(absent));

    Payment explicitNull =
        TaggedOneOfExample.read(
            "{\"kind\":\"bank-transfer\",\"iban\":\"DE12345678\",\"memo\":null}");
    assertEquals(
        new Payment.BankTransfer("DE12345678", Optional.empty(), JsonField.explicitNull()),
        explicitNull);
    assertTrue(TaggedOneOfExample.validate(explicitNull).isValid());
    assertEquals(
        "{\"kind\":\"bank-transfer\",\"iban\":\"DE12345678\",\"memo\":null}",
        TaggedOneOfExample.write(explicitNull));

    Payment value =
        TaggedOneOfExample.read(
            "{\"kind\":\"bank-transfer\",\"iban\":\"DE12345678\",\"memo\":\"payroll\"}");
    assertEquals(
        new Payment.BankTransfer("DE12345678", Optional.empty(), JsonField.value("payroll")),
        value);
    assertTrue(TaggedOneOfExample.validate(value).isValid());
    assertEquals(
        "{\"kind\":\"bank-transfer\",\"iban\":\"DE12345678\",\"memo\":\"payroll\"}",
        TaggedOneOfExample.write(value));
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
  void reportsDuplicateTrailingAndRootTypeReadDiagnostics() {
    assertReadDiagnostic(
        "{\"kind\":\"card\",\"kind\":\"card\",\"last4\":\"1234\",\"amount\":1}",
        "MJJBR-003",
        "$.kind");
    assertReadDiagnostic(
        "{\"kind\":\"card\",\"last4\":\"1234\",\"amount\":1,\"amount\":2}",
        "MJJBR-003",
        "$.amount");
    assertReadDiagnostic(
        "{\"kind\":\"bank-transfer\",\"iban\":\"DE12345678\"} true", "MJJBR-002", "$");
    assertReadDiagnostic("[]", "MJJBR-001", "$");
  }

  @Test
  void reportsUnknownAndLateTagDiagnosticsWithStableMessages() {
    assertReadDiagnostic(
        "{\"kind\":\"cash\"}", "MJJBR-011", "$.kind", "Unknown tagged oneOf value 'cash'.");
    assertReadDiagnostic(
        "{\"last4\":\"1234\",\"amount\":1,\"kind\":\"card\"}",
        "MJJBR-005",
        "$.kind",
        "Missing required JSON property 'kind'.");
  }

  @Test
  void writersDispatchBranchesAndHonorAbsentContainers() throws JsonWriteException {
    JsonStringWriter cardWriter = new JsonStringWriter();
    PaymentJsonWriter.write(cardWriter, new Payment.Card("1234", 1.0D, Optional.empty()));
    assertEquals("{\"kind\":\"card\",\"last4\":\"1234\",\"amount\":1.0}", cardWriter.json());

    JsonStringWriter bankWriter = new JsonStringWriter();
    PaymentJsonWriter.write(
        bankWriter, new Payment.BankTransfer("DE12345678", Optional.empty(), JsonField.absent()));
    assertEquals("{\"kind\":\"bank-transfer\",\"iban\":\"DE12345678\"}", bankWriter.json());
  }

  @Test
  void writerRejectsNullInputsAndModelConstructorsRejectNullContainers() {
    assertThrows(
        NullPointerException.class,
        () ->
            PaymentJsonWriter.write(
                null,
                new Payment.BankTransfer("DE12345678", Optional.empty(), JsonField.absent())));
    assertThrows(
        NullPointerException.class, () -> PaymentJsonWriter.write(new JsonStringWriter(), null));
    assertThrows(NullPointerException.class, () -> new Payment.Card("1234", 1.0D, null));
    assertThrows(
        NullPointerException.class,
        () -> new Payment.BankTransfer("DE12345678", null, JsonField.absent()));
    assertThrows(
        NullPointerException.class,
        () -> new Payment.BankTransfer("DE12345678", Optional.empty(), null));
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

  @Test
  void validationCanAccumulateOrFailFast() {
    Payment invalid = new Payment.Card("12", Double.NaN, Optional.of(List.of("x")));

    ValidationResult accumulated = PaymentJsonValidator.validate(invalid);
    assertValidationDiagnostics(
        accumulated,
        List.of("MJJBV-007", "MJJBV-004", "MJJBV-007"),
        List.of("$.last4", "$.amount", "$.labels[0]"));

    ValidationResult failFast = PaymentJsonValidator.validate(invalid, ValidationMode.FAIL_FAST);
    assertValidationDiagnostics(failFast, List.of("MJJBV-007"), List.of("$.last4"));
  }

  @Test
  void validationReportsNullableEnumFailures() {
    ValidationResult result =
        TaggedOneOfExample.validate(
            new Payment.BankTransfer(
                "DE12345678", Optional.empty(), JsonField.value("not-payroll")));

    assertValidationDiagnostics(result, List.of("MJJBV-015"), List.of("$.memo"));
  }

  @Test
  void validationCoversRemainingCardAndBankTransferConstraints() {
    ValidationResult cardResult =
        PaymentJsonValidator.validate(new Payment.Card("12345", -1.0D, Optional.of(List.of())));
    assertValidationDiagnostics(
        cardResult,
        List.of("MJJBV-008", "MJJBV-011", "MJJBV-005"),
        List.of("$.last4", "$.amount", "$.labels"));

    ValidationResult bankTransferResult =
        PaymentJsonValidator.validate(
            new Payment.BankTransfer("short", Optional.empty(), JsonField.explicitNull()));
    assertValidationDiagnostics(bankTransferResult, List.of("MJJBV-007"), List.of("$.iban"));

    ValidationResult nullRootResult = PaymentJsonValidator.validate(null);
    assertValidationDiagnostics(nullRootResult, List.of("MJJBV-001"), List.of("$"));
  }

  private static void assertReadDiagnostic(String json, String code, String path) {
    JsonReadException exception =
        assertThrows(JsonReadException.class, () -> TaggedOneOfExample.read(json));
    assertEquals(code, exception.diagnostic().code());
    assertEquals(path, exception.diagnostic().path().value());
  }

  private static void assertReadDiagnostic(String json, String code, String path, String message) {
    JsonReadException exception =
        assertThrows(JsonReadException.class, () -> TaggedOneOfExample.read(json));
    assertEquals(code, exception.diagnostic().code());
    assertEquals(path, exception.diagnostic().path().value());
    assertEquals(message, exception.diagnostic().message());
  }

  private static void assertValidationDiagnostics(
      ValidationResult result, List<String> codes, List<String> paths) {
    assertFalse(result.isValid());
    assertEquals(codes, result.errors().stream().map(error -> error.code()).toList());
    assertEquals(paths, result.errors().stream().map(error -> error.path().value()).toList());
  }
}
