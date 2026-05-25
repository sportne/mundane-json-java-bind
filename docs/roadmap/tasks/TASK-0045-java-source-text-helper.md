# TASK-0045: Java Source Text Helper

Task ID: `TASK-0045`
Status: `ready-for-implementation`
Gate: Generator simplification implementation
Depends on: `TASK-0044`
Specification references: `docs/architecture/generator-simplification-audit.md`; `docs/architecture/generated-code-contract.md`
Target modules: `generator-core`

Allowed files:
- `modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/internal/emitter/**`
- `modules/generator-core/src/test/java/io/github/mundanej/mjjb/generator/core/internal/emitter/**`
- `modules/generator-core/src/test/java/io/github/mundanej/mjjb/generator/core/**`
- `docs/roadmap/tasks/TASK-0045-java-source-text-helper.md`

Forbidden files:
- Public generator API changes.
- Broad source-generation DSLs or templating frameworks.
- Generated-source golden changes unless the staged review identifies a pre-existing inconsistency and the commit explains it.
- JSON Schema profile expansion.

Expected behavior:
- Introduce a small package-private Java source text helper for Java string literal escaping and line indentation only.
- Refactor emitters to use the shared helper.
- Preserve generated Java output byte-for-byte.

Tests to add/update:
- Add unit tests covering quotes, backslashes, control characters, newlines, empty lines, and indentation preservation.
- Keep existing generated-source golden and smoke checks passing without output churn.

Documentation to update:
- Update this task status to `complete` after implementation.

Commands to run:
- `./gradlew :modules:generator-core:check --console=plain`
- `./gradlew :modules:schema-model:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Emitters share exact Java string literal and indentation behavior.
- No broad source-generation abstraction is introduced.
- Required gates pass.

Rollback notes:
- Revert the source text helper, emitter call-site changes, tests, and this task status.
