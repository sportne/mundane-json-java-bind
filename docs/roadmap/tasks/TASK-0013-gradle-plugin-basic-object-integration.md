# TASK-0013: Gradle Plugin Integration for Basic Object Binding

Task ID: `TASK-0013`
Status: `complete`
Gate: User tooling
Depends on: `TASK-0012`
Specification references: JSON Schema Draft 2020-12 Core schema resource identification and Validation object keywords
Target modules: `generator-gradle-plugin`, `generator-api`, `generator-core`

Allowed files:
- `modules/generator-gradle-plugin/**`
- `modules/generator-api/**`
- `modules/generator-core/src/test/**`
- `docs/architecture/gradle-plugin.md`
- `README.md`

Forbidden files:
- CLI behavior except shared API fixes.
- New schema keyword support beyond the basic object binding milestone.

Expected behavior:
- Plugin id `io.github.mundanej.mjjb` registers a cacheable generation task.
- Task inputs include schema files, package name, root type name, profile token, and output directory.
- Generated source directory is wired into Java compilation for consuming projects.
- Diagnostics are deterministic and surfaced as Gradle task failures.

Tests to add/update:
- Gradle TestKit generation test.
- Incremental/cacheable task input declaration test.
- Generated source compile test using the plugin output.

Documentation to update:
- README Gradle usage.
- Gradle plugin architecture notes.

Commands to run:
- `./gradlew :modules:generator-gradle-plugin:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- A Gradle sample project can generate and compile the basic object binding milestone through the plugin.

Rollback notes:
- Revert plugin task wiring and TestKit fixtures from this task.
