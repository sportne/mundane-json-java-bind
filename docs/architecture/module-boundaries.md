# Module Boundaries

| Module | Responsibility | Runtime dependency policy |
|---|---|---|
| `runtime-core` | JSON paths, locations, diagnostics, validation values, reader/writer interfaces, nullable field state. | No third-party dependencies. |
| `parser-core` | Dependency-free streaming JSON tokenizer/parser and writer implementation. | Depends only on `runtime-core`. |
| `schema-model` | Draft 2020-12 subset model, vocabulary/profile tokens, schema syntax parsing, schema locations, JSON Pointer helpers. | Depends only on `runtime-core`; does not depend on runtime JSON parser implementations. |
| `generator-api` | Public immutable generator request/result/profile API. | No implementation parser or IR exposure. |
| `generator-core` | Schema frontend, profile validation, binding IR, deterministic Java emitters. | Generator-only dependencies may not leak to generated code. |
| `generator-cli` | CLI entry point. | Depends on generator API/core only. |
| `generator-gradle-plugin` | Cacheable Gradle integration. | Gradle API plus generator API/core. |
| `testing-support` | User-facing generated-binding test helpers. | Test support only. |
| `conformance-tests` | Internal conformance harness and official test-suite allowlist evidence. | Not published. |

## Forbidden Dependencies

- Runtime modules must not depend on generator, CLI, Gradle plugin, examples, or
  conformance modules.
- Generated code must not depend on generator modules, schema-model, parser
  implementation internals, or third-party libraries.
- `generator-api` must not expose frontend, IR, binding, emitter, parser, or
  Gradle types.
