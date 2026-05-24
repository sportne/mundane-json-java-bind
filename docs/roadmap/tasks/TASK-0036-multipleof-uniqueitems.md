# TASK-0036: `multipleOf` and `uniqueItems`

Task ID: `TASK-0036`
Status: `complete`
Gate: Post-v1 profile expansion
Depends on: `TASK-0035`
Specification references: JSON Schema Draft 2020-12 Validation keywords `multipleOf` and `uniqueItems`
Target modules: `schema-model`, `generator-core`, `conformance-tests`

Allowed files:
- `modules/schema-model/**`
- `modules/generator-core/**`
- `modules/conformance-tests/**`
- `docs/supported-profile.md`
- `docs/standards-baseline.md`
- `docs/architecture/generated-code-contract.md`
- `docs/architecture/validation-architecture.md`

Forbidden files:
- Object or array deep-equality uniqueness before non-scalar arrays are supported.
- Raw floating-point modulo checks instead of decimal checks over generated Java numeric values.
- Runtime schema interpretation.

Expected behavior:
- `multipleOf` is accepted for `integer` and `number` fields and homogeneous numeric array items.
- Generated validators use decimal arithmetic over generated Java numeric values for `multipleOf` checks and reject invalid schema values during profile validation.
- `uniqueItems` is accepted for homogeneous scalar arrays and validates generated model lists using JSON Schema scalar equality semantics.
- Unsupported deep equality cases remain rejected until object/array literal equality semantics are deliberately designed.

Tests to add/update:
- Profile tests for valid positive `multipleOf` values, invalid non-positive values, and supported `uniqueItems` shapes.
- Golden source and generated behavior probes for scalar numeric fields, numeric array items, and scalar array uniqueness.
- JSON Schema Test Suite trace updates for representative `multipleOf` and `uniqueItems` cases.

Documentation to update:
- Supported profile validation keyword table.
- Validation architecture codes and numeric equality notes.
- Standards baseline supported keyword list after implementation.

Commands to run:
- `./gradlew :modules:schema-model:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`
- `./gradlew schemaStoreCorpus --console=plain`
- `source "$HOME/.sdkman/bin/sdkman-init.sh" && ./gradlew nativeSmoke --console=plain`
- `./gradlew releaseDryRun --console=plain`

Acceptance criteria:
- Generated validators enforce `multipleOf` and scalar-array `uniqueItems` deterministically over generated Java numeric values with stable validation errors.

Rollback notes:
- Revert validator constraints, diagnostics, fixtures, conformance changes, and documentation updates.
