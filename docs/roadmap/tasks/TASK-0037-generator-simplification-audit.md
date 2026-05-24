# TASK-0037: Generator Architecture Simplification Audit

Task ID: `TASK-0037`
Status: `ready-for-implementation`
Gate: Post-v1 hardening and simplification
Depends on: `TASK-0036`
Specification references: ADR-0002 generated-code no runtime reflection; `docs/architecture/generated-code-contract.md`; `docs/architecture/module-boundaries.md`
Target modules: `generator-core`

Allowed files:
- `docs/architecture/**`
- `docs/roadmap.md`
- `docs/roadmap/tasks/TASK-0037-generator-simplification-audit.md`
- Read-only inspection of `modules/generator-core/**`
- Source changes under `modules/generator-core/**` only if they are mechanical, behavior-preserving simplifications with focused tests

Forbidden files:
- Public generator API changes.
- Generated-code behavior changes.
- JSON Schema profile expansion.
- Runtime schema interpretation or reflection.

Expected behavior:
- Document the highest-friction generator modules and the interfaces maintainers must understand to add a supported schema feature.
- Identify shallow modules or overloaded modules using the deletion test, with concrete file references.
- Produce a prioritized simplification plan that protects locality and keeps generated-code behavior unchanged.
- If a low-risk behavior-preserving cleanup is obvious, implement it only when it reduces repeated logic without broadening scope.

Tests to add/update:
- Existing generator-core checks must continue to pass.
- Add regression tests only if source cleanup changes executable code.

Documentation to update:
- Add a generator simplification audit document under `docs/architecture/`.
- Update this task status to `complete` after the audit and any scoped cleanup are done.

Commands to run:
- `./gradlew :modules:generator-core:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- The audit names concrete simplification candidates, expected benefits, likely risk, and recommended order.
- Any code changes are behavior-preserving and covered by existing or focused tests.
- Required gates pass.

Rollback notes:
- Revert the audit document, task status update, and any scoped generator-core cleanup.
