# TASK-0038: Performance Baseline v2

Task ID: `TASK-0038`
Status: `ready-for-implementation`
Gate: Post-v1 hardening and simplification
Depends on: `TASK-0037`
Specification references: `docs/verification/performance.md`; `docs/architecture/parser-architecture.md`; `docs/architecture/generated-code-contract.md`
Target modules: `conformance-tests`, `generator-core`, `parser-core`

Allowed files:
- `modules/conformance-tests/**`
- `docs/verification/performance.md`
- `docs/roadmap/tasks/TASK-0038-performance-baseline-v2.md`

Forbidden files:
- Runtime or generated-code behavior changes.
- CI timing pass/fail thresholds.
- New runtime dependencies.
- Benchmark framework adoption unless justified by an explicit follow-up decision.

Expected behavior:
- Extend performance evidence beyond parser and generated read/validate/write loops.
- Measure generator throughput, emitted source size, generated Java compile time, larger object/map/nested fixtures, and representative validator-heavy generated code.
- Preserve the current diagnostic-evidence posture: performance data guides regressions but does not create brittle timing gates.
- Document how to interpret results and what is still not measured.

Tests to add/update:
- Add or update conformance-test coverage for producing the v2 performance report.
- Ensure report generation is deterministic enough for local comparison.

Documentation to update:
- Update `docs/verification/performance.md` with v2 coverage and interpretation notes.
- Update this task status to `complete`.

Commands to run:
- `./gradlew :modules:conformance-tests:performanceEvidence --console=plain`
- `./gradlew :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- The generated performance report includes generator-time and generated-source-size evidence in addition to runtime parser/generated-binding evidence.
- The report is written under `build/` and no generated performance output is committed.
- Required gates pass.

Rollback notes:
- Revert performance harness changes, documentation updates, and task status.
