# TASK-0019: Object Diagnostic Hardening

Task ID: `TASK-0019`
Status: `draft`
Gate: Object shape expansion
Depends on: `TASK-0018`
Specification references: JSON Schema Draft 2020-12 Validation keywords `properties`, `required`, `additionalProperties`
Target modules: `generator-core`, `runtime-core`, `parser-core`, `conformance-tests`

Allowed files:
- `modules/generator-core/**`
- `modules/runtime-core/**`
- `modules/parser-core/**`
- `modules/conformance-tests/**`
- `docs/architecture/error-reporting.md`
- `docs/architecture/validation-architecture.md`

Forbidden files:
- New schema keyword support unless needed to reject it precisely.
- Lenient duplicate property handling.

Expected behavior:
- Generated readers reject duplicate object properties with exact JSON paths and source locations when available.
- Generated validators report missing required properties, unknown properties, wrong token kinds, and scalar violations consistently.
- Diagnostics distinguish parse failures, read failures, validation failures, and unsupported schema failures.
- Error ordering is deterministic.

Tests to add/update:
- Duplicate property tests in parser and generated readers.
- Diagnostic ordering tests.
- Error path tests for nested objects and arrays.
- Unsupported keyword pointer tests that cover nested object locations.

Documentation to update:
- Error reporting model with code, path, and location examples.

Commands to run:
- `./gradlew :modules:parser-core:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Object binding diagnostics are stable, precise, and documented across parser, reader, validator, and schema profile failures.

Rollback notes:
- Revert diagnostic code and tests from this task.
