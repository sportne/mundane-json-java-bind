# TASK-0020: Tagged `oneOf` Support

Task ID: `TASK-0020`
Status: `draft`
Gate: Object shape expansion
Depends on: `TASK-0019`
Specification references: JSON Schema Draft 2020-12 Applicator keyword `oneOf`
Target modules: `schema-model`, `generator-core`, `runtime-core`, `examples/tagged-oneof`

Allowed files:
- `modules/schema-model/**`
- `modules/generator-core/**`
- `modules/runtime-core/**`
- `modules/conformance-tests/**`
- `examples/tagged-oneof/**`
- `docs/architecture/generated-code-contract.md`
- `docs/architecture/validation-architecture.md`

Forbidden files:
- Untagged union inference.
- Runtime polymorphic binding.
- Reflection, annotation scanning, or subtype discovery.

Expected behavior:
- Profile accepts only the documented tagged `oneOf` form.
- Generator emits explicit switch-based dispatch on the tag property.
- Generated readers reject missing, duplicate, unknown, or ambiguous tags deterministically.
- Generated validators enforce exactly one matching branch within the supported tagged form.

Tests to add/update:
- Profile tests for accepted tagged forms and rejected generic `oneOf` forms.
- Golden source tests for generated tag dispatch.
- Generated behavior tests for each branch and each rejection path.
- Tagged oneOf example build and behavior tests.

Documentation to update:
- Generated-code contract tagged `oneOf` section.
- Validation architecture branch matching semantics.

Commands to run:
- `./gradlew :modules:schema-model:check :modules:generator-core:check :examples:tagged-oneof:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- The supported tagged `oneOf` form works through generated reader, writer, validator, CLI, Gradle plugin, and example code.

Rollback notes:
- Revert tagged union IR, emitters, example, and tests.
