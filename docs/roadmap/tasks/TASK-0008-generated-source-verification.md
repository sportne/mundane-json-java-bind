# TASK-0008: Generated-Source Verification Harness

Task ID: `TASK-0008`
Status: `complete`
Gate: First binding slice
Depends on: `TASK-0007`
Specification references: Java 21 compiler behavior
Target modules: `generator-core`

Allowed files:
- `modules/generator-core/build.gradle`
- `modules/generator-core/src/test/**`
- `modules/generator-core/src/generatedCodeSmoke/**`
- `docs/verification/**`

Forbidden files:
- Runtime parser behavior.

Expected behavior:
- Generated source fixtures are compiled and optionally executed by dedicated verification tasks.
- Harness enforces deterministic output and forbidden token checks.
- Harness is reusable for reader, writer, validator, and oneOf fixtures.

Tests to add/update:
- Harness self-tests.
- Generated source compile smoke fixtures.

Documentation to update:
- Verification strategy and generated-source contract.

Commands to run:
- `./gradlew :modules:generator-core:generatedCodeSmoke --console=plain`
- `./gradlew :modules:generator-core:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Harness compiles generated model fixtures under Java 21 with warnings as errors.
- Harness failure output names the generated source and reason.

Rollback notes:
- Remove generated-code smoke tasks and fixtures.
