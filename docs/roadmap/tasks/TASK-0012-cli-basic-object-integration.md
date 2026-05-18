# TASK-0012: CLI Integration for Basic Object Binding

Task ID: `TASK-0012`
Status: `draft`
Gate: User tooling
Depends on: `TASK-0011`
Specification references: JSON Schema Draft 2020-12 Core schema resource identification and Validation object keywords
Target modules: `generator-cli`, `generator-api`, `generator-core`

Allowed files:
- `modules/generator-cli/**`
- `modules/generator-api/**`
- `modules/generator-core/src/test/**`
- `docs/architecture/cli.md`
- `README.md`

Forbidden files:
- Gradle plugin implementation.
- New schema keyword support beyond the basic object binding milestone.

Expected behavior:
- CLI accepts schema path, output directory, package name, root type name, and profile token.
- CLI writes deterministic generated source for the basic object binding milestone.
- CLI exits non-zero with deterministic diagnostics for invalid input or unsupported schema features.

Tests to add/update:
- CLI functional tests for successful generation.
- CLI diagnostic tests for missing files, invalid package names, and unsupported keywords.
- Golden output comparison for generated source files.

Documentation to update:
- README basic CLI usage.
- CLI architecture notes.

Commands to run:
- `./gradlew :modules:generator-cli:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- A user can generate and compile the basic object binding milestone using only the CLI.

Rollback notes:
- Revert CLI command wiring and tests from this task.
