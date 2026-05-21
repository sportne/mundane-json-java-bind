# Native Image Test Plan

Native Image checks prove that runtime and generated-code paths avoid unexpected
dynamic JVM behavior.

## Stages

1. Runtime primitive native tests for `runtime-core`.
2. Parser native tests for `parser-core`.
3. Generated-code Native Image smoke executable in `generator-core`.
4. Example binding native tests for representative generated code.

## Policy

Native Image smoke tests are separate from `qualityGate` because they require a
GraalVM `native-image` toolchain.

A Native Image failure caused by reflection, resource lookup, proxy generation,
serialization metadata, or classpath scanning is treated as an architecture
issue unless a future ADR explicitly permits it.

The executable verification guide is `docs/verification/native-image.md`.
