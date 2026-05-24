# JSON Schema Test Suite Trace

The project keeps a curated trace against the official JSON Schema Test Suite:

- Upstream: <https://github.com/json-schema-org/JSON-Schema-Test-Suite>
- Pinned commit: `ba30ec795b67fb0bb636fedda3210b95d8cf558b`
- License/provenance: upstream is MIT licensed and the checked-in manifest records upstream fixture file, case description, and test description for every traced case.

The full upstream suite is not vendored. `TASK-0022` records a small immutable
manifest in `OfficialJsonSchemaTestSuiteConformanceTest` so the project has
repeatable evidence without importing unrelated validator fixtures.

## Projection Policy

The upstream suite is written for general-purpose JSON Schema validators. This
project generates Java bindings for a narrower root-object profile, so allowed
cases are projected into a minimal generated object binding:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "properties": {
    "value": { "... upstream assertion schema ...": true }
  },
  "required": ["value"],
  "additionalProperties": false
}
```

The JSON instance is projected the same way, as `{"value": ...}`. The generated
reader and validator then decide validity. A generated reader failure counts as
an invalid instance result because the projected instance cannot be represented
by the generated binding.

Allowed cases must:

- have nonblank upstream path, case description, and test description;
- be deterministic projections of official Draft 2020-12 scalar, array, or
  closed nested object assertions;
- generate and compile Java sources with `--release 21 -Xlint:all -Werror`;
- execute through generated code only, not a dynamic schema interpreter.

## Skip Policy

Skipped cases are representative official fixtures that do not fit the current
generated-binding profile. Each skip records upstream provenance and one stable
reason code.

| Reason | Meaning |
|---|---|
| `ROOT_NON_OBJECT_BINDING` | The official assertion targets root scalar, array, boolean, or unconstrained schemas rather than a generated root object binding. |
| `MISSING_REQUIRED_BINDING_SHAPE` | The official schema does not define the closed object shape required by the generator profile. |
| `UNTYPED_PROPERTY_SCHEMA` | The official property schema relies on generic validator behavior rather than a supported typed binding. |
| `UNSUPPORTED_KEYWORD` | The fixture depends on a Draft 2020-12 keyword intentionally outside the current profile. |
| `UNSUPPORTED_KEYWORD_VALUE` | The keyword is known, but the fixture uses a value shape outside the supported binding slice. |
| `REMOTE_REFERENCE` | The fixture requires reference resolution or remote resources. |
| `OPTIONAL_FORMAT_SCOPE` | The fixture uses optional format assertions outside the supported `date`, `date-time`, and `uuid` formats. |
| `NUMERIC_SEMANTICS_DEFERRED` | The fixture relies on broader JSON Schema numeric equivalence than the current generated integer binding supports. |

## Update Process

1. Choose an upstream commit and record it in this document and the conformance
   test manifest.
2. Add only representative official cases that map cleanly to the current
   generated-binding profile.
3. Add skip entries for unsupported representative cases instead of broadening
   generation behavior only to satisfy the official suite.
4. Run:

```bash
./gradlew :modules:conformance-tests:check --console=plain
```

`qualityGate` also runs this module and remains the release gate.
