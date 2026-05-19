# TASK-0016: String and Numeric Facets

Task ID: `TASK-0016`
Status: `complete`
Gate: Collections and value constraints
Depends on: `TASK-0015`
Specification references: JSON Schema Draft 2020-12 Validation keywords `minLength`, `maxLength`, `pattern`, `format`, `minimum`, `maximum`, `exclusiveMinimum`, `exclusiveMaximum`
Target modules: `schema-model`, `generator-core`, `runtime-core`

Allowed files:
- `modules/schema-model/**`
- `modules/generator-core/**`
- `modules/runtime-core/**`
- `modules/conformance-tests/**`
- `docs/architecture/validation-architecture.md`
- `docs/standards-baseline.md`

Forbidden files:
- Non-v1 formats.
- Arbitrary custom format registration.
- Runtime regex or format discovery.

Expected behavior:
- Profile accepts supported string and numeric facets.
- Supported formats are limited to `date`, `date-time`, and `uuid`.
- Generated validators accumulate precise facet errors and support fail-fast mode.
- Pattern handling is deterministic and avoids runtime extension points.

Tests to add/update:
- Profile tests for every supported and rejected facet.
- Generated validator behavior tests for boundary values.
- Official JSON Schema Test Suite allowlist entries where applicable.

Documentation to update:
- Validation architecture facet table.
- Standards baseline support notes for supported formats.

Commands to run:
- `./gradlew :modules:schema-model:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Supported scalar facets work through generated validators with deterministic error codes and paths.

Rollback notes:
- Revert facet model, IR, emitter, and tests from this task.
