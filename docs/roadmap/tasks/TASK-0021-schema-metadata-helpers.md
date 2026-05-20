# TASK-0021: Optional Schema Metadata Helpers

Task ID: `TASK-0021`
Status: `complete`
Gate: Object shape expansion
Depends on: `TASK-0020`
Specification references: JSON Schema Draft 2020-12 Core identifiers and Validation annotations used by the accepted profile
Target modules: `generator-api`, `generator-core`, `runtime-core`

Allowed files:
- `modules/generator-api/**`
- `modules/generator-core/**`
- `modules/runtime-core/**`
- `modules/conformance-tests/**`
- `docs/architecture/generated-code-contract.md`

Forbidden files:
- Runtime schema interpretation.
- Runtime dependency on generated metadata for reader, writer, or validator correctness.
- Dynamic metadata discovery.

Expected behavior:
- Metadata helper generation is opt-in through generator configuration.
- Helpers expose compile-time generated schema facts useful for diagnostics or documentation.
- Runtime binding behavior remains independent of metadata helper presence.
- Helper output is deterministic and human-readable.

Tests to add/update:
- Generator API configuration tests.
- Golden source tests for metadata helpers.
- Compile tests with metadata enabled and disabled.
- Architecture tests confirming runtime/generated binding behavior does not require metadata scanning.

Documentation to update:
- Generated-code contract metadata helper section.
- README note describing metadata as optional.

Commands to run:
- `./gradlew :modules:generator-api:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Optional metadata helpers can be generated without changing runtime binding semantics or Native Image requirements.

Rollback notes:
- Revert metadata configuration, emitters, and tests.
