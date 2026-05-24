# TASK-0039: External Issue Mining

Task ID: `TASK-0039`
Status: `ready-for-implementation`
Gate: Post-v1 hardening and simplification
Depends on: `TASK-0038`
Specification references: `docs/supported-profile.md`; `docs/standards-baseline.md`; ADR-0001 JSON Schema Draft 2020-12 baseline
Target modules: Documentation and test planning

Allowed files:
- `docs/verification/**`
- `docs/architecture/**`
- `docs/roadmap.md`
- `docs/roadmap/tasks/TASK-0039-external-issue-mining.md`
- Test files only when adding narrow probes for confirmed project risks

Forbidden files:
- Broad feature implementation.
- Issue tracker mutations outside this repository.
- Vendoring external project test suites.
- Runtime schema interpretation.

Expected behavior:
- Mine issue trackers for functionally similar JSON Schema to code generation tools.
- Classify recurring failure modes relevant to this project, including references, composition, `additionalProperties`, discriminator naming, large-schema memory behavior, and generated-code ergonomics.
- Map each relevant failure mode to current project coverage, an explicit non-risk, or a recommended future task.
- Add narrow regression probes only for risks that are cheap to reproduce and already fit the supported profile.

Tests to add/update:
- Add focused tests only for mined risks that map to existing supported behavior and lack coverage.
- Otherwise document proposed probes without changing executable behavior.

Documentation to update:
- Add an external issue-mining report under `docs/verification/`.
- Update this task status to `complete`.

Commands to run:
- `./gradlew qualityGate --console=plain`
- If tests are added: run the affected module `check` task before `qualityGate`.

Acceptance criteria:
- The report cites the reviewed projects/issues and explains the impact on this project.
- Each mined risk is classified as covered, not applicable, needs probe, or needs future design.
- Required gates pass.

Rollback notes:
- Revert the report, task status update, and any focused probes.
