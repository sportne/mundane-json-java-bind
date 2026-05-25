# TASK-0047: Schema Literal And Annotation Reader

Task ID: `TASK-0047`
Status: `ready-for-implementation`
Gate: Generator simplification implementation
Depends on: `TASK-0046`
Specification references: `docs/architecture/generator-simplification-audit.md`; `docs/architecture/generated-code-contract.md`; JSON Schema Draft 2020-12 validation and annotation keywords
Target modules: `generator-core`

Allowed files:
- `modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/internal/binding/**`
- `modules/generator-core/src/test/java/io/github/mundanej/mjjb/generator/core/internal/binding/**`
- `modules/generator-core/src/test/java/io/github/mundanej/mjjb/generator/core/**`
- `docs/roadmap/tasks/TASK-0047-schema-literal-annotation-reader.md`

Forbidden files:
- Public generator API changes.
- Generated Java behavior changes.
- Diagnostic code or pointer behavior changes.
- JSON Schema profile expansion.

Expected behavior:
- Extract enum, const, default, and annotation JSON reading from `BindingModelBuilder` into a focused internal helper.
- Preserve canonical compact JSON behavior, literal constraints, metadata annotation preservation, null handling, and pointer-stable diagnostics.

Tests to add/update:
- Add focused tests for enum, const, default, annotation JSON preservation, compact JSON stability, null handling, and diagnostic pointer stability.
- Keep existing builder, metadata, golden, and generated-code smoke tests passing.

Documentation to update:
- Update this task status to `complete` after implementation.

Commands to run:
- `./gradlew :modules:generator-core:check --console=plain`
- `./gradlew :modules:schema-model:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Literal and annotation reading is isolated behind a dedicated internal helper.
- Existing generated metadata, defaults, enum, and const behavior is preserved.
- Required gates pass.

Rollback notes:
- Revert the helper, builder call-site changes, tests, and this task status.
