# TASK-0003: JSON Writer Core Completion

Task ID: `TASK-0003`
Status: `ready-for-implementation`
Gate: Runtime/parser foundation
Depends on: `TASK-0002`
Specification references: JSON string and value grammar from RFC 8259
Target modules: `parser-core`, `runtime-core`

Allowed files:
- `modules/parser-core/src/main/java/io/github/mundanej/mjjb/parser/JsonStringWriter.java`
- `modules/parser-core/src/test/java/io/github/mundanej/mjjb/parser/JsonStringWriterTest.java`
- `modules/runtime-core/src/main/java/io/github/mundanej/mjjb/runtime/JsonWriter.java`
- `docs/architecture/parser-architecture.md`

Forbidden files:
- Schema frontend and generator emitters.

Expected behavior:
- JSON writer emits valid JSON for generated writers.
- Writer rejects incomplete documents, duplicate root values, invalid structure, and invalid number literals.
- String escaping is deterministic and JSON-valid.

Tests to add/update:
- Writer tests for objects, arrays, strings, numbers, booleans, nulls, invalid state transitions, and completion checks.

Documentation to update:
- Parser/writer architecture notes.

Commands to run:
- `./gradlew :modules:parser-core:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Writer can produce valid round-trip JSON for the basic-object slice.
- Writer normal path does not allocate reflection or metadata structures.

Rollback notes:
- Revert writer implementation and tests from this task.
