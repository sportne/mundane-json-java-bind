# Generator Simplification Audit

This audit reviews `generator-core` after the 1.1 profile expansion. The goal
is to protect the project's simplicity constraint before adding more schema
surface area.

The generated-code contract remains the controlling constraint: generated
models, readers, writers, validators, and metadata helpers stay explicit,
reflection-free Java 21 source. The simplification target is the generator
implementation, not the generated behavior.

## Current Shape

`generator-core` has a small public interface through `CoreGenerator`, and that
interface is still deep: callers provide a `GeneratorRequest` and get generated
source paths or deterministic diagnostics. Most complexity is behind that
interface.

The internal generator implementation is now concentrated in a few large
modules:

| Module | Approximate size | Main responsibility |
|---|---:|---|
| [ValidatorSourceEmitter.java](/mnt/d/projects/mundane-json-java-bind/modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/internal/emitter/ValidatorSourceEmitter.java) | 2,029 lines / 90 methods | Emits model validator source, helper selection, scalar/array/map/object validation, literal constraints, and traversal helpers. |
| [BindingModelBuilder.java](/mnt/d/projects/mundane-json-java-bind/modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/internal/binding/BindingModelBuilder.java) | 1,532 lines / 85 methods | Converts resolved schema syntax into binding IR, including object flattening, map binding, value typing, facets, annotations, literals, names, and diagnostics. |
| [ReaderSourceEmitter.java](/mnt/d/projects/mundane-json-java-bind/modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/internal/emitter/ReaderSourceEmitter.java) | 1,106 lines / 52 methods | Emits reader source, tagged dispatch, object readers, map routing, scalar/array helpers, and traversal helpers. |
| [ModelSourceEmitter.java](/mnt/d/projects/mundane-json-java-bind/modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/internal/emitter/ModelSourceEmitter.java) | 568 lines / 26 methods | Emits records, compact constructors, default accessors, map invariants, and traversal helpers. |
| [WriterSourceEmitter.java](/mnt/d/projects/mundane-json-java-bind/modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/internal/emitter/WriterSourceEmitter.java) | 565 lines / 29 methods | Emits writers, nested object writers, map writers, finite-number checks, and traversal helpers. |

The large modules are not automatically bad. They are doing real work. The
friction is that several internal interfaces are implicit rather than named, so
new feature work requires a maintainer to hold too many facts in working memory.

## Friction Points

### 1. Binding Traversal Is Repeated In Every Emitter

`ModelSourceEmitter`, `ReaderSourceEmitter`, `WriterSourceEmitter`, and
`ValidatorSourceEmitter` each define local variants of:

- `allFields`
- `allMaps`
- `nestedObjects`
- `collectFields`
- `collectMaps`
- `collectNestedObjects`

Concrete examples:

- [ModelSourceEmitter.java](/mnt/d/projects/mundane-json-java-bind/modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/internal/emitter/ModelSourceEmitter.java:143)
- [ReaderSourceEmitter.java](/mnt/d/projects/mundane-json-java-bind/modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/internal/emitter/ReaderSourceEmitter.java:959)
- [WriterSourceEmitter.java](/mnt/d/projects/mundane-json-java-bind/modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/internal/emitter/WriterSourceEmitter.java:441)
- [ValidatorSourceEmitter.java](/mnt/d/projects/mundane-json-java-bind/modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/internal/emitter/ValidatorSourceEmitter.java:1572)

Deletion test: deleting one copy does not remove complexity; the same recursive
rules reappear in the other emitters. This is a strong signal for a real
internal module.

Recommended simplification: add a package-private traversal module with a small
interface, probably static methods returning fields, maps, nested objects, and
all objects in deterministic order. This module would give emitters leverage
and locality without changing generated source.

Risk: low. The behavior is already duplicated and covered by golden/generated
smoke tests.

### 2. Java Source Assembly Has Repeated Micro-Interfaces

