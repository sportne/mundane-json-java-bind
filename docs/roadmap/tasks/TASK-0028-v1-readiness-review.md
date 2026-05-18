# TASK-0028: v1 Readiness Review

Task ID: `TASK-0028`
Status: `draft`
Gate: Final gate
Depends on: `TASK-0027`
Specification references: JSON Schema Draft 2020-12 Core, Validation, default meta-schema, and official test suite
Target modules: all modules

Allowed files:
- `README.md`
- `docs/**`
- `modules/**`
- `examples/**`
- `build-logic/**`
- `.github/workflows/**`

Forbidden files:
- New feature scope.
- Compatibility-breaking API changes unless required to satisfy an existing v1 contract.

Expected behavior:
- All roadmap acceptance criteria are complete or explicitly deferred with maintainer approval.
- Supported profile behavior is traceable to official JSON Schema sources.
- Unsupported features have deterministic diagnostics and documented skip reasons.
- Quality, architecture, generated-source, conformance, example, and Native Image smoke gates pass.
- Repository contains no known generated scratch files or local-only artifacts.

Tests to add/update:
- Final regression tests for any readiness defects.
- Missing coverage for documented v1 behavior.

Documentation to update:
- Roadmap statuses.
- Release notes or v1 readiness report.

Commands to run:
- `./gradlew qualityGate --console=plain`
- Native smoke command documented by `TASK-0024`
- Release dry-run command documented by `TASK-0027`

Acceptance criteria:
- Maintainer can tag v1 knowing the project is narrow, deterministic, documented, tested, and Native Image friendly.

Rollback notes:
- Revert only readiness fixes that caused regressions; keep the readiness report history.
