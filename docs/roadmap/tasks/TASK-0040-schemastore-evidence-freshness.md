# TASK-0040: SchemaStore Evidence Freshness

Task ID: `TASK-0040`
Status: `ready-for-implementation`
Gate: Post-v1 hardening and simplification
Depends on: `TASK-0039`
Specification references: `docs/verification/schemastore-corpus.md`; `docs/supported-profile.md`
Target modules: `conformance-tests`

Allowed files:
- `modules/conformance-tests/src/schemaStoreCorpusTest/**`
- `docs/verification/schemastore-corpus.md`
- `docs/roadmap/tasks/TASK-0040-schemastore-evidence-freshness.md`

Forbidden files:
- JSON Schema feature expansion.
- Silent manifest digest rewrites.
- Committing downloaded schemas or generated corpus output under `build/`.

Expected behavior:
- Reconcile committed SchemaStore documentation with the current sidecar report.
- Make corpus aggregate counts harder to drift from the harness output.
- Review whether post-1.1 support changes should move any manifest entries from expected rejection to generated evidence.
- Preserve explicit digest drift handling and refresh-mode behavior.

Tests to add/update:
- Add or update tests if needed to make report aggregate totals mechanically verifiable.
- Run the sidecar corpus lane.

Documentation to update:
- Update `docs/verification/schemastore-corpus.md` with current counts and any changed interpretation.
- Update this task status to `complete`.

Commands to run:
- `./gradlew schemaStoreCorpus --console=plain`
- `./gradlew :modules:conformance-tests:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Documentation and generated sidecar report agree on aggregate corpus outcomes.
- Any manifest expectation changes are justified by current supported-profile behavior.
- Required gates pass.

Rollback notes:
- Revert manifest/report harness changes, documentation updates, and task status.
