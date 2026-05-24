# TASK-0035: Object Validation Keywords

Task ID: `TASK-0035`
Status: `complete`
Gate: Post-v1 profile expansion
Depends on: `TASK-0034`
Specification references: JSON Schema Draft 2020-12 Validation keywords `minProperties`, `maxProperties`, `dependentRequired`, and Applicator keyword `propertyNames`
Target modules: `schema-model`, `generator-core`, `conformance-tests`

Allowed files:
- `modules/schema-model/**`
- `modules/generator-core/**`
- `modules/conformance-tests/**`
- `docs/supported-profile.md`
- `docs/standards-baseline.md`
- `docs/architecture/generated-code-contract.md`
- `docs/architecture/validation-architecture.md`

Forbidden files:
- `dependentSchemas` implementation.
- Generic schema evaluation for `propertyNames` beyond supported string facets.
- Runtime reflection or raw JSON object graph validation.

Expected behavior:
- Generated validators enforce `minProperties` and `maxProperties` for generated object and map-capable bindings.
- `propertyNames` is supported only when the key schema maps to supported string assertions.
- `dependentRequired` validates property-presence dependencies against generated model presence semantics.
- Reader and writer behavior remain unchanged; these are generated validator concerns unless a direct read-time violation is already represented by existing reader rules.

Tests to add/update:
- Profile tests for valid keyword shapes and rejected unsupported `propertyNames` schemas.
- Golden source and generated behavior probes for accumulate and fail-fast validation modes.
- JSON Schema Test Suite trace updates for representative object validation cases.
- SchemaStore corpus samples for `propertyNames` and dependent-required schemas where practical.

Documentation to update:
- Supported profile object validation keyword section.
- Validation architecture codes and path behavior.
- Standards baseline supported keyword list after implementation.

Commands to run:
- `./gradlew :modules:schema-model:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`
- `./gradlew schemaStoreCorpus --console=plain`
- `source "$HOME/.sdkman/bin/sdkman-init.sh" && ./gradlew nativeSmoke --console=plain`
- `./gradlew releaseDryRun --console=plain`

Acceptance criteria:
- Object validation keywords generate deterministic validation errors with stable codes, correct instance paths, and no runtime schema interpretation.

Rollback notes:
- Revert object validation IR, emitter changes, tests, conformance changes, and documentation updates.
