# TASK-0031: Local `$defs` and `$ref` Resolution

Task ID: `TASK-0031`
Status: `complete`
Gate: Post-v1 profile expansion
Depends on: `TASK-0030`
Specification references: JSON Schema Draft 2020-12 Core schema resources, `$defs`, `$ref`, and JSON Pointer fragments
Target modules: `schema-model`, `generator-core`, `conformance-tests`

Allowed files:
- `modules/schema-model/**`
- `modules/generator-core/**`
- `modules/conformance-tests/**`
- `docs/supported-profile.md`
- `docs/standards-baseline.md`
- `docs/architecture/generated-code-contract.md`
- `docs/architecture/error-reporting.md`

Forbidden files:
- Remote reference fetching.
- `$dynamicRef`, `$dynamicAnchor`, or vocabulary negotiation.
- Runtime reference resolution in generated readers, writers, validators, or metadata helpers.

Expected behavior:
- Same-document `$ref` values with JSON Pointer fragments resolve before binding model construction.
- `$defs` is accepted as the local definition container for reusable schemas.
- Resolved schemas produce the same generated Java behavior as equivalent inline schemas.
- Cycles, missing targets, invalid fragments, remote URIs, and dynamic references reject with stable diagnostics.

Tests to add/update:
- JSON Pointer resolution tests for root, `$defs`, escaped tokens, and nested schema locations.
- Profile/generator diagnostics for unsupported remote, missing, cyclic, and dynamic references.
- Golden source and generated behavior fixtures comparing inline and referenced nested object schemas.
- JSON Schema Test Suite trace updates for representative local and remote reference cases.

Documentation to update:
- Supported profile `$defs` / local `$ref` limits.
- Standards baseline recommended keyword list after implementation.
- Error-reporting notes for resolved schema locations.

Commands to run:
- `./gradlew :modules:schema-model:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Local references are resolved deterministically before source emission, preserve useful schema locations in diagnostics, and never require runtime schema lookup.

Rollback notes:
- Revert reference resolver, diagnostics, generated fixtures, conformance updates, and documentation updates.
