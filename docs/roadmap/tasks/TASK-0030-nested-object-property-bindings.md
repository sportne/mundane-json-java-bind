# TASK-0030: Nested Object Property Bindings

Task ID: `TASK-0030`
Status: `complete`
Gate: Post-v1 profile expansion
Depends on: `TASK-0029`
Specification references: JSON Schema Draft 2020-12 object `type`, `properties`, `required`, and `additionalProperties`
Target modules: `schema-model`, `generator-core`, `runtime-core`, `conformance-tests`

Allowed files:
- `modules/schema-model/**`
- `modules/generator-core/**`
- `modules/runtime-core/**`
- `modules/conformance-tests/**`
- `docs/supported-profile.md`
- `docs/standards-baseline.md`
- `docs/architecture/generated-code-contract.md`
- `docs/architecture/validation-architecture.md`

Forbidden files:
- Runtime schema interpretation.
- Reflection, annotation scanning, dynamic class loading, or generated-code dependencies outside `runtime-core`.
- Open object semantics beyond the documented closed-object nested shape.

Expected behavior:
- Object properties with `type: "object"` generate deterministic nested Java record types.
- Nested object schemas use the same closed-object profile as root objects: declared properties, optionality through `required`, and `additionalProperties: false`.
- Generated readers, writers, validators, and optional metadata helpers recurse through nested objects without constructing generic JSON object graphs.
- Nested Java type names are deterministic, collision-safe, and stable across generation runs.

Tests to add/update:
- Profile tests for accepted and rejected nested object schemas.
- Binding IR tests for nested object field naming, collisions, required fields, and diagnostics.
- Golden source fixtures and generated behavior probes for nested read, write, validation, and metadata.
- JSON Schema Test Suite and SchemaStore corpus evidence for newly supported object shapes.

Documentation to update:
- Supported profile nested object section and matrix notes.
- Generated-code contract for nested record shape and nested metadata.
- Validation architecture for nested object paths.

Commands to run:
- `./gradlew :modules:schema-model:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- A closed object property generates, compiles, round-trips, validates, and reports nested JSON instance paths deterministically.
- Unsupported open or untyped nested object shapes still reject with stable diagnostics and exact schema JSON Pointer locations.

Rollback notes:
- Revert nested object IR, emitters, tests, and documentation updates as one feature slice.
