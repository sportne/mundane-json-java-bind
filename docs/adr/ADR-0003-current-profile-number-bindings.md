# ADR-0003: Current Profile Number Bindings

## Status

Accepted.

## Context

JSON Schema numeric validation is defined over JSON numbers, not over a single
host-language numeric type. The current generated-binding profile must choose a
Java representation for `integer` and `number` fields while preserving the
project goals of simple generated source, predictable Native Image behavior,
and no runtime schema interpretation.

The project already maps `integer` to `long` and `number` to `double`.
`TASK-0036` added `multipleOf` and scalar-array `uniqueItems`, which made the
precision tradeoff more visible because generated validators must compare Java
model values against schema numeric literals.

## Decision

For `JSP-DATA-2020-12`, `integer` remains `long` and `number` remains finite
Java `double`.

Generated validators compare numeric constraints by converting generated Java
numeric values to `BigDecimal` and comparing them with the exact schema literal
captured during generation. This gives deterministic generated validation over
the current Java model shape, but it does not preserve arbitrary JSON decimal
precision for `number` values after reader parsing or direct Java
construction.

An exact-decimal binding mode is not part of the current profile. If added, it
should be introduced as an explicit future profile or opt-in generator mode with
separate generated-code contract, performance evidence, Native Image evidence,
and conformance trace updates.

## Consequences

- Generated model source stays small and idiomatic for the common Java binding
  case: `long`, `double`, `Optional<Long>`, `Optional<Double>`, and scalar
  lists.
- Readers and writers keep a simple finite-number contract: parse JSON numbers
  into `double`, reject non-finite values, and write finite `double` values with
  `Double.toString`.
- Integer bindings retain integral precision within Java `long` range.
- Number bindings do not support arbitrary-precision JSON decimals or preserve
  decimal lexical form.
- JSON Schema Test Suite cases that require broader numeric equivalence remain
  explicitly skipped as numeric-semantics scope, not silently treated as fully
  supported.
- Future exact-decimal support remains possible, but it must justify the added
  generated API surface, construction friction, validation cost, and Native
  Image impact.
