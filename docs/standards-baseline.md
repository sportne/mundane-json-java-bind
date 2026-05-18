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
