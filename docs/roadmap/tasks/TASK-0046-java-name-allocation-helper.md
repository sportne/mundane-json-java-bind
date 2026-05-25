# TASK-0046: Java Name Allocation Helper

Task ID: `TASK-0046`
Status: `complete`
Gate: Generator simplification implementation
Depends on: `TASK-0045`
Specification references: `docs/architecture/generator-simplification-audit.md`; `docs/architecture/generated-code-contract.md`
Target modules: `generator-core`

Allowed files:
- `modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/internal/binding/**`
- `modules/generator-core/src/test/java/io/github/mundanej/mjjb/generator/core/internal/binding/**`
- `modules/generator-core/src/test/java/io/github/mundanej/mjjb/generator/core/**`
- `docs/roadmap/tasks/TASK-0046-java-name-allocation-helper.md`

Forbidden files:
- Public generator API changes.
- Generated Java naming changes unless required to preserve an already documented behavior bug and explicitly explained in the commit.
- Diagnostic code or pointer behavior changes.
- JSON Schema profile expansion.

Expected behavior:
- Extract deterministic Java field, object type, and tagged branch name allocation from `BindingModelBuilder` into a focused internal helper.
- Preserve current handling of Java keywords, invalid identifiers, punctuation, digit prefixes, and duplicate collisions.
- Preserve diagnostics and schema pointer locations.

Tests to add/update:
- Add focused tests for keywords, invalid identifiers, punctuation splitting, digit prefixes, duplicate collisions, nested object type names, and tagged branch names.
- Keep existing builder and generated-source tests passing.

Documentation to update:
- Update this task status to `complete` after implementation.

Commands to run:
- `./gradlew :modules:generator-core:check --console=plain`
- `./gradlew :modules:schema-model:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Naming decisions live in a dedicated internal helper.
- Existing generated names and diagnostics are preserved.
- Required gates pass.

Rollback notes:
- Revert the helper, builder call-site changes, tests, and this task status.
