package io.github.mundanej.mjjb.runtime;

/** Project-owned streaming JSON reader interface used by generated readers. */
public interface JsonReader {
  JsonToken peek() throws JsonReadException;

  void beginObject() throws JsonReadException;

  void endObject() throws JsonReadException;

  void beginArray() throws JsonReadException;

  void endArray() throws JsonReadException;

  boolean hasNext() throws JsonReadException;

  String nextName() throws JsonReadException;

  String nextString() throws JsonReadException;

  String nextNumberLiteral() throws JsonReadException;

  boolean nextBoolean() throws JsonReadException;

  void nextNull() throws JsonReadException;

  void skipValue() throws JsonReadException;

  JsonLocation location();

  default JsonField<String> nextNullableString() throws JsonReadException {
    if (peek() == JsonToken.NULL) {
      nextNull();
      return JsonField.explicitNull();
    }
    return JsonField.value(nextString());
  }
}
