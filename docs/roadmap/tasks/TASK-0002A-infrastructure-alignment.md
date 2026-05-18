# TASK-0002A: Infrastructure Alignment Before Parser Work

Task ID: `TASK-0002A`
Status: `complete`
Gate: Foundation
Depends on: `TASK-0001`
Specification references: Project governance and mundane sibling project build conventions
Target modules: build infrastructure and documentation

Allowed files:
- `build-logic/**`
- `config/**`
- `docs/roadmap/**`
- `docs/verification/**`
- `gradle.properties`
- `gradle/wrapper/**`

Forbidden files:
- Java production APIs and parser behavior.
- Module layout changes.
- CI workflow additions.

Expected behavior:
- Coverage verification uses one shared threshold policy for all Java projects
  with compiled production classes.
- Gradle defaults align with the sibling mundane project family where that
  alignment does not add unnecessary complexity.
- Small local infrastructure config and documentation files exist for ArchUnit,
  Checkstyle, Error Prone, JaCoCo, Spotless, and the Gradle wrapper.

Tests to add/update:
- No Java tests required.
- Build and design-control validation must pass.

Documentation to update:
- Roadmap ordering.
- Coverage policy.
- Local infrastructure config notes.

Commands to run:
- `./gradlew validateDesignControlPack --console=plain`
- `./gradlew qualityGate --console=plain`
- `git status --short --ignored`

Acceptance criteria:
- The parser task remains the next product implementation task.
- Infrastructure policy is simpler than the initial scaffold.
- `qualityGate` passes with the uniform coverage thresholds.

Rollback notes:
- Revert this task file, roadmap ordering changes, coverage convention changes,
  Gradle property changes, and added config documentation files.
