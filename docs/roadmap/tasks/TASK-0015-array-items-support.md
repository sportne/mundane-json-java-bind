# TASK-0015: Array and `items` Support

Task ID: `TASK-0015`
Status: `draft`
Gate: Collections and value constraints
Depends on: `TASK-0014`
Specification references: JSON Schema Draft 2020-12 Validation keywords `array`, `items`, `minItems`, `maxItems`
Target modules: `schema-model`, `generator-core`, `runtime-core`, `parser-core`

Allowed files:
- `modules/schema-model/**`
- `modules/generator-core/**`
- `modules/runtime-core/**`
- `modules/parser-core/**`
- `modules/conformance-tests/**`
- `docs/architecture/generated-code-contract.md`
- `docs/architecture/validation-architecture.md`

Forbidden files:
- Tuple validation and `prefixItems`.
- Unevaluated item handling.

Expected behavior:
- Profile accepts homogeneous array schemas with `items`.
- Generated models use immutable `List<T>` defensive copies.
- Generated readers stream arrays without constructing generic object graphs.
- Generated writers preserve array order.
- Validators enforce `minItems` and `maxItems` with stable JSON paths.

Tests to add/update:
- Schema profile tests for accepted homogeneous arrays and rejected tuple forms.
- Golden source tests for array fields.
- Generated reader/writer/validator behavior tests.
- Parser streaming array tests where needed.

Documentation to update:
- Generated-code contract collection mapping.
- Validation architecture collection constraints.

Commands to run:
- `./gradlew :modules:schema-model:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Array fields with supported item schemas work through generated model, reader, writer, and validator code.

Rollback notes:
- Revert array IR, emitter, and validation changes from this task.
