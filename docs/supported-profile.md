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
remote references, dynamic references, composition beyond the supported tagged
form, and root scalar or root array bindings.

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
| required closed object | nested record type |
| optional closed object | `Optional<NestedRecord>` |
| nullable scalar or nullable homogeneous scalar array | `JsonField<T>` |

Nullable fields use field-level `type` arrays containing exactly `null` and one
supported non-null type, such as `["null", "string"]`. `JsonField.absent()`,
`JsonField.explicitNull()`, and `JsonField.value(value)` preserve absent,
explicit-null, and present-value states.

Nested object fields must use the same closed-object shape as roots and tagged
branches: `type: "object"`, optional `properties`, optional `required`, and
`additionalProperties: false`. Nested nullable object fields and object arrays
remain outside the current profile.

Object-valued `additionalProperties` generates a deterministic
`Map<String, T>` catch-all binding. The map value schema may be a supported
scalar, nullable scalar, homogeneous scalar array, nullable homogeneous scalar
array, or closed nested object schema. Declared `properties` stay as normal
record components; additional map keys must not duplicate declared property
names.

## Supported Keywords

The profile supports these binding and validation keywords:

| Category | Keywords |
|---|---|
| Shape | `type`, `properties`, `required`, `additionalProperties`, `items`, `oneOf`, `$ref`, `$defs` |
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
metadata accessors and optional schema metadata helpers, including root-object
metadata.

Same-document `$ref` values are resolved before binding model construction.
Supported references must be string URI fragments using JSON Pointer form, such
as `"#/$defs/id"` or `"#"`. `$defs` is accepted as local definition storage.
Remote references, missing targets, invalid fragments, cycles, dynamic
references, anchors, and `$ref` objects with assertion or applicator siblings
fail before source emission. Generated readers, writers, validators, and
metadata helpers never perform runtime reference lookup.

## Accepted Annotations

The profile accepts these annotations and dialect markers:

| Annotation | Behavior |
|---|---|
| `$schema` | Recognized Draft 2020-12 dialect marker. |
| `title`, `description`, `$comment`, `examples` | Accepted annotations and exposed when metadata helpers are generated. |
| `deprecated`, `readOnly`, `writeOnly` | Accepted annotations and exposed when metadata helpers are generated. |

Unknown non-Draft extension keywords are ignored as annotations. Known Draft
2020-12 keywords are classified explicitly as supported or rejected.

## Draft 2020-12 Feature Matrix

The table below is the profile source of truth for known JSON Schema keywords.
`Supported` means the keyword is accepted for generated binding behavior.
`Supported with profile limits` means the keyword is accepted only in the
documented shape. `Accepted annotation` means the keyword can appear without
changing binding, reader, writer, or validator behavior. `Recommended next`
means the feature is currently rejected but has a good usefulness, commonness,
and implementation-complexity tradeoff for post-v1 work. `Deferred - poor
tradeoff` means the feature is currently rejected and should not be implemented
until the project deliberately accepts the complexity. `Rejected` means the
keyword is intentionally outside the current generated-binding profile with no
current recommendation.

Unsupported features fail before source emission. The generator reports stable
`MJJBG-*` diagnostics with the schema file and exact schema JSON Pointer, such
as `/properties/value/allOf`.

### Core And Dialect Keywords

| Keyword | Vocabulary | Status | Profile behavior |
|---|---|---|---|
| `$schema` | Core | Accepted annotation | Recognizes the Draft 2020-12 dialect marker. |
| `$id` | Core | Rejected | No schema resource identifier graph is built in v1. |
| `$anchor` | Core | Rejected | No named-anchor resolution is performed in v1. |
| `$dynamicAnchor` | Core | Deferred - poor tradeoff | Dynamic scope support conflicts with the current static binding model. |
| `$vocabulary` | Core | Deferred - poor tradeoff | Custom vocabulary negotiation is outside the single-profile generator contract. |
| `$ref` | Core | Supported with profile limits | Same-document JSON Pointer fragments are resolved before binding; remote references, cycles, missing targets, invalid fragments, and assertion/applicator siblings are rejected. |
| `$dynamicRef` | Core | Deferred - poor tradeoff | Dynamic reference resolution requires schema evaluation machinery not present in generated bindings. |
| `$defs` | Core | Supported with profile limits | Accepted as same-document definition storage for local `$ref`; definition schemas are validated under the same profile. |
| `$comment` | Core | Accepted annotation | Exposed when metadata helpers are generated. |

### Applicator And Shape Keywords

