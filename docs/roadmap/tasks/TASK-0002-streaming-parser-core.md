# TASK-0002: Streaming Parser Core Completion

Task ID: `TASK-0002`
Status: `ready-for-implementation`
Gate: Runtime/parser foundation
Depends on: `TASK-0001`, `TASK-0002A`
Specification references: JSON data model and grammar from RFC 8259, as used by JSON Schema
Target modules: `parser-core`, `runtime-core`

Allowed files:
- `modules/parser-core/src/main/java/io/github/mundanej/mjjb/parser/**`
- `modules/parser-core/src/test/java/io/github/mundanej/mjjb/parser/**`
- `docs/architecture/parser-architecture.md`

Forbidden files:
- Generator emitters and schema binding logic.

Expected behavior:
- `JsonStreamReader` handles JSON object, array, string, number, boolean, and null tokens.
- Reader-backed parsing is incremental and does not eagerly read full input.
- Parser diagnostics include stable codes and input locations.
- Parser enforces valid JSON string escape, number, literal, and container syntax.

Tests to add/update:
- Parser unit tests for grammar edges, nested values, invalid escapes, invalid numbers, location tracking, and large streaming arrays.
- Fuzz/property tests may be added under the `fuzz` tag but must not run in default `check`.

Documentation to update:
- Parser architecture with supported grammar and known limits.

Commands to run:
- `./gradlew :modules:parser-core:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Parser accepts representative valid JSON documents needed by generated readers.
- Parser rejects invalid JSON deterministically.
- No third-party parser dependency is introduced.

Rollback notes:
- Revert parser-core changes and parser tests from this task.
