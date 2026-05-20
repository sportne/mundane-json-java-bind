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

Programmatic generator requests can opt in to an additional
`ExampleBindingJsonSchemaMetadata.java` helper. Metadata helpers expose generated
schema facts for documentation and diagnostics; readers, writers, validators,
and model construction do not depend on them.

## Gradle Plugin

Gradle projects can generate the same basic object binding slice during Java
compilation:

```groovy
plugins {
  id 'java'
  id 'io.github.mundanej.mjjb'
}

dependencies {
  implementation 'io.github.mundanej:mjjb-runtime-core:0.1.0-SNAPSHOT'
}

mjjb {
  schema('src/main/schema/example.schema.json')
  outputDirectory.set(layout.buildDirectory.dir('generated/sources/mjjb/main/java'))
  defaultPackage.set('com.example.generated')
  rootTypeName.set('ExampleBinding')
  profile.set('JSP-DATA-2020-12')
}
```

When the Java plugin is present, `compileJava` depends on `generateMjjb` and
compiles the generated source directory.

## Basic Example

The checked-in basic record example demonstrates the completed first binding
slice: schema, generated model, reader, validator, writer, and deterministic
failure diagnostics.

```bash
./gradlew :examples:basic-record:check --console=plain
```

The example schema lives at
`examples/basic-record/src/main/schema/basic-record.schema.json`. Its generated
sources are checked in under `examples/basic-record/generated-src/main/java`,
and conformance tests verify they match current generator output.

## Tagged oneOf Example

The checked-in tagged `oneOf` example demonstrates the narrow supported union
slice: a root sealed interface with nested branch records, tag-first generated
reading and writing, validation dispatch, and deterministic tag diagnostics.

```bash
./gradlew :examples:tagged-oneof:check --console=plain
```

The example schema lives at
`examples/tagged-oneof/src/main/schema/tagged-oneof.schema.json`. Its generated
sources are checked in under `examples/tagged-oneof/generated-src/main/java`,
and conformance tests verify they match current generator output.
