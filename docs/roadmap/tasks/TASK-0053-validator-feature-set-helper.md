# TASK-0053: Validator Feature Set Helper

Task ID: `TASK-0053`
Status: `complete`
Gate: Performance and simplicity follow-up
Depends on: `TASK-0052`
Specification references: `docs/architecture/generator-simplification-audit.md`; `docs/architecture/generated-code-contract.md`; `docs/architecture/validation-architecture.md`
Target modules: `generator-core`

Allowed files:
- `modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/internal/emitter/**`
- `modules/generator-core/src/test/java/io/github/mundanej/mjjb/generator/core/internal/emitter/**`
- `modules/generator-core/src/test/resources/golden/**`
- `docs/roadmap/tasks/TASK-0053-validator-feature-set-helper.md`

Forbidden files:
- Public generator API changes.
- Runtime interface changes.
- Schema profile expansion.
- Generated behavior changes.
- Broad validation-plan rewrites.

Expected behavior:
- Extract validator helper/import selection from `ValidatorSourceEmitter` into a package-private internal feature-set helper.
- Preserve generated validator source byte-for-byte after the intentional changes already introduced by `TASK-0051` and `TASK-0052`.
- Keep the helper interface small: it should answer which validator capabilities are needed, not render validation rules.

Tests to add/update:
- Add focused unit tests for feature detection across scalar, array, nullable, map, object, pattern, numeric, literal, and tagged-union bindings.
- Keep generated-source golden and generated behavior tests passing.

Documentation to update:
- Update this task status to `complete` after implementation.

Commands to run:
- `./gradlew :modules:generator-core:check --console=plain`
- `./gradlew :modules:schema-model:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Validator feature detection has locality outside `ValidatorSourceEmitter`.
- Generated validator output is unchanged by this refactor.
- Required gates pass.

Rollback notes:
- Revert feature-set helper, emitter call-site changes, tests, and this task status.
