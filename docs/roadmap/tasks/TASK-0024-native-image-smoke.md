# TASK-0024: Native Image Smoke Lane

Task ID: `TASK-0024`
Status: `complete`
Gate: Completion hardening
Depends on: `TASK-0023`
Specification references: Project Native Image compatibility constraints
Target modules: `runtime-core`, `parser-core`, `generator-cli`, `conformance-tests`, examples

Allowed files:
- `build-logic/**`
- `modules/conformance-tests/**`
- `modules/runtime-core/**`
- `modules/parser-core/**`
- `modules/generator-cli/**`
- `examples/**`
- `docs/verification/native-image.md`
- `.github/workflows/**`

Forbidden files:
- Reflection configuration files for normal runtime behavior.
- Native-specific behavior branches unless documented and tested.

Expected behavior:
- Native Image smoke tests compile representative runtime/parser and generated-binding programs.
- Smoke programs exercise read, validate, write, and deterministic diagnostic paths.
- Native lane is separate from the default local quality gate but available in CI.
- No reachability metadata is required for normal generated bindings.

Tests to add/update:
- Native Image build task or CI lane.
- Representative generated binding smoke fixture.
- Parser/runtime smoke fixture.

Documentation to update:
- Native Image verification guide.
- CI verification notes.

Commands to run:
- `./gradlew qualityGate --console=plain`
- Native smoke command documented by this task.

Acceptance criteria:
- Native Image smoke lane passes in documented environments without reflection configuration for runtime/generated binding behavior.

Rollback notes:
- Revert native smoke tasks, fixtures, and CI lane additions.
