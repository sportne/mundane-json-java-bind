# Architecture Rules

This document maps the project architecture rules to automated checks. The
rules are enforced by module-local ArchUnit tests that run as part of each
module's `check` task and by `qualityGate`.

## Production Boundary Rules

| Area | Enforced rule |
|---|---|
| `runtime-core` | Runtime code must not depend on parser, schema, generator, CLI, Gradle plugin, examples, conformance, or testkit packages. |
| `parser-core` | Parser code may depend only on parser packages, `runtime-core`, and JDK types. |
| `schema-model` | Schema model code may depend only on schema-model packages, `runtime-core`, and JDK types. |
| `generator-api` | Public generator API code may depend only on generator-api packages and JDK types. |
| `generator-core` | Generator implementation code may depend on generator-core, generator-api, schema-model, runtime-core, and JDK types. |
| `generator-cli` | CLI code may depend on generator API/core packages and JDK types. |
| `generator-gradle-plugin` | Gradle plugin code may depend on generator API/core, Gradle APIs, and JDK types. |
| `testing-support` | Published test helpers may depend only on testkit packages, `runtime-core`, and JDK types. |

## Native-Friendly Runtime Rules

Production classes in runtime-facing modules must not use reflection,
`java.lang.invoke`, dynamic proxies, `ServiceLoader`, class loader APIs,
serialization streams, process spawning APIs, JNI/native methods, `Unsafe`, or
finalizers. Public static fields must be final.

CLI and Gradle plugin production code also rejects reflection and process
spawning. Test harnesses are intentionally out of scope because generated-source
verification and conformance tests compile and load generated classes.

## Generated Source Rules

Generated production source must depend only on JDK and `runtime-core` types. It
must not import or reference parser implementation classes, generator internals,
schema-model, reflection, dynamic proxies, `ServiceLoader`, or third-party
libraries. `generator-core` includes a regression test that generates a
representative binding and inspects every emitted source file.

## Adding Rules

Add new rules as module-local ArchUnit tests when they protect a module
boundary. Keep exceptions test-scoped where possible; production exceptions must
be documented here with the reason and the owning task.
