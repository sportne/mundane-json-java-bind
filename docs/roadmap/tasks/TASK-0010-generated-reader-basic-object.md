# TASK-0010: Generated Reader for Basic Objects

Task ID: `TASK-0010`
Status: `draft`
Gate: First round trip
Depends on: `TASK-0009`, `TASK-0002`
Specification references: JSON object/scalar grammar and accepted JSON Schema type validation behavior
Target modules: `generator-core`, `parser-core`, `runtime-core`

Allowed files:
- `modules/generator-core/**`
- `modules/runtime-core/src/main/java/io/github/mundanej/mjjb/runtime/**`
- `modules/parser-core/**`

Forbidden files:
- Array/facet/oneOf emitters except explicit unsupported diagnostics.

Expected behavior:
- Generator emits one reader class per generated root model.
- Readers use explicit token handling and generated `switch` dispatch.
- Missing required fields, duplicate properties, unknown properties, type mismatches, and malformed JSON produce deterministic diagnostics.

Tests to add/update:
- Golden reader source tests.
- Generated reader compile and behavior tests.
- Reader error-path tests with JSON instance paths.

Documentation to update:
- Generated-code contract reader section.

Commands to run:
- `./gradlew :modules:generator-core:check :modules:parser-core:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Generated reader can parse basic object fixtures into generated records.
- Generated reader does not depend on parser implementation internals beyond runtime interfaces.

Rollback notes:
- Revert reader emitter and fixtures.
