# Standards Baseline

The project targets JSON Schema Draft 2020-12.

## Normative Inputs

| Input | URI | Use |
|---|---|---|
| Core | <https://json-schema.org/draft/2020-12/json-schema-core.html> | Schema documents, dialects, vocabularies, identifiers, references, and JSON Pointer terminology. |
| Validation | <https://json-schema.org/draft/2020-12/json-schema-validation> | Validation keyword semantics for the supported subset. |
| Default meta-schema | <https://json-schema.org/draft/2020-12/schema> | Draft 2020-12 dialect identification. |
| JSON Schema Test Suite | <https://github.com/json-schema-org/JSON-Schema-Test-Suite> | Conformance evidence for accepted behavior and explicit skip evidence for unsupported behavior. |

## Policy

The implementation must not invent alternate semantics for supported keywords.
If a keyword is unsupported by the active generated-binding profile, generation
must fail with a deterministic diagnostic at the exact schema JSON Pointer.

The user-facing v1 profile contract is documented in
[`supported-profile.md`](supported-profile.md). This baseline remains the
standards traceability source for that profile.

Schema document syntax parsing lives in `schema-model`. It parses JSON Schema
documents as JSON syntax values with schema JSON Pointer locations; runtime JSON
instance parsing remains the responsibility of `parser-core` and generated
readers.

## JSP-DATA-2020-12 Support Matrix

| Category | Keywords |
|---|---|
| Supported binding keywords | `type`, `properties`, `required`, `additionalProperties`, `enum`, `const`, `default`, `items`, `minItems`, `maxItems`, `minimum`, `maximum`, `exclusiveMinimum`, `exclusiveMaximum`, `minLength`, `maxLength`, `pattern`, `format`, `oneOf` |
| Accepted ignored annotations and dialect markers | `$schema`, `title`, `description`, `$comment`, `examples`, `deprecated`, `readOnly`, `writeOnly` |
| Recommended post-v1 keywords | `$ref`, `$defs`, `allOf`, `patternProperties`, `propertyNames`, `multipleOf`, `uniqueItems`, `maxProperties`, `minProperties`, `dependentRequired` |
| Known rejected or deferred Draft 2020-12 keywords | `$id`, `$anchor`, `$dynamicAnchor`, `$vocabulary`, `$dynamicRef`, `anyOf`, `not`, `if`, `then`, `else`, `dependentSchemas`, `prefixItems`, `contains`, `unevaluatedItems`, `unevaluatedProperties`, `maxContains`, `minContains`, `contentEncoding`, `contentMediaType`, `contentSchema` |

Unknown non-Draft extension keywords are ignored as annotations. Known Draft
2020-12 keywords are always classified explicitly by the profile matrix in
[`supported-profile.md`](supported-profile.md). The recommended post-v1 list is
planning guidance only; those keywords still reject under `JSP-DATA-2020-12`
until their implementation tasks are complete.

Nested object property bindings are supported as a profile shape rather than a
new keyword: object-valued properties use the existing `type`, `properties`,
`required`, and `additionalProperties` keywords with closed-object semantics.

## Facet Notes

`JSP-DATA-2020-12` supports generated validation for `minLength`, `maxLength`,
`pattern`, `format`, `minimum`, `maximum`, `exclusiveMinimum`, and
`exclusiveMaximum` on scalar fields and homogeneous scalar array items.

Supported `format` assertions are limited to `date`, `date-time`, and `uuid`.
Other known format values are rejected by the schema profile until deliberately
added to the v1 subset.

`pattern` uses generated Java `Pattern` checks with JSON Schema search
semantics. Patterns must compile during schema/profile validation; there is no
runtime regex registry or extension lookup.

## JSON Schema Test Suite Traceability

`modules/conformance-tests` contains a curated allowlist and skip manifest tied
to the official JSON Schema Test Suite commit
`ba30ec795b67fb0bb636fedda3210b95d8cf558b`. The manifest is projected into the
generated root-object binding shape instead of interpreted dynamically; see
`docs/verification/json-schema-test-suite.md`.

| Supported category | Representative upstream fixture files | TASK-0022 coverage | Skip policy |
|---|---|---|---|
| Scalar `type` | `tests/draft2020-12/type.json` | String and boolean valid/invalid cases projected through a `value` property. | Root scalar schemas and broader integer equivalence are skipped as `ROOT_NON_OBJECT_BINDING` or `NUMERIC_SEMANTICS_DEFERRED`. |
| String length | `tests/draft2020-12/minLength.json`, `tests/draft2020-12/maxLength.json` | Boundary success and failure cases for generated string validators. | Non-string applicability cases remain outside the generated binding projection. |
| Numeric bounds | `tests/draft2020-12/minimum.json`, `maximum.json`, `exclusiveMinimum.json`, `exclusiveMaximum.json` | Boundary success and failure cases for generated `number` validators. | Broader numeric equivalence and unsupported numeric keywords stay deferred. |
| Array length | `tests/draft2020-12/minItems.json`, `maxItems.json` | Boundary success and failure cases for homogeneous integer arrays. | Tuple, containment, and unique-item fixtures are skipped by unsupported keyword policy. |
| Nested object properties | `tests/draft2020-12/properties.json` | Valid and invalid closed nested object property projections. | Generic object applicability cases that do not define generated binding shapes remain skipped. |
| Literal constraints | `tests/draft2020-12/enum.json`, `const.json` | Scalar match and mismatch cases through generated validators and reader failures. | Object/array literal constraints outside scalar or scalar-item bindings stay unsupported. |
| Object/applicator/reference behavior | `required.json`, `properties.json`, `allOf.json`, `additionalProperties.json`, `ref.json` | Representative unsupported cases are documented in the skip manifest. | Skips use `MISSING_REQUIRED_BINDING_SHAPE`, `UNTYPED_PROPERTY_SCHEMA`, `UNSUPPORTED_KEYWORD`, `UNSUPPORTED_KEYWORD_VALUE`, or `REMOTE_REFERENCE`. |
| Format assertions | `tests/draft2020-12/format.json` | Supported local formats are covered by generator tests. | Optional upstream formats outside `date`, `date-time`, and `uuid` are skipped as `OPTIONAL_FORMAT_SCOPE`. |
