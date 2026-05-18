package io.github.mundanej.mjjb.examples.basicrecord;

import io.github.mundanej.mjjb.examples.basicrecord.generated.BasicRecord;
import io.github.mundanej.mjjb.examples.basicrecord.generated.BasicRecordJsonReader;
import io.github.mundanej.mjjb.examples.basicrecord.generated.BasicRecordJsonValidator;
import io.github.mundanej.mjjb.examples.basicrecord.generated.BasicRecordJsonWriter;
import io.github.mundanej.mjjb.parser.JsonStreamReader;
import io.github.mundanej.mjjb.parser.JsonStringWriter;
import io.github.mundanej.mjjb.runtime.JsonReadException;
import io.github.mundanej.mjjb.runtime.JsonWriteException;
import io.github.mundanej.mjjb.runtime.ValidationResult;

/** Minimal executable example for the basic object binding slice. */
public final class BasicRecordExample {
  private BasicRecordExample() {}

  public static BasicRecord read(String json) throws JsonReadException {
    return BasicRecordJsonReader.read(new JsonStreamReader(json));
  }

  public static ValidationResult validate(BasicRecord value) {
    return BasicRecordJsonValidator.validate(value);
  }

  public static String write(BasicRecord value) throws JsonWriteException {
    JsonStringWriter writer = new JsonStringWriter();
    BasicRecordJsonWriter.write(writer, value);
    return writer.json();
  }

  public static String normalize(String json) throws JsonReadException, JsonWriteException {
    BasicRecord value = read(json);
    ValidationResult result = validate(value);
    if (!result.isValid()) {
      throw new IllegalStateException("generated binding validation failed: " + result.errors());
    }
    return write(value);
  }
}
