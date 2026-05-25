# TASK-0050: Focused Performance Evidence

Task ID: `TASK-0050`
Status: `ready-for-implementation`
Gate: Performance and simplicity follow-up
Depends on: `TASK-0049`
Specification references: `docs/verification/performance.md`; `docs/architecture/parser-architecture.md`; `docs/architecture/generated-code-contract.md`; `docs/verification/generated-code-ergonomics.md`
Target modules: `conformance-tests`

Allowed files:
- `modules/conformance-tests/src/test/java/**`
- `docs/verification/performance.md`
- `docs/roadmap/tasks/TASK-0050-focused-performance-evidence.md`

Forbidden files:
- Production source changes.
- Generated Java behavior changes.
- Timing thresholds or pass/fail performance budgets.
- Committed performance report output under `build/`.

Expected behavior:
- Extend the diagnostic performance evidence lane with focused cases for regex-heavy generated bindings, numeric-heavy generated validation, Reader-backed large JSON input, and separated generated read-only, validate-only, and write-only measurements.
- Keep all generated performance artifacts under `build/`.
- Preserve the existing non-gating performance posture: report shape and command execution are verified, timing values are evidence only.

Tests to add/update:
- Update performance evidence report-shape tests for the new cases.
- Keep the quick mode fast enough for normal conformance checks.
- Ensure checksums keep each measured operation observable.

Documentation to update:
- Update `docs/verification/performance.md` with the new evidence coverage.
- Update this task status to `complete` after implementation.

Commands to run:
- `./gradlew :modules:generator-core:check --console=plain`
- `./gradlew :modules:schema-model:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`
- `./gradlew :modules:conformance-tests:performanceEvidence --console=plain`

Acceptance criteria:
- Performance evidence report includes the new focused cases.
- Normal checks and the full evidence command pass.
- No generated performance report files are committed.

Rollback notes:
- Revert performance evidence harness changes, documentation updates, and this task status.
