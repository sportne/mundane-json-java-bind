# External Issue Mining

TASK-0039 reviewed public issue trackers on 2026-05-24 for JSON Schema and
OpenAPI code generators with similar static binding pressures. The goal was not
to copy feature scope from those projects, but to find recurring failure modes
that should influence this project's tests, documentation, and future design.

## Reviewed Projects

| Project | Scope | Sampled issue themes |
|---|---|---|
| [jsonschema2pojo](https://github.com/joelittlejohn/jsonschema2pojo) | JSON Schema to Java POJOs | `additionalProperties`, composition, generated Java ergonomics. |
| [quicktype](https://github.com/glideapps/quicktype) | JSON/JSON Schema to many languages | Map properties, `allOf`, recursive references, type naming. |
| [OpenAPI Generator](https://github.com/OpenAPITools/openapi-generator) | OpenAPI to clients/servers/models | Java `additionalProperties`, polymorphic discriminators, composition. |
| [json-schema-to-typescript](https://github.com/bcherny/json-schema-to-typescript) | JSON Schema to TypeScript declarations | `additionalProperties` with declared properties, `$ref` plus `allOf`, union typing. |

## Mined Issue Evidence

| Source | Observed failure mode | Project impact |
|---|---|---|
| jsonschema2pojo [#1589](https://github.com/joelittlejohn/jsonschema2pojo/issues/1589) | Generated `additionalProperties` maps need conventional whole-map accessors for some downstream frameworks. | Not applicable for v1 records, but relevant to future ergonomics if JavaBean-style output is ever added. |
| jsonschema2pojo [#1120](https://github.com/joelittlejohn/jsonschema2pojo/issues/1120) | A schema property literally named `additionalProperties` can collide with generated catch-all map names. | Needs a narrow probe because this shape is inside the current profile. |
| jsonschema2pojo [#1649](https://github.com/joelittlejohn/jsonschema2pojo/issues/1649) and quicktype [#1574](https://github.com/glideapps/quicktype/issues/1574) | Users expect `allOf`, `anyOf`, and `oneOf` to merge or bind predictably, but broad composition is ambiguous for static types. | Covered for constrained `allOf`; generic composition remains future design, not an accidental gap. |
| quicktype [#2684](https://github.com/glideapps/quicktype/issues/2684) | Schema-valued `additionalProperties` must be represented in generated output, not silently dropped. | Covered by map bindings and generated behavior/conformance coverage. |
| quicktype [#2836](https://github.com/glideapps/quicktype/issues/2836) | Recursive `$ref` schemas can duplicate types and produce very large generated outputs. | Current profile rejects cycles; performance v2 measures source size but not recursive schema handling. |
| quicktype [#2778](https://github.com/glideapps/quicktype/issues/2778) | Type names can change unexpectedly across runs or inputs. | Mostly covered by golden generated sources and deterministic name allocation; continue watching in TASK-0041 ergonomics review. |
| OpenAPI Generator [#8055](https://github.com/OpenAPITools/openapi-generator/issues/8055) | Java map generation can emit uncompilable source for object-valued `additionalProperties`. | Covered by generated-source compilation gates and conformance behavior probes. |
| OpenAPI Generator [#23276](https://github.com/OpenAPITools/openapi-generator/issues/23276) and [#19194](https://github.com/OpenAPITools/openapi-generator/issues/19194) | `oneOf`/`anyOf` plus discriminator variants can lose branches or fail code generation, especially with enum discriminators. | Covered only for this project's deliberately constrained tagged root `oneOf`; generic discriminator unions remain deferred. |
| json-schema-to-typescript [#402](https://github.com/bcherny/json-schema-to-typescript/issues/402) | `additionalProperties` value types can conflict with declared property value types in target-language type systems. | Covered by separate Java record fields plus a catch-all map that excludes declared names. |
| json-schema-to-typescript [#395](https://github.com/bcherny/json-schema-to-typescript/issues/395) | `required` constraints can be lost when a property references an `allOf` definition. | Covered for local `$ref` followed by constrained `allOf` flattening; keep conformance trace active. |

## Risk Classification

| Risk | Classification | Rationale and action |
|---|---|---|
| Catch-all map is omitted for schema-valued `additionalProperties`. | Covered | Current binding model emits `Map<String, T>`, readers collect unknown names, validators check map values, and writers emit deterministic map entries. |
| Declared property named `additionalProperties` collides with generated map component. | Partially covered by new probe | Added a binding-level regression probe that expects the declared field to keep `additionalProperties` and the generated map to receive the deterministic suffix `additionalProperties2`; a generated-source behavior probe is still recommended below. |
| Declared property value type conflicts with catch-all map value type. | Covered | Java records keep declared properties and catch-all entries as different components; generated readers reject duplicate declared names before map insertion. |
| Generated source for maps fails to compile. | Covered | Golden fixtures, generated-source compilation, behavior probes, and the v2 performance compile case exercise map-capable generated Java. |
| Constrained object `allOf` loses required fields after local `$ref`. | Covered | `$ref` resolution runs before binding construction, and constrained `allOf` flattening unions required names while rejecting incompatible merges. |
| Broad `oneOf` / `anyOf` / discriminator unions lose variants. | Needs future design | The profile only supports root tagged `oneOf`. Generic branch matching would require a broader schema evaluation model and remains a poor tradeoff until a future charter changes that boundary. |
| Conditional `allOf` with `if` / `then` / `else` is expected to merge into output. | Not applicable | Conditional applicators are explicitly deferred because they require generic instance matching, not deterministic binding construction. |
| Recursive `$ref` duplicates generated types or causes unbounded output. | Covered as rejection; needs future design only if cycles are accepted | Cycles are rejected before emission. If recursive model support is ever considered, it needs a separate design for type identity, validation recursion, and output-size limits. |
| Type and field names change unexpectedly. | Needs follow-up in TASK-0041 | Current generated-source golden files catch common drift, but the ergonomics review should inspect broader name stability and collision behavior across nested objects, maps, and tagged branches. |
| Framework-specific JavaBean expectations for generated models. | Not applicable | The project intentionally emits Java records and static helpers, not mutable JavaBeans. This should remain documented as a profile boundary. |
| Large-schema memory or source-size blowups. | Needs follow-up in TASK-0040 / performance evidence | TASK-0038 measures representative generated source size and compile time. TASK-0040 should keep corpus evidence fresh enough to spot broad schema-size pressure. |

## Added Probe

`BindingModelBuilderTest.disambiguatesAdditionalPropertiesMapFromDeclaredPropertyName`
guards the jsonschema2pojo-style collision where a schema declares both:

- a normal property named `additionalProperties`; and
- an object-valued `additionalProperties` keyword.

The expected binding keeps the declared Java field name as
`additionalProperties` and gives the generated map field the deterministic
suffix `additionalProperties2`.

## Follow-Up Candidates

These are recommendations only; no new task files were created in TASK-0039.

| Priority | Candidate | Reason |
|---:|---|---|
| 1 | Add a generated-source or behavior-level probe for property/map name collisions. | The new binding probe proves model allocation; an end-to-end probe would also lock reader/writer/validator generated source behavior. |
| 2 | In TASK-0041, audit deterministic naming across nested object types, tagged branch names, map fields, and Java keywords. | Naming drift appears repeatedly in external generators and directly affects user trust in generated APIs. |
| 3 | In TASK-0040, include source-size and diagnostic counts in SchemaStore freshness notes when feasible. | Recursive or very large schemas are rejected or partially unsupported today, but corpus pressure should remain visible. |
| 4 | Keep generic discriminator unions deferred unless a future ADR accepts runtime branch matching complexity. | External issue histories show this is a high-risk area for silent data loss and non-compilable output. |
