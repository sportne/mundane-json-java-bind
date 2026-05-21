# Supported Profile

`JSP-DATA-2020-12` is the v1 generated-binding profile. It is based on JSON
Schema Draft 2020-12, but it intentionally supports only the schema shapes that
can produce deterministic Java 21 bindings without runtime reflection or
runtime schema interpretation.

For normative inputs and official test-suite traceability, see
[`standards-baseline.md`](standards-baseline.md). For emitted Java shapes, see
[`architecture/generated-code-contract.md`](architecture/generated-code-contract.md).

## Root Shapes

The profile supports two root forms:

- closed object schemas with `type: "object"`, `properties`, `required`, and
  `additionalProperties: false`;
- tagged `oneOf` roots where every branch is a closed object schema and all
  branches share one required string tag property with branch-unique `const`
  values.

The generator rejects open object bindings, generic unions, untagged `oneOf`,
references, composition beyond the supported tagged form, and root scalar or
root array bindings.

## Field Types

Supported object fields are:

| Schema shape | Java shape |
|---|---|
| required `string` | `String` |
| optional `string` | `Optional<String>` |
| required `integer` | `long` |
| optional `integer` | `Optional<Long>` |
| required `number` | `double` |
| optional `number` | `Optional<Double>` |
| required `boolean` | `boolean` |
| optional `boolean` | `Optional<Boolean>` |
| required homogeneous scalar array | `List<T>` |
| optional homogeneous scalar array | `Optional<List<T>>` |
| nullable scalar or nullable homogeneous scalar array | `JsonField<T>` |

Nullable fields use field-level `type` arrays containing exactly `null` and one
supported non-null type, such as `["null", "string"]`. `JsonField.absent()`,
`JsonField.explicitNull()`, and `JsonField.value(value)` preserve absent,
explicit-null, and present-value states.

## Supported Keywords

The profile supports these binding and validation keywords:

| Category | Keywords |
|---|---|
| Shape | `type`, `properties`, `required`, `additionalProperties`, `items`, `oneOf` |
| Collections | `minItems`, `maxItems` |
| String facets | `minLength`, `maxLength`, `pattern`, `format` |
| Numeric facets | `minimum`, `maximum`, `exclusiveMinimum`, `exclusiveMaximum` |
| Literal constraints | `enum`, `const` |
| Metadata annotation | `default` |

`format` assertions are limited to `date`, `date-time`, and `uuid`. `pattern`
uses Java `Pattern` checks with JSON Schema search semantics and must compile
during generation.

`default` is an annotation. It does not change generated constructors, readers,
writers, or validators. Supported defaults are exposed through generated model
metadata accessors and optional schema metadata helpers.

## Accepted Annotations

The profile accepts these annotations and dialect markers:

| Annotation | Behavior |
|---|---|
| `$schema` | Recognized Draft 2020-12 dialect marker. |
| `title`, `description`, `$comment`, `examples` | Accepted annotations and exposed when metadata helpers are generated. |
| `deprecated`, `readOnly`, `writeOnly` | Accepted annotations and exposed when metadata helpers are generated. |

Unknown non-Draft extension keywords are ignored as annotations. Known Draft
2020-12 keywords are classified explicitly as supported or rejected.

## Rejected Draft 2020-12 Features

The v1 profile rejects known Draft 2020-12 features that would require broader
schema evaluation, references, open object matching, tuple validation, or
general-purpose validator behavior. Rejected keywords include:

`$id`, `$anchor`, `$dynamicAnchor`, `$vocabulary`, `$ref`, `$dynamicRef`,
`$defs`, `allOf`, `anyOf`, `not`, `if`, `then`, `else`, `dependentSchemas`,
`prefixItems`, `contains`, `patternProperties`, `propertyNames`,
`unevaluatedItems`, `unevaluatedProperties`, `multipleOf`, `uniqueItems`,
`maxContains`, `minContains`, `maxProperties`, `minProperties`,
`dependentRequired`, `contentEncoding`, `contentMediaType`, and
`contentSchema`.

Unsupported features fail before source emission. The generator reports stable
`MJJBG-*` diagnostics with the schema file and exact schema JSON Pointer, such
as `/properties/value/allOf`.

## Generated Behavior

Generated readers parse through the project-owned `JsonReader` interface,
reject duplicate and unknown properties, require all required properties, and
require full-document consumption. Reader diagnostics use stable `MJJBR-*`
codes; parser failures retain stable `MJJBP-*` codes and are re-pathed when the
generated reader knows the active instance path.

Generated writers emit deterministic schema property order, skip absent
optional fields, write explicit nullable nulls, and validate JSON number
syntax before writing numeric literals.

Generated validators operate on generated model instances and return
`ValidationResult` values. Validation failures use stable `MJJBV-*` codes and
are reported in schema property order, with array item errors using indexed
paths such as `$.labels[0]`.

Optional schema metadata helpers are generated only when requested through the
public generator API. They expose immutable schema facts from generated source
and do not participate in model construction, reading, writing, or validation.

## Verification

Profile behavior is covered by generated-source smoke tests, module tests,
example checks, and an official JSON Schema Test Suite allowlist. The default
local verification command is:

```bash
./gradlew qualityGate --console=plain
```

Native Image verification is available separately for GraalVM environments:

```bash
./gradlew nativeSmoke --console=plain
```
