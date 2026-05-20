package io.github.mundanej.mjjb.examples.taggedoneof;

import io.github.mundanej.mjjb.examples.taggedoneof.generated.Payment;
import io.github.mundanej.mjjb.examples.taggedoneof.generated.PaymentJsonReader;
import io.github.mundanej.mjjb.examples.taggedoneof.generated.PaymentJsonValidator;
import io.github.mundanej.mjjb.examples.taggedoneof.generated.PaymentJsonWriter;
import io.github.mundanej.mjjb.parser.JsonStreamReader;
import io.github.mundanej.mjjb.parser.JsonStringWriter;
import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.JsonWriteException;
import io.github.mundanej.mjjb.runtime.ValidationResult;

/** Minimal executable example for the tagged oneOf binding slice. */
public final class TaggedOneOfExample {
  private TaggedOneOfExample() {}

  public static Payment read(String json) throws JsonReadException {
    return PaymentJsonReader.read(new JsonStreamReader(json));
  }

  public static ValidationResult validate(Payment value) {
    return PaymentJsonValidator.validate(value);
  }

  public static String write(Payment value) throws JsonWriteException {
    JsonStringWriter writer = new JsonStringWriter();
    PaymentJsonWriter.write(writer, value);
    return writer.json();
  }

  public static String normalize(String json) throws JsonReadException, JsonWriteException {
    Payment value = read(json);
    ValidationResult result = validate(value);
    if (!result.isValid()) {
      throw new IllegalStateException("generated binding validation failed: " + result.errors());
    }
    return write(value);
  }
}
