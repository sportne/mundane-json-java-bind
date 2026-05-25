# TASK-0051: Generated Pattern Constants

Task ID: `TASK-0051`
Status: `ready-for-implementation`
Gate: Performance and simplicity follow-up
Depends on: `TASK-0050`
Specification references: `docs/architecture/generated-code-contract.md`; `docs/architecture/validation-architecture.md`; `docs/verification/generated-code-ergonomics.md`
Target modules: `generator-core`

Allowed files:
- `modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/internal/emitter/**`
- `modules/generator-core/src/test/**`
- `modules/generator-core/src/generatedCodeSmoke/**`
- `docs/architecture/generated-code-contract.md`
- `docs/verification/generated-code-ergonomics.md`
- `docs/roadmap/tasks/TASK-0051-generated-pattern-constants.md`

Forbidden files:
- Public generator API changes.
- Runtime interface changes.
- Schema profile expansion.
- Generated model public API changes.
- Changes to JSON Schema regex search semantics.

Expected behavior:
- Generated source uses private static final `Pattern` constants instead of repeated `Pattern.compile(...)` for generated model constructor key checks, generated reader pattern-property routing, and generated validator pattern checks.
- Pattern matching continues to use JSON Schema search semantics.
- Generated source continues to depend only on JDK and runtime-core types.

Tests to add/update:
- Update generated-source goldens that intentionally change.
- Add or update generated behavior probes proving pattern maps and string `pattern` validation still behave the same.
- Verify generated source no longer contains repeated hot-path `Pattern.compile(...)` calls for supported pattern use.

Documentation to update:
- Update generated-code ergonomics/performance notes if they mention inline pattern compilation.
- Update this task status to `complete` after implementation.

Commands to run:
- `./gradlew :modules:generator-core:check --console=plain`
- `./gradlew :modules:schema-model:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Generated pattern behavior is unchanged.
- Generated hot paths use generated constants instead of repeated regex compilation.
- Required gates pass.

Rollback notes:
- Revert emitter, golden, behavior-test, documentation, and this task status changes.
