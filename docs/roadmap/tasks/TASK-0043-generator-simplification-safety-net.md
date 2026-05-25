# TASK-0043: Generator Simplification Safety Net

Task ID: `TASK-0043`
Status: `ready-for-implementation`
Gate: Generator simplification implementation
Depends on: `TASK-0042`
Specification references: `docs/architecture/generator-simplification-audit.md`; `docs/architecture/generated-code-contract.md`
Target modules: `generator-core`

Allowed files:
- `modules/generator-core/src/test/**`
- `modules/generator-core/src/generatedCodeSmoke/**`
- `docs/roadmap/tasks/TASK-0043-generator-simplification-safety-net.md`

Forbidden files:
- Production source changes under `modules/generator-core/src/main/**`.
- Public generator API changes.
- Generated Java behavior changes.
- JSON Schema profile expansion.

Expected behavior:
- Add characterization coverage that protects the current complex generated binding behavior before simplification refactors.
- Emphasize nested objects, tagged unions, map bindings, pattern maps, literal constraints, metadata, and deterministic diagnostics.
- Keep generated-source output byte-for-byte stable.
- Use the current generated-code smoke and golden verification conventions where they already fit.

Tests to add/update:
- Add focused generator tests or smoke probes that exercise representative complex fixtures.
- Ensure deterministic diagnostics are covered for at least one complex unsupported or invalid schema edge.
- Avoid narrowing existing fixture coverage while adding the safety net.

Documentation to update:
- Update this task status to `complete` after implementation.

Commands to run:
- `./gradlew :modules:generator-core:check --console=plain`
- `./gradlew :modules:schema-model:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- The new tests fail if complex generated binding behavior or diagnostics regress.
- No production source files change.
- Required gates pass.

Rollback notes:
- Revert the added or updated tests and this task status.
