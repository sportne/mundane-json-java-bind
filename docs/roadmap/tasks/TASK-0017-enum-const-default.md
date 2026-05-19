# TASK-0017: `enum`, `const`, and `default`

Task ID: `TASK-0017`
Status: `complete`
Gate: Collections and value constraints
Depends on: `TASK-0016`
Specification references: JSON Schema Draft 2020-12 Validation keywords `enum`, `const`, `default`
Target modules: `schema-model`, `generator-core`, `runtime-core`

Allowed files:
- `modules/schema-model/**`
- `modules/generator-core/**`
- `modules/runtime-core/**`
- `modules/conformance-tests/**`
- `docs/architecture/generated-code-contract.md`
- `docs/architecture/validation-architecture.md`

Forbidden files:
- Java enum generation for arbitrary JSON values unless the binding contract is explicitly updated.
- Runtime schema interpretation for defaults.

Expected behavior:
- `enum` and `const` constraints are represented in the binding IR.
- Generated validators check accepted scalar and null literal values.
- `default` is emitted as optional generated metadata or constructor helper behavior only when the generated-code contract defines it explicitly.
- Defaults never hide missing required field validation.

Tests to add/update:
- Profile tests for accepted literal sets and rejected unsupported shapes.
- Golden source tests for enum, const, and default emission.
- Generated validator behavior tests for matching and non-matching values.

Documentation to update:
- Generated-code contract default semantics.
- Validation architecture literal constraint behavior.

Commands to run:
- `./gradlew :modules:schema-model:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Literal constraints and documented default behavior are deterministic in generated code.

Rollback notes:
- Revert literal constraint and default handling changes.
