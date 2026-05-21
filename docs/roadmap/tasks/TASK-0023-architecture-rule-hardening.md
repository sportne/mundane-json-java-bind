# TASK-0023: Architecture Rule Hardening

Task ID: `TASK-0023`
Status: `complete`
Gate: Completion hardening
Depends on: `TASK-0022`
Specification references: Project architecture charter and Native Image constraints
Target modules: all modules

Allowed files:
- `build-logic/**`
- `modules/**/src/test/**`
- `modules/**/src/archTest/**`
- `docs/architecture/architecture-rules.md`
- `docs/verification/**`

Forbidden files:
- Runtime feature changes except fixes required by failing architecture rules.

Expected behavior:
- Architecture tests forbid reflection, `java.lang.invoke`, dynamic proxies, `ServiceLoader`, class loader tricks, runtime scanning, JNI, `Unsafe`, serialization streams, process spawning, finalizers, mutable public statics, runtime-to-generator dependencies, and parser implementation leakage into generated code.
- Rules are split where useful by runtime, parser, generator, generated-code, CLI, and Gradle plugin boundaries.
- Violations produce actionable messages.

Tests to add/update:
- ArchUnit tests for every forbidden behavior category.
- Positive boundary tests showing allowed dependencies.
- Regression fixture for generated code dependency boundaries.

Documentation to update:
- Architecture rules reference.
- Verification guide for architecture gates.

Commands to run:
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- Forbidden architecture behaviors are enforced automatically across the project.

Rollback notes:
- Revert architecture rule additions and any rule-driven production fixes.
