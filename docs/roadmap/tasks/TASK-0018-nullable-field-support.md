# TASK-0018: Nullable Field Support

Task ID: `TASK-0018`
Status: `draft`
Gate: Collections and value constraints
Depends on: `TASK-0017`
Specification references: JSON Schema Draft 2020-12 Core and Validation `type` arrays including `null`
Target modules: `schema-model`, `generator-core`, `runtime-core`

Allowed files:
- `modules/schema-model/**`
- `modules/generator-core/**`
- `modules/runtime-core/**`
- `modules/conformance-tests/**`
- `docs/architecture/generated-code-contract.md`

Forbidden files:
- General union support beyond nullable `type` arrays.
- Untagged `oneOf` support.

Expected behavior:
- Profile accepts nullable `type` arrays that combine exactly one non-null v1 type with `null`.
- Required non-null fields use `T`.
- Optional non-null fields use `Optional<T>`.
- Nullable or absent-vs-null-sensitive fields use `JsonField<T>`.
- Generated readers preserve absent versus explicit null where required by the binding contract.

Tests to add/update:
- Profile tests for nullable accepted and unsupported union rejected cases.
- Golden source tests for nullable generated fields.
- Reader/writer/validator behavior tests for absent, null, and non-null values.

Documentation to update:
- Generated-code contract nullability matrix.
- Runtime `JsonField` usage notes.

Commands to run:
- `./gradlew :modules:runtime-core:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Nullable fields work through generated model, reader, writer, and validator code with documented absent/null semantics.

Rollback notes:
- Revert nullable mapping and generated-code changes.
