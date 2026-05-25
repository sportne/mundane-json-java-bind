# TASK-0052: Generated Numeric Literal Constants

Task ID: `TASK-0052`
Status: `complete`
Gate: Performance and simplicity follow-up
Depends on: `TASK-0051`
Specification references: `docs/adr/ADR-0003-current-profile-number-bindings.md`; `docs/architecture/generated-code-contract.md`; `docs/architecture/validation-architecture.md`
Target modules: `generator-core`

Allowed files:
- `modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/internal/emitter/**`
- `modules/generator-core/src/test/**`
- `modules/generator-core/src/generatedCodeSmoke/**`
- `docs/architecture/generated-code-contract.md`
- `docs/architecture/validation-architecture.md`
- `docs/roadmap/tasks/TASK-0052-generated-numeric-literal-constants.md`

Forbidden files:
- Public generator API changes.
- Runtime interface changes.
- Schema profile expansion.
- Numeric binding semantics changes beyond ADR-0003.
- Generated model public API changes.

Expected behavior:
- Generated validators use private static final `BigDecimal` constants for schema numeric literals used by numeric facets, `multipleOf`, and numeric enum/const comparisons.
- Generated validators may still convert Java model values to `BigDecimal` at validation time.
- Diagnostics, paths, and ADR-0003 numeric semantics remain unchanged.

Tests to add/update:
- Update generated-source goldens that intentionally change.
- Add or update generated behavior probes for numeric facets, `multipleOf`, and numeric literal constraints.
- Verify generated validators do not repeatedly parse unchanged schema numeric literals in helper methods or literal comparisons.

Documentation to update:
- Update validation architecture or generated-code contract if the generated numeric helper shape is described.
- Update this task status to `complete` after implementation.

Commands to run:
- `./gradlew :modules:generator-core:check --console=plain`
- `./gradlew :modules:schema-model:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Generated numeric validation behavior is unchanged.
- Schema numeric literals are represented as generated constants.
- Required gates pass.

Rollback notes:
- Revert emitter, golden, behavior-test, documentation, and this task status changes.
