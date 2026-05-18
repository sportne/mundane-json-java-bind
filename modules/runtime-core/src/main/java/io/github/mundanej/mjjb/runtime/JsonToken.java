package io.github.mundanej.mjjb.runtime;

/** Streaming JSON token kinds exposed to generated readers. */
public enum JsonToken {
  BEGIN_OBJECT,
  END_OBJECT,
  BEGIN_ARRAY,
  END_ARRAY,
  NAME,
  STRING,
  NUMBER,
  BOOLEAN,
  NULL,
  END_DOCUMENT
}