| Keyword | Vocabulary | Status | Profile behavior |
|---|---|---|---|
| `type` | Validation | Supported with profile limits | Root objects, field scalars, arrays, and nullable pairs containing exactly `null` plus one supported non-null type are accepted. |
| `properties` | Applicator | Supported with profile limits | Root, tagged-branch, and nested object properties are accepted when every property maps to a supported field shape. |
| `required` | Validation | Supported | Drives required field generation and reader/validator required-property checks. |
| `additionalProperties` | Applicator | Supported with profile limits | Accepted as literal `false` for closed objects or as a supported object-valued schema for `Map<String, T>` catch-all bindings. |
| `items` | Applicator | Supported with profile limits | Accepted only for homogeneous scalar array items. |
| `oneOf` | Applicator | Supported with profile limits | Accepted only for root tagged object unions with one common required string `const` tag. |
| `allOf` | Applicator | Supported with profile limits | Supported only for constrained object schemas whose branches flatten into one deterministic object binding; conflicting properties and annotations are rejected, and schema objects that constrain `additionalProperties` must declare every merged property locally. |
| `anyOf` | Applicator | Deferred - poor tradeoff | Generic union matching requires broader runtime/schema evaluation than the current generator design. |
| `not` | Applicator | Deferred - poor tradeoff | Negative schema assertions are hard to represent as static Java bindings. |
| `if` | Applicator | Deferred - poor tradeoff | Conditional evaluation depends on generic schema matching. |
| `then` | Applicator | Deferred - poor tradeoff | Conditional evaluation depends on generic schema matching. |
| `else` | Applicator | Deferred - poor tradeoff | Conditional evaluation depends on generic schema matching. |
| `dependentSchemas` | Applicator | Deferred - poor tradeoff | Low observed frequency and requires subschema evaluation after property presence checks. |
| `prefixItems` | Applicator | Deferred - poor tradeoff | Tuple arrays are uncommon in the corpus and do not fit the homogeneous-list model. |
| `contains` | Applicator | Deferred - poor tradeoff | Containment validation is uncommon and interacts with `minContains`, `maxContains`, and unevaluated item tracking. |
| `patternProperties` | Applicator | Recommended next | Regex-key map fields are useful after map binding support exists. |
| `propertyNames` | Applicator | Recommended next | Useful as generated object/map key validation without broad schema interpretation. |
| `unevaluatedItems` | Applicator | Deferred - poor tradeoff | Requires annotation-dependent tracking across applicator evaluation. |
| `unevaluatedProperties` | Applicator | Deferred - poor tradeoff | Requires annotation-dependent tracking across object applicator evaluation. |

### Validation Keywords

| Keyword | Vocabulary | Status | Profile behavior |
|---|---|---|---|
| `enum` | Validation | Supported with profile limits | Supported for compatible scalar values and scalar array items. |
| `const` | Validation | Supported with profile limits | Supported for compatible scalar values, scalar array items, and tagged `oneOf` tag fields. |
| `minItems` | Validation | Supported | Generated validators enforce homogeneous array lower bounds. |
| `maxItems` | Validation | Supported | Generated validators enforce homogeneous array upper bounds. |
| `minLength` | Validation | Supported | Generated validators enforce string lower bounds by Unicode code point count. |
| `maxLength` | Validation | Supported | Generated validators enforce string upper bounds by Unicode code point count. |
| `pattern` | Validation | Supported with profile limits | Java `Pattern` is compiled during generation and checked with JSON Schema search semantics. |
| `format` | Validation | Supported with profile limits | Only `date`, `date-time`, and `uuid` assertions are supported. |
| `minimum` | Validation | Supported | Generated validators compare numeric values against exact schema literals. |
| `maximum` | Validation | Supported | Generated validators compare numeric values against exact schema literals. |
| `exclusiveMinimum` | Validation | Supported | Generated validators compare numeric values against exact schema literals. |
| `exclusiveMaximum` | Validation | Supported | Generated validators compare numeric values against exact schema literals. |
| `multipleOf` | Validation | Recommended next | Numeric divisibility is a low-risk generated validator addition. |
| `uniqueItems` | Validation | Recommended next | Scalar-array uniqueness is a low-to-medium complexity generated validator addition. |
| `maxContains` | Validation | Deferred - poor tradeoff | Depends on deferred `contains` support. |
| `minContains` | Validation | Deferred - poor tradeoff | Depends on deferred `contains` support. |
| `maxProperties` | Validation | Recommended next | Useful as generated object/map size validation. |
| `minProperties` | Validation | Recommended next | Useful as generated object/map size validation. |
| `dependentRequired` | Validation | Recommended next | Useful as generated property-presence validation for closed object bindings. |

