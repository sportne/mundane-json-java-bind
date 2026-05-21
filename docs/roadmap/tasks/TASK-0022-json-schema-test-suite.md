# TASK-0022: JSON Schema Test Suite Allowlist

Task ID: `TASK-0022`
Status: `complete`
Gate: Completion hardening
Depends on: `TASK-0021`
Specification references: Official JSON Schema Test Suite Draft 2020-12 fixtures
Target modules: `conformance-tests`, `schema-model`, `generator-core`

Allowed files:
- `modules/conformance-tests/**`
- `modules/schema-model/src/test/**`
- `modules/generator-core/src/test/**`
- `docs/verification/json-schema-test-suite.md`
- `docs/standards-baseline.md`

Forbidden files:
- Broadening profile support only to satisfy unsupported fixture cases.
- Vendoring untracked upstream fixture snapshots without documented provenance.

Expected behavior:
- Conformance tests include an explicit allowlist of supported Draft 2020-12 test cases.
- Unsupported official fixtures have explicit skip reasons tied to profile exclusions.
- Allowlist results exercise generated code, not a dynamic schema interpreter.
- Fixture provenance and update process are documented.

Tests to add/update:
- Official fixture allowlist execution.
- Skip reason validation test.
- Regression tests for any supported fixtures that reveal generator defects.

Documentation to update:
- JSON Schema Test Suite verification notes.
- Standards baseline traceability table.

Commands to run:
- `./gradlew :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Supported and skipped official Draft 2020-12 fixtures are traceable, deterministic, and documented.

Rollback notes:
- Revert allowlist fixtures and related conformance harness changes.
