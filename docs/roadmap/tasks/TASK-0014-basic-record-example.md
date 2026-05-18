# TASK-0014: Basic Record Example

Task ID: `TASK-0014`
Status: `complete`
Gate: Example evidence
Depends on: `TASK-0013`
Specification references: JSON Schema Draft 2020-12 Validation keywords `type`, `properties`, `required`, `additionalProperties`
Target modules: `examples/basic-record`, `conformance-tests`

Allowed files:
- `examples/basic-record/**`
- `modules/conformance-tests/**`
- `docs/architecture/generated-code-contract.md`
- `README.md`

Forbidden files:
- Generator behavior except defects found by the example.
- Additional examples outside JSON Schema to Java binding.

Expected behavior:
- Basic example demonstrates schema, generated source, read, validate, write, and deterministic failure diagnostics.
- Example uses generated code and runtime APIs only; it does not depend on generator internals.
- Example can be run as part of normal verification.

Tests to add/update:
- Example build test.
- Generated source compile test for the example.
- Runtime behavior test using the example binding.

Documentation to update:
- README quickstart with the basic example.
- Generated-code contract if example reveals missing contract details.

Commands to run:
- `./gradlew :examples:basic-record:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- The basic object binding milestone is complete and demonstrable through CLI, Gradle plugin, and example project.

Rollback notes:
- Revert example project and related conformance fixtures.