### Metadata And Content Keywords

| Keyword | Vocabulary | Status | Profile behavior |
|---|---|---|---|
| `title` | Metadata | Accepted annotation | Exposed when metadata helpers are generated. |
| `description` | Metadata | Accepted annotation | Exposed when metadata helpers are generated. |
| `default` | Metadata | Supported with profile limits | Treated as an annotation and exposed through supported metadata helpers; it does not affect construction, reading, writing, or validation. |
| `deprecated` | Metadata | Accepted annotation | Exposed when metadata helpers are generated. |
| `readOnly` | Metadata | Accepted annotation | Exposed when metadata helpers are generated. |
| `writeOnly` | Metadata | Accepted annotation | Exposed when metadata helpers are generated. |
| `examples` | Metadata | Accepted annotation | Exposed when metadata helpers are generated. |
| `contentEncoding` | Content | Deferred - poor tradeoff | Low value for static Java binding unless encoded string helper APIs are added. |
| `contentMediaType` | Content | Deferred - poor tradeoff | Low value for static Java binding unless media-aware string helper APIs are added. |
| `contentSchema` | Content | Deferred - poor tradeoff | Requires content decoding plus nested schema evaluation. |

### Legacy And Out-Of-Profile Keywords

| Keyword | Vocabulary | Status | Profile behavior |
|---|---|---|---|
| `dependencies` | Legacy | Deferred - poor tradeoff | Draft-07 legacy keyword; use Draft 2020-12 `dependentRequired` or `dependentSchemas` when support is added. |
| `definitions` | Legacy | Rejected | Draft-07 legacy keyword; `$defs` is the Draft 2020-12 replacement. |
| `additionalItems` | Legacy | Deferred - poor tradeoff | Draft-07 tuple keyword superseded by `prefixItems` and incompatible with the homogeneous-list model. |

## Post-v1 Implementation Ranking

The ranking below combines the SchemaStore broader catalog scan from
2026-05-23 with the current generated-code architecture. Frequency is
document-level frequency among the 1,259 reachable and parseable SchemaStore
documents from that scan.

| Rank | Feature | Commonness | Usefulness | Complexity | Decision |
|---:|---|---:|---|---|---|
| 1 | Nested object property bindings | Very high inferred | Very high | High | Implemented in `TASK-0030`. |
| 2 | Internal `$defs` / local `$ref` resolution | 73.4% `$ref` | Very high | High | Implemented in `TASK-0031`. |
| 3 | Map bindings via object-valued `additionalProperties` | 48.2% | Very high | High | Implemented in `TASK-0032`. |
| 4 | Constrained object `allOf` flattening | 25.2% | High | High | Implemented in `TASK-0033`. |
| 5 | `patternProperties` map bindings | 22.8% | High | High | Create `TASK-0034`. |
| 6 | Object validation keywords: `minProperties`, `maxProperties`, `propertyNames`, `dependentRequired` | 5.7% / low | Medium | Medium | Create `TASK-0035`. |
| 7 | Low-risk scalar/array validators: `multipleOf`, `uniqueItems` | Not in scan | Medium | Low-medium | Create `TASK-0036`. |

### Deferred Low-Tradeoff Features

The following features are common enough to notice but do not currently have a
good complexity tradeoff for this project:

- Generic `oneOf`, `anyOf`, `not`, `if` / `then` / `else`, and broad non-null
  `type` unions require general schema matching or ambiguous Java value shapes.
- `prefixItems`, tuple-style array `items`, `contains`, `minContains`, and
  `maxContains` are less common and would pull array generation away from the
  current homogeneous-list model.
- `unevaluatedItems`, `unevaluatedProperties`, `$dynamicRef`,
  `$dynamicAnchor`, and `$vocabulary` require vocabulary or
  annotation-dependent evaluation machinery that the static generator does not
  have.
- `contentEncoding`, `contentMediaType`, and `contentSchema` are low value for
  generated Java records unless the project first adds encoded-content helper
  APIs.

## Generated Behavior

Generated readers parse through the project-owned `JsonReader` interface,
reject duplicate and unknown properties, require all required properties, and
require full-document consumption. Reader diagnostics use stable `MJJBR-*`
codes; parser failures retain stable `MJJBP-*` codes and are re-pathed when the
generated reader knows the active instance path.

Generated Java field names are derived deterministically from JSON property
names. When multiple JSON properties normalize to the same Java identifier, the
later fields receive numeric suffixes such as `userId2` while metadata retains
the original JSON property names.

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
