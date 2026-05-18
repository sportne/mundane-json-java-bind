# TASK-0007: Generated Model Emitter

Task ID: `TASK-0007`
Status: `complete`
Gate: First binding slice
Depends on: `TASK-0006`
Specification references: Java 21 language baseline; JSON Schema scalar type semantics for accepted types
Target modules: `generator-core`, `runtime-core`

Allowed files:
- `modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/**`
- `modules/generator-core/src/test/java/io/github/mundanej/mjjb/generator/core/**`
- `modules/generator-core/src/test/resources/**`
- `docs/architecture/generated-code-contract.md`

Forbidden files:
- Parser implementation and user tooling modules.

Expected behavior:
- Basic object schemas emit Java records for scalar properties.
- Required reference fields call `Objects.requireNonNull`.
- Optional non-null fields use `Optional<T>`.
- Generated model source is deterministic and warning-free.

Tests to add/update:
- Golden model source tests.
- Generated source compile tests with `-Xlint:all -Werror`.
- Architecture token tests forbidding reflection, annotations for binding behavior, ServiceLoader, and generator dependencies.

Documentation to update:
- Generated-code contract examples.

Commands to run:
- `./gradlew :modules:generator-core:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Approved basic-object model fixtures compile and match golden files.
- Generated models have no runtime dependency beyond JDK and allowed runtime types.

Rollback notes:
- Revert model emitter and golden fixtures.
