# TASK-0029: SchemaStore Sidecar Corpus Evidence

Status: Complete

## Goal

Add release-blocking external schema corpus evidence before tagging `1.0.0`
without making the normal build depend on internet access.

## Scope

- Add an explicit SchemaStore sidecar lane outside `check`, `checkAll`,
  `qualityGate`, release dry-run, and default CI.
- Commit only compact schema metadata: name, URL, expected SHA-256, expected
  outcome, and expected diagnostic category.
- Keep downloaded schemas, generated sources, and reports under `build/`.
- Exercise supported schemas through generated code and expected unsupported
  schemas through deterministic profile or generator diagnostics.

## Evidence

```bash
./gradlew schemaStoreCorpus --console=plain
```

The sidecar passes against 99 SchemaStore entries: 78 generated, compiled, and
round-tripped; and 21 rejected by expected profile diagnostics.

Detailed evidence is documented in
[`../../verification/schemastore-corpus.md`](../../verification/schemastore-corpus.md).
