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

The evidence command measures small and medium JSON fixtures for:

- parser streaming reads through `JsonStreamReader`;
- generated binding read, validate, and write loops compiled from a representative
  generated object binding.

The report includes fixture size, warmup batches, measured batches, batch
iterations, min/median/max batch time, rough heap delta, runtime details, and a
checksum that keeps measured work observable.

## Interpretation

The timing values are intentionally not thresholds. Use them to compare local
runs on the same machine, JVM, and Gradle setup when investigating regressions.
Do not compare report values across unrelated hardware as pass/fail evidence.

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

## Non-Goals

- No JMH dependency is introduced.
- No runtime measurement dependency is introduced.
- No public API or generated-code behavior is changed by the evidence lane.
- No CI timing threshold is enforced.