Each emitter owns its own small text helpers for indentation, Java string
literals, generated type names, path expressions, and helper method naming.
Some duplication is harmless because each emitter has domain-specific source
shape. The repeated `javaStringLiteral` and `indent` helpers are different:
they encode general Java source-writing facts rather than reader/writer/model
behavior.

Concrete examples:

- [ModelSourceEmitter.java](/mnt/d/projects/mundane-json-java-bind/modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/internal/emitter/ModelSourceEmitter.java:170)
- [ReaderSourceEmitter.java](/mnt/d/projects/mundane-json-java-bind/modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/internal/emitter/ReaderSourceEmitter.java:1082)
- [WriterSourceEmitter.java](/mnt/d/projects/mundane-json-java-bind/modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/internal/emitter/WriterSourceEmitter.java:541)
- [ValidatorSourceEmitter.java](/mnt/d/projects/mundane-json-java-bind/modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/internal/emitter/ValidatorSourceEmitter.java:1432)

Deletion test: deleting these helpers would spread string escaping and
indentation details through many call sites. They earn their keep, but their
current location makes each emitter an island.

Recommended simplification: add a tiny package-private Java source text module
for escaping and indentation only. Avoid a broad source-generation framework.
The interface should stay small enough that callers do not have to learn a new
DSL to read emitted output.

Risk: low-medium. It can cause broad golden churn if formatting changes, so the
first slice should preserve exact output.

### 3. `BindingModelBuilder` Owns Too Many Distinct Decisions

`BindingModelBuilder` currently owns these separable responsibilities:

- root object and tagged union selection;
- constrained `allOf` flattening;
- property and map binding;
- scalar, nullable, array, object, literal, facet, and annotation extraction;
- Java field/type name allocation;
- binding diagnostics and sorting.

Concrete hotspots include constrained `allOf` flattening in
[BindingModelBuilder.java](/mnt/d/projects/mundane-json-java-bind/modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/internal/binding/BindingModelBuilder.java:480),
annotation and JSON literal handling in
[BindingModelBuilder.java](/mnt/d/projects/mundane-json-java-bind/modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/internal/binding/BindingModelBuilder.java:675),
and Java name allocation in
[BindingModelBuilder.java](/mnt/d/projects/mundane-json-java-bind/modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/internal/binding/BindingModelBuilder.java:1300).

This module still has good locality because schema-to-binding decisions happen
in one place. The problem is depth at its internal interface: adding a small
schema feature requires reading unrelated naming, literal, annotation, and
composition code.

Recommended simplification: split only where the extracted module can hide a
stable decision. Good first candidates are:

- `JavaNameAllocator`: field/type/tag naming and collision handling.
- `SchemaLiteralReader`: canonical JSON, compact JSON, literal constraints,
  and annotation JSON preservation.
- `ObjectShapeFlattener`: constrained `allOf` object merge rules.

Avoid extracting one-method pass-through modules. The split should reduce the
facts a feature author needs to know, not just shorten a file.

Risk: medium. The builder is heavily covered by `BindingModelBuilderTest`, but
schema diagnostics and pointer locations are easy to perturb.

### 4. Validator Emission Has The Highest Future Complexity Risk

`ValidatorSourceEmitter` grew with every supported validation keyword. It now
contains root dispatch, branch validators, nested validators, map validators,
object keyword validators, scalar facets, numeric facets, literal constraints,
helper selection, and binding traversal.

The densest regions are map validation at
[ValidatorSourceEmitter.java](/mnt/d/projects/mundane-json-java-bind/modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/internal/emitter/ValidatorSourceEmitter.java:280),
object keyword validation at
[ValidatorSourceEmitter.java](/mnt/d/projects/mundane-json-java-bind/modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/internal/emitter/ValidatorSourceEmitter.java:366),
field/array validation at
[ValidatorSourceEmitter.java](/mnt/d/projects/mundane-json-java-bind/modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/internal/emitter/ValidatorSourceEmitter.java:578),
and helper selection/traversal at
[ValidatorSourceEmitter.java](/mnt/d/projects/mundane-json-java-bind/modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/internal/emitter/ValidatorSourceEmitter.java:1319).

