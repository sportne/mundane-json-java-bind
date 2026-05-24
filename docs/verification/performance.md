# Performance Verification

The performance evidence lane records repeatable parser and generated-binding
measurements for regression investigation. It is diagnostic evidence, not a
benchmark gate.

## Command

Run the ordinary JVM gate first:

```bash
./gradlew qualityGate --console=plain
```

Generate performance evidence:

```bash
./gradlew :modules:conformance-tests:performanceEvidence --console=plain
```

The command writes:

```text
modules/conformance-tests/build/reports/performance/performance-evidence.md
```

## Coverage

The evidence command measures:

- parser streaming reads through `JsonStreamReader` on small and medium
  fixtures;
- generator throughput for a representative rich object schema;
- generated Java compilation time for the emitted binding sources;
- emitted schema/source size for the representative generated binding;
- generated binding read, validate, and write loops for small, medium, and rich
  fixtures.

The rich generated-binding fixture includes a nested object, declared arrays,
object validation keywords, numeric validation, `uniqueItems`,
`patternProperties`, and an object-valued `additionalProperties` map. This keeps
the evidence lane aligned with the current post-v1 feature set without changing
runtime behavior or adding benchmark dependencies.

The report includes fixture or artifact size, warmup batches, measured batches,
batch iterations, min/median/max batch time, rough heap delta, runtime details,
and a checksum that keeps measured work observable.

## Interpretation

The timing values are intentionally not thresholds. Use them to compare local
runs on the same machine, JVM, and Gradle setup when investigating regressions.
Do not compare report values across unrelated hardware as pass/fail evidence.

The conformance test suite uses an internal `--quick` mode to verify the v2
report shape without doing the full diagnostic measurement loop. The published
Gradle evidence task continues to run the normal diagnostic plan.

Memory deltas are rough heap observations around measured batches, not allocation
profiles. They can show obvious regressions, but a profiler or Java Flight
Recorder capture is still required for detailed allocation analysis.

## Memory Boundary

Parser and generated binding paths stream JSON tokens and do not reconstruct a
generic JSON object graph. Generated readers construct only the requested Java
binding model.

`JsonStreamReader` currently retains consumed input in its internal character
buffer while reading from a `Reader`. This is a known v1 boundary: token handling
is streaming and object-graph-free, but the parser is not yet a fixed-size
sliding-window input buffer.

## Not Yet Measured

The v2 evidence lane still does not measure:

- allocation profiles or object-retention graphs;
- generated-code cold start outside the first measured batches;
- native-image runtime performance;
- corpus-scale generation across many independent schemas;
- end-to-end Gradle plugin or CLI latency;
- concurrent parser, generator, or generated-binding use.

## Non-Goals

- No JMH dependency is introduced.
- No runtime measurement dependency is introduced.
- No public API or generated-code behavior is changed by the evidence lane.
- No CI timing threshold is enforced.
