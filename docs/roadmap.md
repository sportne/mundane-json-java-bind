# Roadmap

This roadmap defines the work required to complete `mundane-json-java-bind` as a
single-purpose JSON Schema to Java binding/code generation project.

The roadmap is intentionally ordered as discrete implementation tasks grouped
into vertical-slice milestones. A task may be a narrow internal step; a
milestone is complete when its ordered tasks leave the repository with a
testable behavior slice before broadening the supported JSON Schema surface.

## Completion Definition

The project is complete when it can:

- consume JSON Schema Draft 2020-12 schemas in the `JSP-DATA-2020-12` profile;
- generate Java 21 model, reader, writer, validator, and optional metadata
  helpers for the accepted v1 subset;
- reject unsupported Draft 2020-12 features with deterministic diagnostics and
  exact schema JSON Pointer locations;
- run generated bindings on the JVM and under GraalVM Native Image without
  reflection configuration;
- pass quality, architecture, conformance, generated-source, parser, and native
  smoke gates.

## Vertical-Slice Milestones

| Milestone | Tasks | Complete when |
|---|---|---|
| Foundation | `TASK-0001` to `TASK-0005` | Runtime JSON primitives, infrastructure alignment, parser/writer foundations, schema syntax modeling, and profile diagnostics are testable. |
| Basic object binding | `TASK-0006` to `TASK-0014` | A basic object schema generates model, reader, writer, validator, CLI/Gradle output, and an executable example. |
| Collections and value constraints | `TASK-0015` to `TASK-0018` | Arrays, scalar facets, enum/const/default, and nullable handling work through generated code. |
| Object shape expansion | `TASK-0019` to `TASK-0021` | Object diagnostics, tagged `oneOf`, and optional metadata helpers are supported without runtime reflection. |
| Completion hardening | `TASK-0022` to `TASK-0029` | Conformance allowlists, architecture rules, Native Image smoke tests, external corpus evidence, evidence docs, and release checks pass. |
| Post-v1 profile expansion | `TASK-0030` to `TASK-0036` | Recommended JSON Schema features are added in usefulness/commonness order without changing the no-reflection generated-binding model. |
| Post-v1 hardening and simplification | `TASK-0037` to `TASK-0042` | The expanded profile has documented simplification opportunities, current performance evidence, mined external-risk probes, fresh corpus evidence, generated-code ergonomics review, and an explicit numeric semantics decision. |

## Task Order

