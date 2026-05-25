# Release Notes

## 1.1.1

This patch release improves correctness confidence, parser behavior, generated
code performance, and generator maintainability without changing the supported
JSON Schema profile.

- Fixed JSON number parsing so fractional and exponent digits may contain
  leading zeroes while integer leading zeroes remain rejected.
- Added an optional deterministic fuzz test lane for parser behavior and
  generated read, validate, write, and reread behavior.
- Improved Reader-backed parser input handling with chunked fills.
- Improved generated hot paths by emitting reusable `Pattern` and `BigDecimal`
  constants.
- Added focused performance evidence and refreshed SchemaStore corpus evidence.
- Simplified generator internals around binding traversal, Java source text,
  Java name allocation, schema literal reading, constrained `allOf` flattening,
  validator feature detection, and generator test fixtures.

The release remains GitHub-only. Maven artifacts are attached to the `v1.1.1`
GitHub release as a zipped local Maven repository.

## 1.1.0

This release expands the supported JSON Schema Draft 2020-12 profile for
static Java binding generation while preserving the v1 generated-code contract.

- Added nested object property bindings with generated nested records.
- Added same-document `$defs` and local `$ref` resolution.
- Added map bindings for object-valued `additionalProperties`.
- Added constrained `allOf` object flattening.
- Added `patternProperties` map bindings.
- Added generated validation for `minProperties`, `maxProperties`,
  `propertyNames`, and `dependentRequired`.
- Added generated validation for `multipleOf` and scalar-array `uniqueItems`.

The release remains GitHub-only. Maven artifacts are attached to the `v1.1.0`
GitHub release as a zipped local Maven repository.
