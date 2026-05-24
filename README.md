# mundane-json-java-bind

`mundane-json-java-bind` generates explicit Java 21 binding source from a
narrow JSON Schema Draft 2020-12 profile.

The generated source includes model types, JSON readers, JSON writers,
validators, and optional schema metadata helpers. It does not provide runtime
object mapping, reflection-based binding, annotation scanning, dynamic class
loading, schema interpretation, or a plugin framework.

## Why It Exists

- Generate readable Java source instead of runtime reflection.
- Keep generated bindings friendly to GraalVM Native Image.
- Keep the default runtime path free of third-party dependencies.
- Preserve deterministic generation, diagnostics, and source ordering.
- Support a deliberately small JSON Schema profile before widening scope.

## Quickstart From Source

Build and verify the repository with the default local gate:

```bash
./gradlew qualityGate --console=plain
```

Run the checked-in examples:

```bash
./gradlew :examples:basic-record:check --console=plain
./gradlew :examples:tagged-oneof:check --console=plain
```

Native Image smoke tests are separate because they require a GraalVM Java 21
toolchain with `native-image`:

```bash
./gradlew nativeSmoke --console=plain
```

## Supported Profile

The v1 profile token is `JSP-DATA-2020-12`. It accepts closed root object
bindings and a narrow tagged `oneOf` root form. Supported object fields include:

- scalar `string`, `integer`, `number`, and `boolean` values;
- homogeneous scalar arrays with `items`;
- optional fields through omitted entries in `required`;
- nullable field-level type arrays such as `["null", "string"]`;
- scalar and array-item facets including length, pattern, format, item count,
  and numeric bounds;
- scalar `enum`, `const`, and `default` annotation handling;
- accepted annotations such as `title`, `description`, `$comment`, `examples`,
  `deprecated`, `readOnly`, and `writeOnly`.

Unsupported Draft 2020-12 features fail during generation with deterministic
generator diagnostics and exact schema JSON Pointer locations. The detailed
profile contract is documented in
[`docs/supported-profile.md`](docs/supported-profile.md), with standards
traceability in [`docs/standards-baseline.md`](docs/standards-baseline.md).

## CLI

The CLI generates Java source from supported schemas:

```bash
mjjb generate \
  --schema schema.json \
  --out build/generated/sources/mjjb \
  --package com.example.generated \
  --root-type ExampleBinding \
  --profile JSP-DATA-2020-12
```

The command writes:

- `ExampleBinding.java`
- `ExampleBindingJsonWriter.java`
- `ExampleBindingJsonReader.java`
- `ExampleBindingJsonValidator.java`

The files are written under the requested package directory. Successful
generation prints one `Generated <path>` line per emitted source. Diagnostics
are written to standard error in the shared manifest-line format. CLI behavior
and exit codes are documented in [`docs/architecture/cli.md`](docs/architecture/cli.md).

Programmatic generator requests can opt in to an additional
`ExampleBindingJsonSchemaMetadata.java` helper. Metadata helpers expose
generated schema facts for documentation and diagnostics; models, readers,
writers, validators, and model construction do not depend on them.

## Gradle Plugin

Gradle projects can generate bindings during Java compilation:

```groovy
plugins {
  id 'java'
  id 'io.github.mundanej.mjjb'
}

dependencies {
  implementation 'io.github.mundanej:mjjb-runtime-core:1.1.0'
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
compiles the generated source directory. The plugin does not apply Java and
does not add runtime dependencies for the consuming project. See
[`docs/architecture/gradle-plugin.md`](docs/architecture/gradle-plugin.md).

## GitHub Release Artifacts

The current release version is `1.1.0`. This project publishes artifacts only
as GitHub Release assets, not to Maven Central. Download the
`mjjb-1.1.0-maven-repository.zip` asset from the `v1.1.0` GitHub release,
verify its SHA-256 sidecar if desired, unzip it, and point your build at the
unpacked local Maven repository.

The v1 Maven coordinates are:

| Artifact | Coordinate |
|---|---|
| BOM | `io.github.mundanej:mjjb-bom` |
| Runtime API | `io.github.mundanej:mjjb-runtime-core` |
| Parser and writer implementation | `io.github.mundanej:mjjb-parser-core` |
| Schema model | `io.github.mundanej:mjjb-schema-model` |
| Generator API | `io.github.mundanej:mjjb-generator-api` |
| Generator implementation | `io.github.mundanej:mjjb-generator-core` |
| CLI | `io.github.mundanej:mjjb-cli` |
| Gradle plugin | `io.github.mundanej:mjjb-gradle-plugin` |
| Generated-binding test helpers | `io.github.mundanej:mjjb-testing-support` |

Release dry-run verification is documented in
[`docs/verification/release.md`](docs/verification/release.md). Release notes
are tracked in [`docs/release-notes.md`](docs/release-notes.md).

## Examples

The basic record example demonstrates a closed object schema with required and
optional scalar fields:

```bash
./gradlew :examples:basic-record:check --console=plain
```

The example lives in [`examples/basic-record`](examples/basic-record), with its
schema at
[`examples/basic-record/src/main/schema/basic-record.schema.json`](examples/basic-record/src/main/schema/basic-record.schema.json)
and checked-in generated sources under
[`examples/basic-record/generated-src/main/java`](examples/basic-record/generated-src/main/java).

The tagged `oneOf` example demonstrates a root sealed interface with nested
branch records, tag-first generated reading and writing, validation dispatch,
arrays, nullable fields, literal constraints, and deterministic tag diagnostics:

```bash
./gradlew :examples:tagged-oneof:check --console=plain
```

The example lives in [`examples/tagged-oneof`](examples/tagged-oneof), with its
schema at
[`examples/tagged-oneof/src/main/schema/tagged-oneof.schema.json`](examples/tagged-oneof/src/main/schema/tagged-oneof.schema.json)
and checked-in generated sources under
[`examples/tagged-oneof/generated-src/main/java`](examples/tagged-oneof/generated-src/main/java).

## Design And Verification

- Project scope and non-goals: [`docs/charter.md`](docs/charter.md)
- Supported profile and diagnostics: [`docs/supported-profile.md`](docs/supported-profile.md)
- Generated source contract: [`docs/architecture/generated-code-contract.md`](docs/architecture/generated-code-contract.md)
- Module boundaries: [`docs/architecture/module-boundaries.md`](docs/architecture/module-boundaries.md)
- Error reporting: [`docs/architecture/error-reporting.md`](docs/architecture/error-reporting.md)
- Generated source verification: [`docs/verification/generated-source-verification.md`](docs/verification/generated-source-verification.md)
- JSON Schema Test Suite trace: [`docs/verification/json-schema-test-suite.md`](docs/verification/json-schema-test-suite.md)
- SchemaStore sidecar corpus evidence: [`docs/verification/schemastore-corpus.md`](docs/verification/schemastore-corpus.md)
- Native Image verification: [`docs/verification/native-image.md`](docs/verification/native-image.md)
- Performance evidence: [`docs/verification/performance.md`](docs/verification/performance.md)
- Release verification: [`docs/verification/release.md`](docs/verification/release.md)
- v1 readiness: [`docs/verification/v1-readiness.md`](docs/verification/v1-readiness.md)

The roadmap and task history live in [`docs/roadmap.md`](docs/roadmap.md).
