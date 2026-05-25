# TASK-0044: Emitter Binding Traversal Helper

Task ID: `TASK-0044`
Status: `complete`
Gate: Generator simplification implementation
Depends on: `TASK-0043`
Specification references: `docs/architecture/generator-simplification-audit.md`; `docs/architecture/generated-code-contract.md`
Target modules: `generator-core`

Allowed files:
- `modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/internal/emitter/**`
- `modules/generator-core/src/test/java/io/github/mundanej/mjjb/generator/core/internal/emitter/**`
- `modules/generator-core/src/test/java/io/github/mundanej/mjjb/generator/core/**`
- `docs/roadmap/tasks/TASK-0044-emitter-binding-traversal-helper.md`

Forbidden files:
- Public generator API changes.
- Binding model semantic changes.
- Generated-source golden changes unless the staged review identifies a pre-existing inconsistency and the commit explains it.
- JSON Schema profile expansion.

Expected behavior:
- Introduce a package-private traversal helper for deterministic access to all fields, all maps, nested objects, and all objects in a `BindingModel`.
- Replace duplicated traversal logic in model, reader, writer, validator, and metadata emitters where applicable.
- Preserve generated Java output and diagnostics.

Tests to add/update:
- Add unit tests for traversal order across root objects, nested object fields, object-valued maps, pattern maps, and tagged union branches.
- Keep existing generated-source golden and smoke checks passing without output churn.

Documentation to update:
- Update this task status to `complete` after implementation.

Commands to run:
- `./gradlew :modules:generator-core:check --console=plain`
- `./gradlew :modules:schema-model:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Emitters share one traversal implementation for common recursive binding traversal.
- Traversal tests document deterministic ordering.
- Required gates pass.

Rollback notes:
- Revert the traversal helper, emitter call-site changes, tests, and this task status.
