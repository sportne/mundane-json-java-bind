# Validation Architecture

Generated validators implement accepted Draft 2020-12 validation keyword
semantics directly in Java source.

Validation failures are returned as values through `ValidationResult`.
Parse/read failures use `JsonReadException`. Normal validation flow must not be
exception-driven.

Each validation error carries a stable code, human message, JSON instance path,
optional JSON location, and schema location where available.

The first generated-validator slice validates generated model instances rather
than raw JSON input. Generated readers own parse and type diagnostics for raw
JSON. Generated validators own model-level invariants that can still be broken
by direct Java construction, reflection, or future generated shapes.

Generated validators expose an accumulating default mode and an explicit
fail-fast mode. Both modes use `ValidationErrors`; generated code must stop
after the first failed `add` call in fail-fast mode and return all accumulated
errors in accumulate mode.

Array validators enforce collection constraints on generated model lists.
`minItems` and `maxItems` failures are reported at the array field path with
stable generated-validator codes `MJJBV-005` and `MJJBV-006`. Validation that
targets an array item, such as non-finite `number` items, reports the indexed
instance path, for example `$.scores[0]`.

## Supported Facets

Generated validators enforce scalar facets on scalar fields and homogeneous
array items. Facet failures use the field path for scalar fields and indexed
item paths for array items.

| Keyword | Applies to | Code | Notes |
|---|---|---|---|
| `minLength` | `string` | `MJJBV-007` | Counts Unicode code points. |
| `maxLength` | `string` | `MJJBV-008` | Counts Unicode code points. |
| `pattern` | `string` | `MJJBV-009` | Uses deterministic generated `Pattern` checks and JSON Schema search semantics. |
| `format` | `string` | `MJJBV-010` | Supports only `date`, `date-time`, and `uuid`. |
| `minimum` | `integer`, `number` | `MJJBV-011` | Compares generated model values against exact schema literals with `BigDecimal`. |
| `maximum` | `integer`, `number` | `MJJBV-012` | Compares generated model values against exact schema literals with `BigDecimal`. |
| `exclusiveMinimum` | `integer`, `number` | `MJJBV-013` | Requires model value greater than the schema literal. |
| `exclusiveMaximum` | `integer`, `number` | `MJJBV-014` | Requires model value less than the schema literal. |
