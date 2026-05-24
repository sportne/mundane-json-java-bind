# TASK-0032: Map Bindings for Object-Valued `additionalProperties`

Task ID: `TASK-0032`
Status: `draft`
Gate: Post-v1 profile expansion
Depends on: `TASK-0031`
Specification references: JSON Schema Draft 2020-12 Applicator keyword `additionalProperties`
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
- Generic `Map<String, Object>` bindings.
- Runtime schema interpretation or reflection-based map value binding.
- Pattern-based key routing; that belongs to `TASK-0034`.

Expected behavior:
- Object-valued `additionalProperties` generates `Map<String, T>` bindings when the value schema maps to a supported scalar, nested object, nullable, or homogeneous array value shape.
- Closed objects with `additionalProperties: false` keep their existing behavior.
- Readers collect unknown declared-property names into the generated map instead of rejecting them when a map binding is present.
- Writers emit declared properties first in schema order, then map entries in deterministic key order.
- Validators apply value-schema constraints to every map entry and report errors at `$.key` paths.

Tests to add/update:
- Profile tests for accepted scalar, array, and nested-object map value schemas.
- Generator diagnostics for unsupported map value schemas and map/declaration collisions.
- Golden source and generated behavior probes for read, write, validation, metadata, and deterministic ordering.
- SchemaStore corpus samples that currently reject for object-valued `additionalProperties`.

Documentation to update:
- Supported profile map binding section and matrix note.
- Generated-code contract map field shape.
- Validation architecture for map entry paths.

Commands to run:
- `./gradlew :modules:schema-model:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- A supported object-valued `additionalProperties` schema generates deterministic Java map bindings with round-trip and validation behavior covered by generated-code tests.

Rollback notes:
- Revert map IR, emitter changes, tests, examples if added, and documentation updates.
