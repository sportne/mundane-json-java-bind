# v1 Readiness Review

This document records the final v1 readiness state for
`mundane-json-java-bind`.

## Status

`TASK-0028` is complete. The default JVM quality gate, release dry-run gate, and
Native Image smoke lane pass locally. Native Image was run with SDKMAN's GraalVM
CE 21.0.2 Java 21 toolchain.

## Roadmap Summary

All prerequisite roadmap tasks are complete:

- `TASK-0001` through `TASK-0005`: project governance, infrastructure, parser,
  writer, schema syntax, and profile diagnostics.
- `TASK-0006` through `TASK-0014`: basic object model, reader, writer,
  validator, CLI, Gradle plugin, and executable example.
- `TASK-0015` through `TASK-0018`: arrays, scalar facets, literal constraints,
  defaults, and nullable fields.
- `TASK-0019` through `TASK-0021`: object diagnostics, tagged `oneOf`, and
  optional schema metadata helpers.
- `TASK-0022` through `TASK-0027`: JSON Schema Test Suite traceability,
  architecture hardening, Native Image smoke lane, performance evidence,
  documentation completion, and release dry-run readiness.

`TASK-0028` closes the roadmap subject to maintainer approval to tag v1.

## Profile Traceability

The v1 profile is `JSP-DATA-2020-12`. It is intentionally partial and is
documented in:

- [`../supported-profile.md`](../supported-profile.md)
- [`../standards-baseline.md`](../standards-baseline.md)
- [`json-schema-test-suite.md`](json-schema-test-suite.md)

Supported behavior is generated Java binding behavior, not runtime schema
interpretation. Unsupported Draft 2020-12 features fail before source emission
with deterministic `MJJBG-*` diagnostics and exact schema JSON Pointer
locations.

Known limits are deliberate v1 boundaries:

- no full JSON Schema implementation;
- no root scalar or root array binding;
- no reference resolution;
- no generic, untagged, or runtime-discovered union matching;
- no runtime reflection, annotations, dynamic class loading, `ServiceLoader`,
  or runtime schema scanning in generated binding behavior;
- no remote publication, signing, Maven Central upload, or release repository
  credentials.

## Verification Evidence

Local evidence:

| Gate | Evidence | Status |
|---|---|---|
| JVM quality gate | `./gradlew qualityGate --console=plain` | Pass |
| Release dry-run | `./gradlew releaseDryRun --console=plain` | Pass |
| Release version override | `./gradlew releaseDryRun -Pmjjb.version=0.1.0 --console=plain` | Pass |
| Native Image smoke | `source "$HOME/.sdkman/bin/sdkman-init.sh" && ./gradlew nativeSmoke --console=plain` | Pass |

Native Image toolchain evidence:

```text
Current default java version 21.0.2-graalce
native-image 21.0.2 2024-01-16
GraalVM Runtime Environment GraalVM CE 21.0.2+13.1
```

The existing CI lane is documented in [`native-image.md`](native-image.md) and
defined at `../../.github/workflows/native-image.yml`.

## Completion Criteria

`TASK-0028` is complete when:

1. Native Image evidence for the current tree is recorded.
2. `./gradlew qualityGate --console=plain` passes.
3. `./gradlew releaseDryRun --console=plain` passes.
4. `git status --short --ignored` shows no unignored generated output.

Those conditions were satisfied for this readiness review.
