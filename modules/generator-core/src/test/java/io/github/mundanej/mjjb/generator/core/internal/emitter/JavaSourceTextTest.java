package io.github.mundanej.mjjb.generator.core.internal.emitter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

final class JavaSourceTextTest {
  @Test
  void escapesJavaStringLiteralsExactly() {
    assertEquals("\"plain\"", JavaSourceText.stringLiteral("plain"));
    assertEquals("\"quote\\\"slash\\\\\"", JavaSourceText.stringLiteral("quote\"slash\\"));
    assertEquals("\"\\b\\f\\n\\r\\t\"", JavaSourceText.stringLiteral("\b\f\n\r\t"));
    assertEquals("\"\\u0000\\u001f\"", JavaSourceText.stringLiteral("\u0000\u001f"));
  }

  @Test
  void indentsNonBlankLinesOnly() {
    assertEquals(
        List.of("  alpha", "", "  beta"),
        JavaSourceText.indent(List.of("alpha", "", "beta"), "  "));
  }

  @Test
  void canIndentBlankLinesWhenNeeded() {
    assertEquals(
        List.of("  alpha", "  ", "  beta"),
        JavaSourceText.indentAll(List.of("alpha", "", "beta"), "  "));
  }
}
