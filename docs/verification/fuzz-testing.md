# Deterministic Fuzz Testing

The fuzz lane adds optional seeded JUnit tests for parser behavior and generated
binding behavior. It is deterministic and dependency-free. It is not
coverage-guided fuzzing, and it is not a replacement for the JSON Schema Test
Suite or the generated-source smoke fixtures.

## Commands

Run the parser fuzz tests:

```bash
./gradlew :modules:parser-core:fuzzTest --console=plain
```

Run the generated-binding fuzz tests:

```bash
./gradlew :modules:conformance-tests:fuzzTest --console=plain
```

Run the full optional fuzz lane:

```bash
./gradlew fuzzTest --console=plain
```

The default `test`, `check`, `checkAll`, and `qualityGate` tasks exclude tests
tagged `fuzz`. The fuzz lane is explicit so normal development and release gates
stay fast and stable.

## Reproduction

Fuzz configuration is controlled by Gradle properties:

```bash
./gradlew fuzzTest \
  -Pmjjb.fuzz.seed=3405691582 \
  -Pmjjb.fuzz.iterations=96 \
  -Pmjjb.fuzz.maxDepth=5 \
  -Pmjjb.fuzz.maxStringLength=24 \
  --console=plain
```

Failure messages include the scenario name, seed, iteration, and input text. To
reproduce a failure, rerun the same task with the printed seed and the same
iteration-related properties.

## Coverage

Parser fuzz tests generate valid RFC 8259-style JSON values covering objects,
arrays, strings, numbers, booleans, nulls, whitespace, escapes, and unicode
escapes. Each valid case is consumed through both string-backed and
Reader-backed `JsonStreamReader` instances. Targeted invalid mutations cover
truncation, invalid escapes, malformed numbers, trailing commas, missing
separators, and extra trailing tokens.

Generated-binding fuzz tests compile a fixed set of supported-profile schemas
and exercise generated read, validate, write, and reread behavior. The scenarios
cover closed objects, nullable fields, arrays, nested objects, map bindings,
pattern maps, local refs, constrained `allOf`, tagged `oneOf`, numeric facets,
literals, and object validators. Invalid cases cover required fields, duplicate
declared properties, unknown properties, invalid scalar types, validator
failures, map key routing, numeric facets, and literal constraints.

## Boundaries

- No new fuzzing library or runtime dependency is introduced.
- Timing is not measured by this lane.
- Coverage-guided fuzzing such as Jazzer remains a possible future task if the
  deterministic lane exposes gaps that seeded tests cannot explore effectively.
