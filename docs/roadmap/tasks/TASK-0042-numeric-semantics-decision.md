# TASK-0042: Numeric Semantics Decision

Task ID: `TASK-0042`
Status: `complete`
Gate: Post-v1 hardening and simplification
Depends on: `TASK-0041`
Specification references: JSON Schema Draft 2020-12 numeric validation semantics; `docs/standards-baseline.md`; `docs/architecture/validation-architecture.md`
Target modules: Documentation and possible ADR

Allowed files:
- `docs/adr/**`
- `docs/architecture/validation-architecture.md`
- `docs/architecture/generated-code-contract.md`
- `docs/standards-baseline.md`
- `docs/supported-profile.md`
- `docs/roadmap/tasks/TASK-0042-numeric-semantics-decision.md`
- Focused tests only if the decision requires documenting current behavior with regression coverage

Forbidden files:
- Implementing a new numeric binding mode in this task.
- Changing generated numeric Java types without a follow-up implementation task.
- Weakening documented JSON Schema traceability.

Expected behavior:
- Decide whether `number` remains `double` only for the current profile or whether a future exact decimal binding mode should be planned.
- Document the tradeoff between simplicity, JSON Schema numeric precision, generated-code ergonomics, performance, and Native Image friendliness.
- If the decision changes the project contract, record it as an ADR.
- Identify follow-up implementation tasks only in prose unless the decision explicitly says to add task files later.

Tests to add/update:
- Add focused tests only if existing coverage does not lock down the documented current numeric behavior.

Documentation to update:
- Update numeric notes in supported profile, standards baseline, generated-code contract, and validation architecture as needed.
- Add an ADR if the decision is load-bearing.
- Update this task status to `complete`.

Commands to run:
- `./gradlew :modules:schema-model:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- The numeric semantics decision is explicit, documented, and consistent across profile and architecture docs.
- Follow-up work is clearly separated from the decision task.
- Required gates pass.

Rollback notes:
- Revert ADR/docs/test changes and task status.
