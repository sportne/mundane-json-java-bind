# TASK-0009: Generated Writer for Basic Objects

Task ID: `TASK-0009`
Status: `draft`
Gate: First round trip
Depends on: `TASK-0008`, `TASK-0003`
Specification references: JSON object and scalar serialization grammar
Target modules: `generator-core`, `parser-core`, `runtime-core`

Allowed files:
- `modules/generator-core/**`
- `modules/runtime-core/src/main/java/io/github/mundanej/mjjb/runtime/JsonWriter.java`
- `modules/parser-core/src/main/java/io/github/mundanej/mjjb/parser/JsonStringWriter.java`
- `modules/parser-core/src/test/**`

Forbidden files:
- Schema profile expansion beyond basic object keywords.

Expected behavior:
- Generator emits one writer class per generated root model.
- Writers emit deterministic schema property order.
- Writers skip absent optional values and write scalar JSON values explicitly.

Tests to add/update:
- Golden writer source tests.
- Generated writer compile and behavior tests.
- Round-trip writer output comparison for basic object fixtures.

Documentation to update:
- Generated-code contract writer section.

Commands to run:
- `./gradlew :modules:generator-core:check :modules:parser-core:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Generated writer output is valid JSON and deterministic.
- Generated writer source contains no reflection or runtime discovery.

Rollback notes:
- Revert writer emitter and related fixtures.
