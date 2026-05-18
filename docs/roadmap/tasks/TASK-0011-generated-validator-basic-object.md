# TASK-0011: Generated Validator for Basic Objects

Task ID: `TASK-0011`
Status: `complete`
Gate: First round trip
Depends on: `TASK-0010`
Specification references: JSON Schema Draft 2020-12 Validation keywords `type`, `required`, `properties`, `additionalProperties`
Target modules: `generator-core`, `runtime-core`

Allowed files:
- `modules/generator-core/**`
- `modules/runtime-core/src/main/java/io/github/mundanej/mjjb/runtime/Validation*.java`
- `modules/runtime-core/src/test/**`
- `docs/architecture/validation-architecture.md`

Forbidden files:
- Facet, array, and oneOf validation expansion.

Expected behavior:
- Generator emits one validator class per generated root model.
- Validators support accumulate and fail-fast modes.
- Object validation reports stable error codes and JSON instance paths.

Tests to add/update:
- Golden validator source tests.
- Generated validator compile and behavior tests.
- Accumulate and fail-fast mode tests.

Documentation to update:
- Validation architecture and generated-code contract.

Commands to run:
- `./gradlew :modules:runtime-core:check :modules:generator-core:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Basic object generated model/reader/writer/validator slice is complete and executable on JVM.

Rollback notes:
- Revert validator emitter and validation runtime changes.
