# TASK-0054: JsonStreamReader Chunked Fill

Task ID: `TASK-0054`
Status: `complete`
Gate: Performance and simplicity follow-up
Depends on: `TASK-0053`
Specification references: `docs/architecture/parser-architecture.md`; `docs/verification/performance.md`
Target modules: `parser-core`

Allowed files:
- `modules/parser-core/src/main/java/io/github/mundanej/mjjb/parser/**`
- `modules/parser-core/src/test/java/io/github/mundanej/mjjb/parser/**`
- `docs/architecture/parser-architecture.md`
- `docs/verification/performance.md`
- `docs/roadmap/tasks/TASK-0054-jsonstreamreader-chunked-fill.md`

Forbidden files:
- Runtime `JsonReader` interface changes.
- Parser diagnostic code changes.
- Generated reader changes.
- Sliding-window buffer compaction.
- Generic JSON object graph construction.

Expected behavior:
- `JsonStreamReader.fromReader(...)` fills input from a reusable char buffer instead of calling `Reader.read()` once per character.
- String-backed reader behavior remains unchanged.
- Character offsets, line/column tracking, parsed literals, and diagnostics remain stable.
- Consumed input may still be retained; bounded sliding-window buffering is explicitly out of scope.

Tests to add/update:
- Add Reader-backed tests for large inputs, long strings, numbers split across buffer fills, unicode escapes, arrays, and invalid JSON diagnostics.
- Add a custom `Reader` test double that proves reads are chunked rather than one character at a time.

Documentation to update:
- Update parser/performance docs only if wording about reader fill behavior changes.
- Update this task status to `complete` after implementation.

Commands to run:
- `./gradlew :modules:generator-core:check --console=plain`
- `./gradlew :modules:schema-model:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Reader-backed parsing uses chunked fill.
- Parser behavior and diagnostics remain stable.
- Required gates pass.

Rollback notes:
- Revert parser fill changes, tests, documentation, and this task status.
