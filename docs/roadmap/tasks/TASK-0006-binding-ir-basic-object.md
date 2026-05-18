# TASK-0006: Binding IR for Basic Objects

Task ID: `TASK-0006`
Status: `complete`
Gate: First binding slice
Depends on: `TASK-0005`
Specification references: JSON Schema Draft 2020-12 Validation keywords `type`, `properties`, `required`, `additionalProperties`
Target modules: `generator-core`

Allowed files:
- `modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/**`
- `modules/generator-core/src/test/java/io/github/mundanej/mjjb/generator/core/**`
- `docs/architecture/generated-code-contract.md`

Forbidden files:
- CLI and Gradle plugin behavior except tests that consume public API results.

Expected behavior:
- Generator builds a binding IR for object schemas with scalar properties.
- IR captures Java package, type name, field names, requiredness, scalar type mapping, and source schema pointers.
- `additionalProperties:false` is required for v1 object generation.

Tests to add/update:
- Binding IR tests for scalar fields, required fields, optional fields, naming collisions, and unsupported additional properties.

Documentation to update:
- Generated-code contract with initial type mapping table.

Commands to run:
- `./gradlew :modules:generator-core:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Supported basic object schemas produce deterministic IR.
- Unsupported object shapes stop before emission with exact diagnostics.

Rollback notes:
- Remove binding IR classes and tests from this task.
