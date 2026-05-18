# Parser Architecture

The v1 parser is custom and dependency-free.

`parser-core` provides a streaming tokenizer/parser and writer backed by
project-owned runtime interfaces. It tracks byte/character location data for
diagnostics and does not build a generic object mapper graph as the normal
generated-binding path.

Generated readers call explicit token methods, switch on property names, and
construct generated model records directly.

## Supported Grammar

`JsonStreamReader` accepts RFC 8259 JSON values needed by generated readers:

- objects with string member names, colon separators, and comma separators;
- arrays with comma-separated values;
- strings with valid JSON escapes, unicode escapes, and no unescaped control
  characters;
- number literals with optional sign, fraction, and exponent parts;
- `true`, `false`, and `null` literals;
- JSON whitespace around tokens.

The parser rejects malformed container syntax, trailing commas, invalid
strings, invalid numbers, and invalid literals with stable `MJJBP-*`
diagnostic codes.

## Reader Contract

The reader exposes explicit token methods through the runtime `JsonReader`
interface. It does not create a generic object graph and it does not perform
schema-aware validation.

Full-document validation is a caller contract: generated readers parse the root
value, then require `peek()` to return `END_DOCUMENT`. If more input remains,
`peek()` returns the next token or reports an unexpected token diagnostic.

Duplicate property rejection is also a generated-reader responsibility. The
parser reads object member names in source order; generated readers can track
the schema-known property set and reject duplicates with schema-specific paths.

## Streaming Behavior

String-backed readers use the provided input directly. Reader-backed parsing is
incremental and does not drain the `Reader` at construction time.

The current implementation buffers consumed characters so it can expose stable
offsets and simple literal slicing. It still streams from the caller's
`Reader`, avoids generic object construction, and supports large arrays through
normal `hasNext()` loops.

## Diagnostics

Parser failures throw `JsonReadException` with a `JsonDiagnostic` containing:

- a stable parser code beginning with `MJJBP-`;
- a deterministic message;
- the current best-effort `JsonLocation`;
- `JsonPath.ROOT` until generated readers add schema-aware instance paths.

Locations are character offsets with one-based line and column coordinates.

## Writer Contract

`JsonStringWriter` is the dependency-free compact JSON writer used by generated
writers. It writes one complete root value, rejects duplicate root values, and
allows `json()` only after the document is complete.

The writer preserves object property order exactly as callers provide names.
Generated writers own deterministic schema property ordering.

String escaping is deterministic: JSON short escapes are used for `"`, `\`,
backspace, form feed, newline, carriage return, and tab; other control
characters are emitted as lowercase unicode escapes; non-control unicode
characters are left unchanged.

Number literals are syntactically checked against the JSON number grammar before
they are appended. Semantic numeric range checks belong to generated
validators, not the writer.
