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

Schema document syntax parsing lives in `schema-model`. It parses JSON Schema
documents as JSON syntax values with schema JSON Pointer locations; runtime JSON
instance parsing remains the responsibility of `parser-core` and generated
readers.

## JSP-DATA-2020-12 Support Matrix

| Category | Keywords |
|---|---|
| Supported binding keywords | `type`, `properties`, `required`, `additionalProperties`, `enum`, `const`, `default`, `items`, `minItems`, `maxItems`, `minimum`, `maximum`, `exclusiveMinimum`, `exclusiveMaximum`, `minLength`, `maxLength`, `pattern`, `format`, `oneOf` |
| Accepted ignored annotations and dialect markers | `$schema`, `title`, `description`, `$comment`, `examples`, `deprecated`, `readOnly`, `writeOnly` |
| Known rejected Draft 2020-12 keywords | `$id`, `$anchor`, `$dynamicAnchor`, `$vocabulary`, `$ref`, `$dynamicRef`, `$defs`, `allOf`, `anyOf`, `not`, `if`, `then`, `else`, `dependentSchemas`, `prefixItems`, `contains`, `patternProperties`, `propertyNames`, `unevaluatedItems`, `unevaluatedProperties`, `multipleOf`, `uniqueItems`, `maxContains`, `minContains`, `maxProperties`, `minProperties`, `dependentRequired`, `contentEncoding`, `contentMediaType`, `contentSchema` |

Unknown non-Draft extension keywords are ignored as annotations. Known Draft
2020-12 keywords are always classified explicitly by the profile matrix.
