# Native Image Verification

The Native Image lane proves that runtime, parser, CLI, and generated-binding
example paths compile and run as GraalVM native executables without reflection
configuration for normal binding behavior.

## Prerequisites

- Java 21 GraalVM with the `native-image` executable available through the
  configured Gradle toolchain or `JAVA_HOME`.
- Enough local memory and disk for Native Image compilation.

The default JVM quality gate does not run Native Image because the toolchain is
optional for local development.

## Commands

Run the ordinary JVM gate first:

```bash
./gradlew qualityGate --console=plain
```

Run the Native Image smoke lane in a GraalVM environment:

```bash
./gradlew nativeSmoke --console=plain
```

The aggregate runs these dedicated native test binaries:

- `:modules:runtime-core:nativeSmokeTest`
- `:modules:parser-core:nativeSmokeTest`
- `:modules:generator-cli:nativeSmokeTest`
- `:examples:basic-record:nativeSmokeTest`
- `:examples:tagged-oneof:nativeSmokeTest`

## Coverage

The smoke tests cover runtime primitives, parser read/write behavior, CLI
generation and deterministic generator diagnostics, and generated example
bindings for basic records and tagged `oneOf`.

Native smoke tests live in `src/nativeSmokeTest/java`. The lane does not native
compile the broad JVM unit suites or ArchUnit tests.

## Policy

No reflection configuration files, reachability metadata repository entries, or
native-only runtime behavior branches are required for normal runtime and
generated binding behavior. A Native Image failure caused by reflection,
resource lookup, proxy generation, serialization metadata, or classpath scanning
is treated as an architecture issue unless a future ADR explicitly permits it.

## CI

`.github/workflows/native-image.yml` installs GraalVM Java 21 with Native Image
support and runs `./gradlew nativeSmoke --console=plain`.
