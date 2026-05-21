# TASK-0025: Performance and Memory Evidence

Task ID: `TASK-0025`
Status: `complete`
Gate: Completion hardening
Depends on: `TASK-0024`
Specification references: Project predictable performance and memory behavior goals
Target modules: `parser-core`, `runtime-core`, `generator-core`, `conformance-tests`

Allowed files:
- `modules/parser-core/**`
- `modules/runtime-core/**`
- `modules/generator-core/**`
- `modules/conformance-tests/**`
- `docs/verification/performance.md`

Forbidden files:
- Benchmark-driven public API churn unless separately documented.
- Introducing runtime dependencies for measurement.

Expected behavior:
- Parser and generated binding performance evidence covers representative small and medium JSON documents.
- Memory evidence covers streaming behavior and avoids generic object graph reconstruction.
- Measurements are repeatable enough for regression investigation without becoming a brittle CI gate.
- Performance notes document known limits and non-goals.

Tests to add/update:
- Parser allocation or bounded-memory regression tests where practical.
- Generated binding throughput smoke tests where practical.
- Documentation examples with exact commands and environment notes.

Documentation to update:
- Performance verification guide.
- README performance claim boundaries.

Commands to run:
- `./gradlew qualityGate --console=plain`
- Performance evidence command documented by this task.

Acceptance criteria:
- The project has documented, reproducible evidence for predictable parser and generated binding behavior.

Rollback notes:
- Revert performance fixtures and documentation from this task.