The external interface is simple: `emit(BindingModel)`. Internally, however,
feature logic is split by value shape and by keyword. For example, scalar
fields, nullable fields, arrays, and maps each need variants of string,
numeric, and literal validation logic.

Recommended simplification: do not start with a large rewrite. First remove
shared traversal and source-text duplication. Then consider a validation-plan
module that lowers `BindingModel` into rule descriptions such as "string facet
on field", "numeric facet on array item", and "object property count". The
emitter would then only render rules.

Risk: high. This is where a shallow abstraction would be worse than the current
long file. Any validation-plan module must hide real branching complexity and
must be tested through generated-source behavior.

### 5. Core Generator Tests Are Doing Repetitive Fixture Work

`CoreGeneratorTest` is useful but large. It repeats the same generated source
verification pattern across many fixtures:

- generate;
- locate model/writer/reader/validator;
- compare goldens;
- verify allowed tokens;
- compile generated source.

For example, the empty-object fixture path in
[CoreGeneratorTest.java](/mnt/d/projects/mundane-json-java-bind/modules/generator-core/src/test/java/io/github/mundanej/mjjb/generator/core/CoreGeneratorTest.java:43)
performs the same generate, locate, compare, boundary-check, and compile
sequence repeated by later fixture tests.

The generated-code smoke fixture harness already represents a deeper test
interface for this style of assertion.

Recommended simplification: migrate repeated golden fixture checks toward the
generated-code smoke fixture interface where possible, leaving `CoreGeneratorTest`
for generator API behavior, diagnostics, output path handling, and integration
edges.

Risk: medium. Test-only, but coverage can accidentally narrow if migration is
done mechanically.

## Recommended Order

| Order | Work | Why first |
---:|---|---|
| 1 | Extract binding traversal helpers for emitters. | Low risk, removes repeated recursive rules from four emitters, and reduces future feature touch points. |
| 2 | Extract Java source text helpers for exact escaping and indentation. | Low risk if output is preserved; makes emitter code easier to scan. |
| 3 | Split `BindingModelBuilder` naming into a `JavaNameAllocator`. | Naming is self-contained and currently far from the schema-shape code that calls it. |
| 4 | Split `BindingModelBuilder` literal/annotation reading. | Canonical JSON and literal extraction are coherent and independently testable. |
| 5 | Evaluate an `ObjectShapeFlattener` module for constrained `allOf`. | Good locality gain, but higher diagnostic-location risk. |
| 6 | Only after the above, design a validator-plan module. | Highest potential payoff, highest risk of creating a shallow abstraction. |
| 7 | Reduce `CoreGeneratorTest` fixture repetition. | Useful after emitter behavior is stable, but less urgent than production generator locality. |

## Non-Recommendations

- Do not introduce a general source-generation DSL. The generated Java is simple
  enough that a broad DSL would likely become another interface to learn.
- Do not split every helper out of the emitters. Some source-shape knowledge is
  legitimately local to a specific emitter.
- Do not combine reader, writer, model, and validator emitters. Their public
  generated artifacts are separate and the current module split matches that
  output shape.
- Do not pursue generic `anyOf` or conditional schema support until this
  simplification work has reduced the cost of adding validation behavior.

## Suggested Follow-Up Tasks

These should be created only after the maintainer chooses to proceed:

- Extract emitter binding traversal helpers.
- Extract Java source text helpers with exact-output golden preservation.
- Extract Java name allocation from `BindingModelBuilder`.
- Extract literal and annotation reading from `BindingModelBuilder`.
- Evaluate constrained `allOf` flattening as a dedicated internal module.
- Design a validator-plan module after smaller simplifications land.
