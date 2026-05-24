# TASK-0033: Constrained `allOf` Object Flattening

Task ID: `TASK-0033`
Status: `complete`
Gate: Post-v1 profile expansion
Depends on: `TASK-0032`
Specification references: JSON Schema Draft 2020-12 Applicator keyword `allOf`
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
- Generic applicator evaluation.
- `anyOf`, generic `oneOf`, `not`, or conditional schema support.
- Runtime branch matching or dynamic schema interpretation.

Expected behavior:
- `allOf` is accepted only when every branch is a supported object schema that can be flattened into one deterministic object binding.
- Flattening merges properties, required names, annotations, and compatible object-level constraints without changing generated read/write/validate semantics.
- Conflicting property definitions, unsafe `additionalProperties` constraints, ambiguous annotations, or non-object branches reject with stable diagnostics.
- Any schema object inside the flattened `allOf` composition that constrains `additionalProperties` must declare every merged property in that same schema object.
- Local `$ref` branches are allowed only after `TASK-0031` resolution is complete.

Tests to add/update:
- Profile and binding tests for successful object merges, required-field union, and deterministic property order.
- Diagnostics for conflicting property schemas, incompatible open/closed object rules, and unsupported branch shapes.
- Golden source and behavior probes comparing flattened `allOf` with equivalent inline schemas.
- JSON Schema Test Suite trace updates for representative `allOf` cases.

Documentation to update:
- Supported profile `allOf` flattening limits.
- Generated-code contract object merge order and diagnostics.
- Validation architecture notes for flattened schema locations.

Commands to run:
- `./gradlew :modules:schema-model:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Supported `allOf` object compositions generate the same Java shape and behavior as equivalent flattened object schemas, while ambiguous compositions reject deterministically.

Rollback notes:
- Revert `allOf` flattening, diagnostics, fixtures, conformance updates, and documentation updates.
