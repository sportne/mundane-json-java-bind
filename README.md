# mundane-json-java-bind

`mundane-json-java-bind` is a JSON Schema driven Java binding and code generation
project.

The project generates explicit Java 21 source for model types, JSON readers,
JSON writers, and validators from a strict JSON Schema Draft 2020-12 subset. It
does not provide runtime object mapping, reflection-based binding, annotation
scanning, dynamic class loading, or a plugin framework.

## Design Principles

- Explicit generated code over runtime reflection.
- GraalVM Native Image friendliness first.
- Tiny runtime dependency footprint.
- Deterministic generation, diagnostics, and generated source ordering.
- Human-readable generated Java source.
- Boring, maintainable engineering over framework magic.

## Specification Baseline

The truth-driving specification inputs are:

- JSON Schema Draft 2020-12 Core: <https://json-schema.org/draft/2020-12/json-schema-core.html>
- JSON Schema Draft 2020-12 Validation: <https://json-schema.org/draft/2020-12/json-schema-validation>
- JSON Schema Draft 2020-12 default meta-schema: <https://json-schema.org/draft/2020-12/schema>
- JSON Schema Test Suite: <https://github.com/json-schema-org/JSON-Schema-Test-Suite>

The v1 profile is intentionally partial. Unsupported JSON Schema features must
fail with deterministic diagnostics and exact schema locations.

## Build

```bash
./gradlew qualityGate --console=plain
```

Native Image smoke tests are separate because they require a GraalVM
`native-image` toolchain:

```bash
./gradlew nativeSmoke --console=plain
```

## CLI

The CLI can generate the current basic object binding slice from a supported
JSON Schema object:

```bash
mjjb generate \
  --schema schema.json \
  --out build/generated/sources/mjjb \
  --package com.example.generated \
  --root-type ExampleBinding \
  --profile JSP-DATA-2020-12
```

The command writes `ExampleBinding.java`, `ExampleBindingJsonWriter.java`,
`ExampleBindingJsonReader.java`, and `ExampleBindingJsonValidator.java` under
the requested package directory.
