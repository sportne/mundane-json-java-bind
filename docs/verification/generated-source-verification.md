# Generated Source Verification

Generated source verification is owned by `generator-core` and runs through the
`generatedCodeSmoke` task.

## Fixture Layout

Smoke fixtures live under:

```text
modules/generator-core/src/generatedCodeSmoke/resources/fixtures/<fixture-name>/
```

Each fixture contains:

- `schema.json`: the JSON Schema input.
- `*.java.golden`: exact expected generated Java source files.
- `WriterBehaviorProbe.java` when generated writer behavior should be compiled
  and executed against the fixture.

Golden files use fixed `\n` line endings. The verifier compares generated
source bytes directly against the golden resources.

## Task

Run:

```text
./gradlew :modules:generator-core:generatedCodeSmoke --console=plain
```

The task generates source for every fixture, compares the output with its
golden files, checks forbidden architecture tokens, and compiles the generated
Java source set with:

```text
--release 21 -Xlint:all -Werror
```

`generator-core:check` depends on `generatedCodeSmoke`, so the default
`qualityGate` includes generated-source smoke verification.

Writer behavior probes are test-only Java sources compiled after the generated
sources. They may use `JsonStringWriter` to observe compact output, but generated
source itself must depend only on `runtime-core` writer interfaces.

## Forbidden Tokens

Generated source must not contain binding annotations, reflection APIs,
`MethodHandles`, `ServiceLoader`, dynamic proxies, generator implementation
packages, schema-model packages, or parser implementation packages.

## Extension Policy

Reader, validator, nullable, array, and tagged `oneOf` generated-code fixtures
should extend this same harness. New fixtures should keep failure messages
actionable by including the fixture name, generated source path, and specific
reason.