| Task | Title | Milestone |
|---|---|---|
| [TASK-0001](roadmap/tasks/TASK-0001-roadmap-and-governance-baseline.md) | Roadmap and governance baseline | Planning |
| [TASK-0002A](roadmap/tasks/TASK-0002A-infrastructure-alignment.md) | Infrastructure alignment before parser work | Planning |
| [TASK-0002](roadmap/tasks/TASK-0002-streaming-parser-core.md) | Streaming parser core completion | Runtime foundation |
| [TASK-0003](roadmap/tasks/TASK-0003-json-writer-core.md) | JSON writer core completion | Runtime foundation |
| [TASK-0004](roadmap/tasks/TASK-0004-schema-syntax-model.md) | Schema syntax model and parser | Schema frontend |
| [TASK-0005](roadmap/tasks/TASK-0005-profile-diagnostics.md) | Profile validation diagnostics | Schema frontend |
| [TASK-0006](roadmap/tasks/TASK-0006-binding-ir-basic-object.md) | Binding IR for basic objects | First binding slice |
| [TASK-0007](roadmap/tasks/TASK-0007-generated-model-emitter.md) | Generated model emitter | First binding slice |
| [TASK-0008](roadmap/tasks/TASK-0008-generated-source-verification.md) | Generated-source verification harness | First binding slice |
| [TASK-0009](roadmap/tasks/TASK-0009-generated-writer-basic-object.md) | Generated writer for basic objects | First round trip |
| [TASK-0010](roadmap/tasks/TASK-0010-generated-reader-basic-object.md) | Generated reader for basic objects | First round trip |
| [TASK-0011](roadmap/tasks/TASK-0011-generated-validator-basic-object.md) | Generated validator for basic objects | First round trip |
| [TASK-0012](roadmap/tasks/TASK-0012-cli-basic-object-integration.md) | CLI integration for basic object binding | User tooling |
| [TASK-0013](roadmap/tasks/TASK-0013-gradle-plugin-basic-object-integration.md) | Gradle plugin integration for basic object binding | User tooling |
| [TASK-0014](roadmap/tasks/TASK-0014-basic-record-example.md) | Basic record example | Example evidence |
| [TASK-0015](roadmap/tasks/TASK-0015-array-items-support.md) | Array and `items` support | Collection slice |
| [TASK-0016](roadmap/tasks/TASK-0016-string-number-facets.md) | String and numeric facets | Validation slice |
| [TASK-0017](roadmap/tasks/TASK-0017-enum-const-default.md) | `enum`, `const`, and `default` | Value constraints |
| [TASK-0018](roadmap/tasks/TASK-0018-nullable-field-support.md) | Nullable field support | Nullability slice |
| [TASK-0019](roadmap/tasks/TASK-0019-object-diagnostics-hardening.md) | Object diagnostic hardening | Diagnostics slice |
| [TASK-0020](roadmap/tasks/TASK-0020-tagged-oneof-support.md) | Tagged `oneOf` support | Polymorphism slice |
| [TASK-0021](roadmap/tasks/TASK-0021-schema-metadata-helpers.md) | Optional schema metadata helpers | Metadata slice |
| [TASK-0022](roadmap/tasks/TASK-0022-json-schema-test-suite.md) | JSON Schema Test Suite allowlist | Conformance |
| [TASK-0023](roadmap/tasks/TASK-0023-architecture-rule-hardening.md) | Architecture rule hardening | Governance |
| [TASK-0024](roadmap/tasks/TASK-0024-native-image-smoke.md) | Native Image smoke lane | Native Image |
| [TASK-0025](roadmap/tasks/TASK-0025-performance-memory-evidence.md) | Performance and memory evidence | Hardening |
| [TASK-0026](roadmap/tasks/TASK-0026-documentation-completion.md) | Documentation completion | Documentation |
| [TASK-0027](roadmap/tasks/TASK-0027-release-publication-readiness.md) | Release and publication readiness | Release |
| [TASK-0028](roadmap/tasks/TASK-0028-v1-readiness-review.md) | v1 readiness review | Final gate |
| [TASK-0029](roadmap/tasks/TASK-0029-schemastore-sidecar-corpus-evidence.md) | SchemaStore sidecar corpus evidence | Final gate |
| [TASK-0030](roadmap/tasks/TASK-0030-nested-object-property-bindings.md) | Nested object property bindings | Post-v1 profile expansion |
| [TASK-0031](roadmap/tasks/TASK-0031-local-defs-ref-resolution.md) | Local `$defs` and `$ref` resolution | Post-v1 profile expansion |
| [TASK-0032](roadmap/tasks/TASK-0032-map-bindings-additional-properties.md) | Map bindings for object-valued `additionalProperties` | Post-v1 profile expansion |
| [TASK-0033](roadmap/tasks/TASK-0033-constrained-allof-object-flattening.md) | Constrained `allOf` object flattening | Post-v1 profile expansion |
| [TASK-0034](roadmap/tasks/TASK-0034-pattern-properties-map-bindings.md) | `patternProperties` map bindings | Post-v1 profile expansion |
| [TASK-0035](roadmap/tasks/TASK-0035-object-validation-keywords.md) | Object validation keywords | Post-v1 profile expansion |
| [TASK-0036](roadmap/tasks/TASK-0036-multipleof-uniqueitems.md) | `multipleOf` and `uniqueItems` | Post-v1 profile expansion |
| [TASK-0037](roadmap/tasks/TASK-0037-generator-simplification-audit.md) | Generator architecture simplification audit | Post-v1 hardening and simplification |
| [TASK-0038](roadmap/tasks/TASK-0038-performance-baseline-v2.md) | Performance baseline v2 | Post-v1 hardening and simplification |
| [TASK-0039](roadmap/tasks/TASK-0039-external-issue-mining.md) | External issue mining | Post-v1 hardening and simplification |
| [TASK-0040](roadmap/tasks/TASK-0040-schemastore-evidence-freshness.md) | SchemaStore evidence freshness | Post-v1 hardening and simplification |
| [TASK-0041](roadmap/tasks/TASK-0041-generated-code-ergonomics-review.md) | Generated-code ergonomics review | Post-v1 hardening and simplification |
| [TASK-0042](roadmap/tasks/TASK-0042-numeric-semantics-decision.md) | Numeric semantics decision | Post-v1 hardening and simplification |

## Policy

- Tasks should be completed in order unless their allowed file sets do not
  overlap and the dependency notes allow parallel work.
- Every task must leave `./gradlew qualityGate --console=plain` passing unless
  the task file explicitly defines a narrower temporary evidence command.
- Native Image work remains separate from the default quality gate but must pass
  before v1 readiness.
- SchemaStore corpus evidence remains separate from the default quality gate but
  must pass before tagging `1.0.0`.
- New scope outside JSON Schema to Java binding requires a charter update before
  implementation.
