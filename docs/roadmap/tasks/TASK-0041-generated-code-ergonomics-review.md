# TASK-0041: Generated-Code Ergonomics Review

Task ID: `TASK-0041`
Status: `complete`
Gate: Post-v1 hardening and simplification
Depends on: `TASK-0040`
Specification references: `docs/architecture/generated-code-contract.md`; `README.md`; examples under `examples/**`
Target modules: `generator-core`, examples, documentation

Allowed files:
- `docs/architecture/**`
- `docs/verification/**`
- `README.md`
- `examples/**`
- `modules/generator-core/src/generatedCodeSmoke/**`
- `docs/roadmap/tasks/TASK-0041-generated-code-ergonomics-review.md`

Forbidden files:
- Public API breaking changes.
- Generated-code behavior changes without explicit tests and documentation.
- New external runtime dependencies.
- Schema profile expansion.

Expected behavior:
- Review generated model, reader, writer, validator, and metadata helper usability against realistic supported schemas.
- Identify confusing naming, construction, validation, map, nested record, and metadata workflows.
- Add or update examples only when they clarify an existing supported workflow.
- Produce a concise ergonomics report with prioritized follow-up candidates.

Tests to add/update:
- If examples or generated fixtures change, add or update focused smoke tests.
- Existing generated-source verification must remain green.

Documentation to update:
- Add an ergonomics review report under `docs/verification/`.
- Update README or examples only for concrete usability gaps.
- Update this task status to `complete`.

Commands to run:
- `./gradlew :modules:generator-core:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- The review covers at least one realistic nested/ref/map/composition schema workflow.
- Follow-up candidates distinguish easy documentation fixes from deeper generated-code contract decisions.
- Required gates pass.

Rollback notes:
- Revert review docs, example updates, fixture updates, and task status.
