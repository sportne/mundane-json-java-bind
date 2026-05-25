# TASK-0048: Object Shape Flattener

Task ID: `TASK-0048`
Status: `ready-for-implementation`
Gate: Generator simplification implementation
Depends on: `TASK-0047`
Specification references: `docs/architecture/generator-simplification-audit.md`; `docs/supported-profile.md`; JSON Schema Draft 2020-12 `allOf`
Target modules: `generator-core`

Allowed files:
- `modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/internal/binding/**`
- `modules/generator-core/src/test/java/io/github/mundanej/mjjb/generator/core/internal/binding/**`
- `modules/generator-core/src/test/java/io/github/mundanej/mjjb/generator/core/**`
- `docs/roadmap/tasks/TASK-0048-object-shape-flattener.md`

Forbidden files:
- Public generator API changes.
- Generated Java behavior changes.
- Broadening `allOf` support beyond the documented constrained object flattening profile.
- Diagnostic code or pointer behavior changes.

Expected behavior:
- Extract constrained `allOf` object merge behavior from `BindingModelBuilder` into a focused internal object-shape flattener.
- Preserve accepted merge behavior, duplicate property equivalence, required union behavior, metadata merge behavior, closure handling, and rejection diagnostics.

Tests to add/update:
- Add focused tests for compatible merges, required union, duplicate property equivalence, conflicting properties, incompatible `additionalProperties`, metadata merge behavior, and diagnostic pointer stability.
- Keep existing builder, generated-source golden, and smoke tests passing.

Documentation to update:
- Update this task status to `complete` after implementation.

Commands to run:
- `./gradlew :modules:generator-core:check --console=plain`
- `./gradlew :modules:schema-model:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Constrained `allOf` object flattening is isolated behind a dedicated internal helper.
- Existing accepted and rejected `allOf` behavior is preserved.
- Required gates pass.

Rollback notes:
- Revert the flattener, builder call-site changes, tests, and this task status.
