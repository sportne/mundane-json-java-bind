# TASK-0034: `patternProperties` Map Bindings

Task ID: `TASK-0034`
Status: `complete`
Gate: Post-v1 profile expansion
Depends on: `TASK-0032`
Specification references: JSON Schema Draft 2020-12 Applicator keyword `patternProperties`
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
- Generic overlapping-schema evaluation for one property name.
- Runtime regex registries or user-provided matcher plugins.
- `unevaluatedProperties` support.

Expected behavior:
- `patternProperties` generates deterministic map fields for regex-keyed property groups when each pattern value schema maps to a supported map value shape.
- Patterns compile during generation and use JSON Schema regex search semantics.
- Declared `properties` retain precedence for exact declared names.
- Property names matching no declared property, no pattern, and no supported `additionalProperties` map reject as unknown.
- Ambiguous overlapping patterns reject unless the implementation defines one deterministic non-overlap rule in this task before coding.

Tests to add/update:
- Profile tests for valid and invalid pattern schemas, invalid regexes, and overlapping patterns.
- Golden source and generated behavior probes for read, write, validation, deterministic ordering, and metadata.
- Diagnostics for unmatched properties and unsupported pattern value schemas.
- SchemaStore corpus samples currently blocked by `patternProperties`.

Documentation to update:
- Supported profile `patternProperties` limits.
- Generated-code contract for regex-key map fields and property routing.
- Validation architecture for pattern map entry paths.

Commands to run:
- `./gradlew :modules:schema-model:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Supported `patternProperties` schemas generate deterministic map bindings without runtime schema interpretation and with exact diagnostics for unsupported overlaps or value schemas.

Rollback notes:
- Revert pattern-property IR, emitters, tests, conformance changes, and documentation updates.
