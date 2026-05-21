# Architecture Gates

Architecture checks run through ordinary module `check` tasks. The default
release-quality command is:

```bash
./gradlew qualityGate --console=plain
```

For focused architecture work, run the affected module checks directly. Example:

```bash
./gradlew :modules:runtime-core:check :modules:parser-core:check :modules:generator-core:check --console=plain
```

## Diagnosing Failures

ArchUnit failures report the violated rule name and the dependency, field, or
method that broke it. Fix the production dependency when it contradicts the
module boundary. If a failure is caused by a legitimate test harness behavior,
keep the code in `src/test` so production-class analysis does not include it.

Generated-source boundary failures are reported by `generator-core` tests. The
fix is usually in the emitter that introduced a forbidden import or runtime
dependency.

## Adding Coverage

Add architecture rules in the module whose production classes own the boundary.
Document the rule in `docs/architecture/architecture-rules.md`, then run the
module's `check` task and the full `qualityGate`.
