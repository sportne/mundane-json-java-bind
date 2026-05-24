# Release Notes

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
