# Error Reporting

Diagnostics are deterministic value objects. Each diagnostic has a stable code,
a human message, an instance path or schema pointer, and a source location when
the producing layer has one.

## Ownership

Parser diagnostics use `MJJBP-*`. They report malformed JSON and streaming
contract failures from `parser-core`. Parser diagnostics are rooted at `$`
because the parser is schema-agnostic.

Generated reader diagnostics use `MJJBR-*`. They report binding-time failures
while reading raw JSON into generated model instances: root type mismatch,
trailing root content, duplicate properties, unknown properties, missing
required properties, wrong scalar token kinds, invalid integer or number
literals, and non-array values for array fields. Generated readers preserve
parser `MJJBP-*` codes for parse failures, but re-path them to the active field
or array item when that context is known.

Generated validator diagnostics use `MJJBV-*`. They report model-level
validation failures from generated Java values and do not throw for normal
validation failures. Validators report errors in schema property order and
array item errors in ascending index order. `FAIL_FAST` returns the first error
from that same order.

Generator diagnostics use `MJJBG-*`. They report schema syntax, profile,
same-document reference resolution, and binding-analysis failures before source
generation. Generator diagnostics are sorted by schema pointer, code, message,
and manifest line.

## Paths And Pointers

JSON instance paths use the project `JsonPath` form rooted at `$`. Simple
property names use dot notation, such as `$.name`. Property names that are not
simple identifiers use deterministic bracket notation, such as `$["a.b"]` or
`$["quote\"slash\\"]`. Array items append indexes, such as `$.scores[0]`.

Schema locations in generator diagnostics use JSON Pointer strings. Nested
unsupported keywords keep exact pointers, such as `/properties/a/allOf`.
Reference-resolution diagnostics point at the `$ref` value or unsupported
`$ref` sibling that made normalization fail.

## Examples

| Layer | Example code | Example path or pointer | Meaning |
|---|---|---|---|
| Parser | `MJJBP-013` | `$` or re-pathed by a generated reader | Invalid JSON escape. |
| Reader | `MJJBR-003` | `$.name` | Duplicate known JSON property. |
| Reader | `MJJBR-004` | `$["a.b"]` | Unknown JSON property. |
| Reader | `MJJBR-010` | `$.scores` | Expected JSON array for an array field. |
| Validator | `MJJBV-004` | `$.scores[0]` | Non-finite generated number value. |
| Generator | `MJJBG-SCHEMA-UNSUPPORTED-KEYWORD` | `/properties/a/allOf` | Unsupported Draft 2020-12 keyword. |
| Generator | `MJJBG-SCHEMA-MISSING-REF` | `/properties/id/$ref` | Local `$ref` target was not present in the schema document. |

Duplicate property handling is intentionally schema-aware. `parser-core` reads
object member names in source order and exposes locations. Generated readers
track the known property set and reject duplicates with the schema-known
instance path and the current reader location.
