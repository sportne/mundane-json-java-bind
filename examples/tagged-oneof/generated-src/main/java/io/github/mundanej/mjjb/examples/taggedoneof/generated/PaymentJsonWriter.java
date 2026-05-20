package io.github.mundanej.mjjb.examples.taggedoneof.generated;

import io.github.mundanej.mjjb.runtime.JsonWriteException;
import io.github.mundanej.mjjb.runtime.JsonWriter;
import java.util.Objects;

public final class PaymentJsonWriter {
  private PaymentJsonWriter() {}

  public static void write(JsonWriter writer, Payment value) throws JsonWriteException {
    Objects.requireNonNull(writer, "writer");
    Objects.requireNonNull(value, "value");
    if (value instanceof Payment.Card branch) {
      writeCard(writer, branch);
      return;
    }
    if (value instanceof Payment.BankTransfer branch) {
      writeBankTransfer(writer, branch);
      return;
    }
    throw new JsonWriteException("Unsupported tagged oneOf branch: " + value.getClass().getName());
  }

  private static void writeCard(JsonWriter writer, Payment.Card value) throws JsonWriteException {
    requireFinite(value.amount(), "amount");
    writer.beginObject();
    writer.name("kind");
    writer.value("card");
    writer.name("last4");
    writer.value(value.last4());
    writer.name("amount");
    writer.number(Double.toString(value.amount()));
    if (value.labels().isPresent()) {
      writer.name("labels");
      writer.beginArray();
      for (String item : value.labels().orElseThrow()) {
        writer.value(item);
      }
      writer.endArray();
    }
    writer.endObject();
  }

  private static void writeBankTransfer(JsonWriter writer, Payment.BankTransfer value) throws JsonWriteException {
    writer.beginObject();
    writer.name("kind");
    writer.value("bank-transfer");
    writer.name("iban");
    writer.value(value.iban());
    if (value.urgent().isPresent()) {
      writer.name("urgent");
      writer.value(value.urgent().orElseThrow());
    }
    if (!value.memo().isAbsent()) {
      writer.name("memo");
      if (value.memo().isExplicitNull()) {
        writer.nullValue();
      } else {
        writer.value(value.memo().requireValue());
      }
    }
    writer.endObject();
  }

  private static void requireFinite(double value, String name) throws JsonWriteException {
    if (!Double.isFinite(value)) {
      throw new JsonWriteException("JSON number field '" + name + "' must be finite.");
    }
  }
}
