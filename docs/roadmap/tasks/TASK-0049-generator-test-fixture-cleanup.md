# TASK-0049: Generator Test Fixture Cleanup

Task ID: `TASK-0049`
Status: `complete`
Gate: Generator simplification implementation
Depends on: `TASK-0048`
Specification references: `docs/architecture/generator-simplification-audit.md`; `docs/architecture/generated-code-contract.md`
Target modules: `generator-core`

Allowed files:
- `modules/generator-core/src/test/**`
- `modules/generator-core/src/generatedCodeSmoke/**`
- `docs/roadmap/tasks/TASK-0049-generator-test-fixture-cleanup.md`

Forbidden files:
- Production source changes under `modules/generator-core/src/main/**`.
- Generated Java behavior changes.
- Removing existing fixture scenarios.
- JSON Schema profile expansion.

Expected behavior:
- Reduce repeated golden verification, allowed-token checks, source lookup, and compilation boilerplate in generator tests.
- Keep generator API behavior, diagnostics, output path handling, and integration edge coverage clear.
- Preserve or improve existing coverage without production changes.

Tests to add/update:
- Refactor existing tests to use focused fixture helpers.
- Keep scenario coverage unchanged and add assertions only where they clarify existing behavior.

Documentation to update:
- Update this task status to `complete` after implementation.

Commands to run:
- `./gradlew :modules:generator-core:check --console=plain`
- `./gradlew :modules:schema-model:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Repeated generator fixture verification code is materially reduced.
- Existing fixture scenarios remain covered.
- Required gates pass.

Rollback notes:
- Revert the test fixture cleanup and this task status.
