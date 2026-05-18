# Architecture Rule Catalog

Rules in this catalog should be mechanically enforced with ArchUnit where
practical.

| Rule | Rationale |
|---|---|
| Runtime modules must not depend on generator, CLI, Gradle plugin, examples, conformance, or testing-support packages. | Runtime and generated paths stay small and stable. |
| Generated-code-facing APIs must not expose parser implementation internals. | Generated readers target project-owned interfaces, not implementation classes. |
| `generator-api` must not expose frontend, IR, binding, emitter, parser, or Gradle implementation types. | Public generator API remains stable and narrow. |
| Runtime and generated-code paths must not use reflection, `java.lang.invoke`, dynamic proxies, ServiceLoader, class loaders, runtime scanning, JNI, `Unsafe`, serialization streams, process spawning, finalizers, or mutable public statics. | Native Image compatibility and deterministic behavior are core constraints. |
| Generated source must not depend on third-party libraries. | Generated bindings remain readable, portable, and reachability-safe. |
