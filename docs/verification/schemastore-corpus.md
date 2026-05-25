# SchemaStore Corpus Evidence

This document records the explicit SchemaStore sidecar evidence lane. It is
release-blocking for `1.0.0`, but it is not part of the default build,
`checkAll`, `qualityGate`, release dry-run, or CI.

## Source

- Catalog: <https://www.schemastore.org/api/json/catalog.json>
- Evidence date: 2026-05-24
- Manifest:
  `../../modules/conformance-tests/src/schemaStoreCorpusTest/resources/schemastore-corpus/manifest.tsv`

The committed manifest stores schema names, source URLs, expected SHA-256
digests, expected outcome, and expected diagnostic category. Downloaded schema
bodies, generated sources, and reports are written under `build/` and are not
committed.

Manifest URLs may include JSON Pointer fragments. The sidecar verifies the
SHA-256 digest of the downloaded base schema document, then selects the
referenced subschema for profile validation and generation.

SchemaStore provides schema metadata and URLs. It does not provide a general
positive/negative JSON instance corpus, so the sidecar synthesizes generated
binding instances for schemas that fit the v1 profile.

## Command

Run the sidecar explicitly:

```bash
./gradlew schemaStoreCorpus --console=plain
```

If an upstream schema digest drifts, the normal sidecar fails. To inspect drift
without accepting it as release evidence, run:

```bash
./gradlew schemaStoreCorpus -Pmjjb.schemaStoreRefresh=true --console=plain
```

Refresh mode writes observed digests to the generated report only; it does not
rewrite the committed manifest.

## Harness Behavior

For every manifest entry, the sidecar:

- fetches the schema URL and verifies the SHA-256 digest;
- parses the schema with `SchemaSyntaxParser`;
- validates the supported profile with `SchemaSupportProfile`;
- treats expected profile or generator rejection as passing evidence when the
  diagnostic category matches the manifest;
- for supported schemas, runs `CoreGenerator`, compiles generated sources, and
  uses reflection in the test harness to construct a generated record;
- writes generated JSON, reads it back, compares object equality, and validates
  the generated object;
- exercises generic invalid JSON paths for supported schemas, including
  non-object roots, unknown properties, duplicate properties, missing required
  properties where available, and wrong scalar types where identifiable.

Reflection is confined to the sidecar test harness. Generated v1 bindings
remain reflection-free.

## Evidence

Latest local run:

```text
./gradlew schemaStoreCorpus --console=plain
BUILD SUCCESSFUL
```

Aggregate results:

| Result | Count |
|---|---:|
| Total manifest entries | 99 |
| Generated, compiled, and round-tripped | 77 |
| Expected profile rejections | 19 |
| Expected generator rejections | 3 |
| Digest drift failures | 0 |
| Unexpected failures | 0 |

The generated report is written to:

```text
modules/conformance-tests/build/reports/schemastore-corpus/schemastore-corpus-results.md
```

The report is build output and is intentionally not committed.

On 2026-05-24, refresh mode identified upstream digest drift for six Renovate
subschema entries. The selected subschemas still fit the supported profile and
round-trip successfully, so the committed manifest digests were updated
explicitly rather than by an automatic rewrite.

The sidecar report emits the same aggregate table as this document. A successful
non-refresh sidecar run verifies that the committed aggregate counts above match
the harness output, so the documentation cannot drift silently from the current
manifest evidence.

## Broader Catalog Feature Scan

The sidecar manifest is intentionally named and stable. To guide future feature
selection, the SchemaStore catalog was also scanned on 2026-05-23 for common
features outside the v1 profile. The scan fetched 1,309 catalog entries; 1,259
schema documents were reachable and parseable, and 50 failed fetch or JSON
parse.

Counts below are document-level counts: a schema is counted once when it
contains the feature anywhere in the document.

| Feature | Documents | Share of parsed docs | Example schemas |
|---|---:|---:|---|
| `$ref` | 924 | 73.4% | Hashgraph Online Skill Manifest, Releasaurus Config, Bacon config |
| Object-valued `additionalProperties` | 607 | 48.2% | revola.json, release-hub.json, Hashgraph Online Skill Manifest |
| Generic `oneOf` | 567 | 45.0% | ReleaseKit, Releasaurus Config, Bacon config |
| `anyOf` | 530 | 42.1% | revola.json, release-hub.json, Releasaurus Config |
| `allOf` | 317 | 25.2% | Upsun config, Application Accelerator, .NET Aspire 8.0 Manifest |
| `patternProperties` | 287 | 22.8% | .adonisrc.json, ABCInventoryModuleData, ABCClinicalDemandForecast |
| Multiple non-null `type` alternatives | 218 | 17.3% | Upsun config, Platform.sh application, Platform.sh routes |
| `not` | 174 | 13.8% | gRPC API Gateway and OpenAPI Config, .NET Aspire 8.0 Manifest, angular.json |
| `if` / `then` / `else` | 162 / 148 / 50 | 12.9% / 11.8% / 4.0% | AnyWork Automation Configuration, ABCSupplyPlan, ACP Sync |
| Legacy `dependencies` | 146 | 11.6% | Upsun config, Platform.sh application, appsscript.json |
| `propertyNames` | 72 | 5.7% | revola.json, release-hub.json, Hashgraph Online Skill Manifest |
| Tuple-style array `items` | 46 | 3.7% | App config Spotify Backstage, .appsemblerc.yaml, arc.json |
| `contains` | 24 | 1.9% | Convex, CVE Record Format, Helm Unittest Test Suite |
| `unevaluatedProperties` | 22 | 1.7% | CMake Presets, Enonic XP descriptors |
| `prefixItems` | 16 | 1.3% | EveryVoice TTS Toolkit schemas |
| `dependentRequired` / `dependentSchemas` | 9 / 1 | 0.7% / 0.1% | DataYoga Connections, FlexGet Config |

This distribution suggests the highest-leverage post-v1 features are reference
resolution, object-valued `additionalProperties`, and broader composition /
union support.
