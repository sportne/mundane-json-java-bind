# TASK-0027: Release and Publication Readiness

Task ID: `TASK-0027`
Status: `draft`
Gate: Completion hardening
Depends on: `TASK-0026`
Specification references: Project licensing, publication, and reproducibility requirements
Target modules: all published modules

Allowed files:
- `build.gradle`
- `settings.gradle`
- `build-logic/**`
- `gradle/**`
- `modules/**`
- `docs/verification/release.md`
- `LICENSE`
- `NOTICE`

Forbidden files:
- Feature changes unrelated to publication readiness.
- License text changes that diverge from BSD 3-Clause project standard.

Expected behavior:
- Published artifacts have stable coordinates, descriptions, licenses, and source/javadoc artifacts.
- BOM aligns all public module versions.
- Release build is reproducible and excludes local/generated scratch outputs.
- `.gitignore` supports `git add .` without staging build products or local caches.
- License and notice metadata match the repository BSD 3-Clause text.

Tests to add/update:
- Publication metadata verification.
- BOM dependency alignment verification.
- Release dry-run task where practical.

Documentation to update:
- Release verification guide.
- README artifact coordinates.

Commands to run:
- `./gradlew qualityGate --console=plain`
- Release dry-run command documented by this task.

Acceptance criteria:
- The project can produce release-ready artifacts with correct metadata and without unrelated local files.

Rollback notes:
- Revert publication metadata and release-task changes.
