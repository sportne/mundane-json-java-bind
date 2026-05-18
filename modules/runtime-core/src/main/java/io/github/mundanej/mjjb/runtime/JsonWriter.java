package io.github.mundanej.mjjb.runtime;

/** Project-owned streaming JSON writer interface used by generated writers. */
public interface JsonWriter {
  void beginObject() throws JsonWriteException;

  void endObject() throws JsonWriteException;

  void beginArray() throws JsonWriteException;

  void endArray() throws JsonWriteException;

  void name(String name) throws JsonWriteException;

  void value(String value) throws JsonWriteException;

  void number(String literal) throws JsonWriteException;

  void value(boolean value) throws JsonWriteException;

  void nullValue() throws JsonWriteException;
}
