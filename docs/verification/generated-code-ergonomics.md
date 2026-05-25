# Generated-Code Ergonomics Review

TASK-0041 reviewed generated models, readers, writers, validators, metadata
helpers, examples, and generated-code smoke fixtures after the post-1.1 profile
expansion. This is a review report only; no generated-code behavior changed.

## Reviewed Workflow

The realistic supported workflow used for review combines the generated-code
contract with these smoke fixtures:

| Capability | Representative fixture | Ergonomic observation |
|---|---|---|
| Nested closed objects | `nested-object` | Nested records are readable and strongly typed, but names grow with path depth, such as `GeneratedBindingsProfileAddress`. |
| Local `$defs` / `$ref` | `local-ref` | Reference resolution is invisible in generated source, which keeps generated code simple; users need metadata/schema docs to understand where reused shapes came from. |
| Object-valued `additionalProperties` | `additional-properties-nested-object-map` | `Map<String, NestedRecord>` is explicit and Java-native, but record construction becomes verbose for mixed declared/map objects. |
| `patternProperties` plus `additionalProperties` | `pattern-properties-map` | Separate `patternProperties` and `additionalProperties` maps make routing deterministic, though the field names are schema-keyword-oriented rather than domain-oriented. |
| Constrained object `allOf` | `allof-flattened-object` | Flattened records are pleasant for callers because composition disappears; diagnostics and metadata remain important for explaining merged constraints. |
| Object and scalar validators | `object-validation-keywords`, `multipleof-uniqueitems` | Static validator helpers are easy to call, but validation is separate from construction for most model-level constraints. |

The current generated shape is coherent: Java records expose plain data,
readers/writers stay stateless, validators accumulate structured errors, and
metadata helpers keep schema annotations out of runtime binding logic. The main
ergonomic gaps are now around discoverability and construction convenience, not
core generated-source correctness.

## Current Strengths

| Area | Finding |
|---|---|
| Static Java shape | Records, nested records, sealed tagged roots, `Optional`, `JsonField`, and `Map` components are easy to inspect in generated source. |
| Deterministic output | Property order, map entry order, nested type names, branch names, and helper class names are stable enough for golden-source review. |
| Runtime dependency boundary | Generated source depends on `runtime-core` and JDK types only; parser usage remains caller-owned through `JsonReader` and `JsonWriter`. |
| Failure model | Reader and validator diagnostics use stable codes and `JsonPath`, which is stronger than plain exception strings for generated-code users. |
| Metadata helpers | Optional metadata helpers expose schema pointers and annotations without adding runtime schema interpretation. |

## Usability Friction

| Priority | Friction | Impact | Suggested handling |
|---:|---|---|---|
| 1 | No public advanced example covers nested `$ref`, maps, and constrained `allOf` together. | Users can see the pieces in smoke fixtures but not a normal application workflow. | Add an advanced generated-binding example that compiles checked-in generated sources and demonstrates construction, read/write, validation, and metadata. |
| 2 | Record construction is verbose for optional, nullable, and map-heavy objects. | Java callers must manually supply `Optional.empty()`, `JsonField.absent()`, and empty maps for every optional shape. | Consider generated convenience factories or a builder as an opt-in contract decision, not a default behavior change. |
| 3 | Keyword-derived map fields (`patternProperties`, `additionalProperties`) are precise but not domain-friendly. | Generated APIs for map-heavy schemas read like schema mechanics rather than domain model names. | Keep the current names for determinism; evaluate metadata-first docs or future naming customization before changing generated contracts. |
| 4 | Nested type names can become long in deep object graphs. | Names remain deterministic but can reduce readability in schemas with repeated nested containers. | Keep path-derived names for now; audit actual SchemaStore depth in future corpus freshness work before adding naming knobs. |
| 5 | Pattern map constructors compile regexes inline in generated compact constructors. | Behavior is correct, but readability and repeated construction overhead are weaker than a generated static `Pattern` constant. | Candidate small implementation task: emit private static final patterns for model/reader/writer/validator use. |
| 6 | Validation is intentionally separate from construction for most constraints. | Callers may assume a constructed record is fully schema-valid when only null/map-key invariants are constructor-enforced. | Improve examples and README wording before considering constructor-level validation hooks. |
| 7 | Metadata default accessors are typed only for supported scalar defaults. | This is simple and safe, but users may expect object/array defaults to have typed helpers. | Keep current boundary; document raw annotation access as the fallback. |

## Follow-Up Candidates

| Rank | Candidate | Size | Why |
|---:|---|---|---|
| 1 | Add an `advanced-profile` example covering nested `$ref`, map bindings, constrained `allOf`, validation, and metadata. | Medium | Highest documentation leverage; no generated-code contract change. |
| 2 | Generate shared static `Pattern` constants for `patternProperties` key checks. | Small | Improves readability and construction performance without changing public behavior. |
| 3 | Add a generated-source behavior probe for a declared property named `additionalProperties` plus an additional map. | Small | Completes the external issue-mining follow-up at generated reader/writer/validator level. |
| 4 | Decide whether optional builder/factory generation belongs in the project. | Large | Helpful for construction ergonomics, but increases generated surface area and maintenance. |
| 5 | Add a naming stability audit over generated smoke fixtures and SchemaStore-supported samples. | Medium | Makes path-derived name behavior explicit before any naming customization is considered. |

## Recommendation

Do not change the generated-code contract yet. The current shape is consistent
with the project goal of simple, static, dependency-light Java bindings. The
next best step is an advanced checked-in example because it teaches existing
supported workflows without adding API surface. After that, the static-pattern
cleanup and the `additionalProperties` collision behavior probe are small,
low-risk hardening tasks.
