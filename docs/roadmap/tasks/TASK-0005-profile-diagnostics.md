# TASK-0005: Profile Validation Diagnostics

Task ID: `TASK-0005`
Status: `ready-for-implementation`
Gate: Schema frontend
Depends on: `TASK-0004`
Specification references: JSON Schema Draft 2020-12 Core and Validation keyword definitions
Target modules: `schema-model`, `generator-core`, `conformance-tests`

Allowed files:
- `modules/schema-model/**`
- `modules/generator-core/**`
- `modules/conformance-tests/**`
- `docs/standards-baseline.md`

Forbidden files:
- Model/reader/writer emitters except diagnostic consumption points.

Expected behavior:
- `JSP-DATA-2020-12` has an explicit support matrix for every accepted and known-rejected keyword.
- Unsupported keywords fail with stable code, exact schema pointer, and actionable reason.
- Unsupported shapes of accepted keywords fail deterministically.

Tests to add/update:
- One accepted-keyword test per v1 keyword.
- One unsupported-keyword test per rejected Draft 2020-12 keyword known to the project.
- Diagnostic ordering tests.

Documentation to update:
- Standards baseline support table.

Commands to run:
- `./gradlew :modules:schema-model:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Unsupported-feature diagnostics are deterministic and pointer-accurate.
- No unsupported schema reaches source emission.

Rollback notes:
- Revert support-matrix and diagnostic changes.
