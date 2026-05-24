# Generated Code Contract

Generated source must be production-quality Java 21.

## Rules

1. Use records for object models where the binding shape is representable.
2. Use final classes only when records cannot express the binding cleanly.
3. Required non-null values use direct Java types.
4. Optional non-null values use `Optional<T>`.
5. Nullable or absent-vs-null-sensitive values use `JsonField<T>`.
6. Arrays use immutable `List<T>` values with defensive copies.
7. Readers use explicit token handling, reject duplicate properties, and require
   full-document consumption.
8. Writers emit deterministic schema property order.
9. Validators accumulate errors by default and support fail-fast mode.
10. No binding annotations, reflection, ServiceLoader, runtime scanning,
    dynamic proxies, runtime code generation, or third-party generated-code
    dependencies.

## Initial Binding IR

`generator-core` owns an internal binding IR used between schema profile
validation and Java source emission. The IR is not part of the public generator
API and generated source must not depend on it.

The first object binding slice maps scalar JSON Schema types as follows:

| JSON Schema type | Required Java type | Optional Java type |
| --- | --- | --- |
| `string` | `String` | `Optional<String>` |
| `integer` | `long` | `Optional<Long>` |
| `number` | `double` | `Optional<Double>` |
| `boolean` | `boolean` | `Optional<Boolean>` |

Homogeneous arrays with scalar `items` map to immutable Java lists:

| JSON Schema array item type | Required Java type | Optional Java type |
| --- | --- | --- |
| `string` | `List<String>` | `Optional<List<String>>` |
| `integer` | `List<Long>` | `Optional<List<Long>>` |
| `number` | `List<Double>` | `Optional<List<Double>>` |
| `boolean` | `List<Boolean>` | `Optional<List<Boolean>>` |

Nullable field-level type arrays preserve absent, explicit null, and value
states with `JsonField<T>`:

| JSON Schema property type | Required Java type | Optional Java type |
| --- | --- | --- |
| `["null", "string"]` | `JsonField<String>` | `JsonField<String>` |
| `["null", "integer"]` | `JsonField<Long>` | `JsonField<Long>` |
| `["null", "number"]` | `JsonField<Double>` | `JsonField<Double>` |
| `["null", "boolean"]` | `JsonField<Boolean>` | `JsonField<Boolean>` |
| `["null", "array"]` with scalar `items` | `JsonField<List<T>>` | `JsonField<List<T>>` |

Closed object properties map to nested records owned by the root generated
type:

| JSON Schema property type | Required Java type | Optional Java type |
| --- | --- | --- |
| `object` with `additionalProperties: false` | nested record type | `Optional<NestedRecord>` |

Object-valued `additionalProperties` generates one non-null
`Map<String, T>` record component after declared properties. Constructors copy
the map and reject keys that duplicate declared property names. Generated
writers emit declared properties first in schema order, then additional map
entries sorted by key.

## Basic Object Model Shape

Generated basic object models are Java records. Record components are emitted in
schema property order. Required reference fields and optional fields are checked
in the compact constructor with `Objects.requireNonNull`; required primitive
fields are not checked.

```java
public record GeneratedBindings(String id, long count, Optional<String> name) {
  public GeneratedBindings {
    id = Objects.requireNonNull(id, "id");
    name = Objects.requireNonNull(name, "name");
  }
}
```

Nullable or absent-vs-null-sensitive fields use `JsonField<T>`:

```java
public record User(String id, Optional<String> name, JsonField<String> nickname) {}
```

`JsonField.absent()` means the JSON property was not present,
`JsonField.explicitNull()` means the property value was JSON `null`, and
`JsonField.value(value)` means the property was present with a non-null value.
Generated compact constructors reject null `JsonField` containers. Nullable
array values are defensively copied when the field has a value.

Array fields are defensively copied with `List.copyOf` in the compact
constructor. Required array fields reject null lists; optional array fields
reject null optional containers. Present arrays reject null elements through the
same copy operation. Accessors expose immutable lists.

Nested object fields generate public nested records inside the root model type.
Names are derived from the parent generated type and property name, with stable
numeric suffixes when normalized names collide. Nested records use the same
record-component ordering, required/optional semantics, compact-constructor
null checks, metadata annotations, and closed-object profile as root records.

Local `$ref` and `$defs` support is a generator-time normalization step. The
binding model sees only the resolved schema shape; generated model, reader,
writer, validator, and metadata helper sources contain no reference resolver,
schema registry, remote loader, or runtime schema lookup.

The JSON Schema `default` keyword is an annotation in generated code. It does
not affect constructors, readers, writers, or validation. For supported scalar
or `null` defaults on scalar fields, the generated model exposes static metadata
accessors named after the Java field, such as `defaultDisplayName()`. Accessors
return `Optional<T>` using the boxed Java scalar type; a schema default of
`null` returns `Optional.empty()`.

## Tagged oneOf Model Shape

The supported tagged `oneOf` form is a root schema whose branches are object
schemas distinguished by one common required string tag property with
branch-unique `const` values. The generated model is a sealed interface named
after the configured root type. Each branch is a nested record that implements
the sealed interface.

```java
public sealed interface Payment permits Payment.Card, Payment.BankTransfer {
  record Card(String last4, double amount) implements Payment {}

  record BankTransfer(String iban, Optional<Boolean> urgent) implements Payment {}
}
```

The tag property is a schema discriminator, not a record component. Branch
record components contain only non-tag schema properties and keep the same
scalar, array, nullable, facet, literal, and default behavior as basic object
bindings. Branch record names are derived from tag literals with deterministic
Java identifier normalization; normalized branch-name collisions are rejected
by the generator.

## Basic Object Writer Shape

Generated basic object writers are final, stateless utility classes named after
the generated model, such as `GeneratedBindingsJsonWriter`. Writers accept the
project-owned `JsonWriter` interface from `runtime-core`; generated source does
not construct or import parser implementations.

```java
public final class GeneratedBindingsJsonWriter {
  private GeneratedBindingsJsonWriter() {}

  public static void write(JsonWriter writer, GeneratedBindings value)
      throws JsonWriteException {
    Objects.requireNonNull(writer, "writer");
    Objects.requireNonNull(value, "value");
    writer.beginObject();
    writer.name("id");
    writer.value(value.id());
    writer.name("count");
    writer.number(Long.toString(value.count()));
    if (value.name().isPresent()) {
      writer.name("name");
      writer.value(value.name().orElseThrow());
    }
    writer.endObject();
  }
}
```

Writers emit object properties in schema order. Required scalar fields are always
written. Optional scalar fields are written only when their `Optional<T>` is
present; absent optionals are skipped rather than serialized as `null`.
Nullable fields are written only when not `JsonField.absent()`; explicit null
fields write JSON `null`, and value fields write the contained value.
Nested object fields delegate to generated private writer methods and preserve
schema order at every object depth.

For tagged `oneOf` roots, writers dispatch with explicit branch type checks,
write the tag property first with that branch's `const` value, and then write
branch fields in the branch schema property order.

Array fields are written with `beginArray`, item values in list iteration order,
and `endArray`. Optional array fields are skipped when absent.

`number` fields use `Double.toString` after an explicit `Double.isFinite` check.
`number` array items use the same finite check before writing. `integer` fields
and integer array items use `Long.toString`. Semantic numeric constraints remain
validator responsibility.

## Basic Object Reader Shape

Generated basic object readers are final, stateless utility classes named after
the generated model, such as `GeneratedBindingsJsonReader`. Readers accept the
project-owned `JsonReader` interface from `runtime-core`; generated source does
not construct or import parser implementations.

```java
public final class GeneratedBindingsJsonReader {
  private GeneratedBindingsJsonReader() {}

  public static GeneratedBindings read(JsonReader reader) throws JsonReadException {
    Objects.requireNonNull(reader, "reader");
    // Generated source checks the root token, dispatches known properties with
    // a switch, rejects duplicates and unknowns, and requires END_DOCUMENT.
  }
}
```

Readers accept object properties in any input order and construct the generated
record directly. Required fields are tracked with generated `seen` flags;
optional scalar fields default to `Optional.empty()` and become
`Optional.of(value)` when present. JSON `null` is not accepted for non-null
fields in this slice.

Nullable fields initialize to `JsonField.absent()`, become
`JsonField.explicitNull()` for JSON `null`, and become `JsonField.value(value)`
for non-null JSON values. Required nullable fields still require the property to
be present in input JSON.

Reader diagnostics use stable `MJJBR-*` codes for generated-binding failures:
root type mismatch, trailing root content, duplicate property, unknown property,
missing required property, and scalar type mismatch. Parser failures retain
their `MJJBP-*` codes; generated scalar readers re-path those failures to the
active JSON instance field path where the reader knows it.
Nested object readers recurse without buffering generic JSON objects. Duplicate,
unknown, missing-required, scalar, and parser diagnostics use the active nested
instance path, such as `$.profile.address.city`.

`integer` fields parse JSON number literals as Java `long` values and reject
decimal, exponent, and out-of-range literals. `number` fields parse Java
`double` values and reject non-finite results. Semantic numeric constraints
remain validator responsibility.

Array fields are streamed with `beginArray`, `hasNext`, scalar item reads, and
`endArray`; generated readers do not construct generic JSON value graphs. Array
item diagnostics use indexed instance paths such as `$.tags[0]`. Non-array
values for array fields use generated reader code `MJJBR-010`.

For tagged `oneOf` roots, readers require the root JSON value to be an object
and require the tag property to be the first object property. This is a v1
streaming constraint: the reader does not buffer a generic JSON object to find a
late discriminator. Missing or late tags are reported as missing required
properties at the tag path. Wrong tag token types use the string scalar
diagnostic at the tag path, and unknown tag values use `MJJBR-011`. After tag
dispatch, branch-specific parsing uses the same duplicate, unknown, missing
required, scalar, array, nullable, and trailing-root diagnostics as basic object
readers.

## Basic Object Validator Shape

Generated basic object validators are final, stateless utility classes named
after the generated model, such as `GeneratedBindingsJsonValidator`. Validators
operate on generated model instances and return `ValidationResult` values
instead of throwing for normal validation failures.

```java
public final class GeneratedBindingsJsonValidator {
  private GeneratedBindingsJsonValidator() {}

  public static ValidationResult validate(GeneratedBindings value) {
    return validate(value, ValidationMode.ACCUMULATE);
  }

  public static ValidationResult validate(GeneratedBindings value, ValidationMode mode) {
    Objects.requireNonNull(mode, "mode");
    ValidationErrors errors = ValidationErrors.create(mode);
    // Generated source validates object-level invariants and returns errors.
  }
}
```

The first validator slice checks root null values, required reference-field null
values, optional `Optional<T>` container null values, and finite `number` field
values. Primitive required scalar fields do not need null checks. Schema
locations may remain unknown in this slice.

Validators use stable `MJJBV-*` codes for generated-validator failures:
`MJJBV-001` for root object null, `MJJBV-002` for required null reference
fields, `MJJBV-003` for null optional containers, and `MJJBV-004` for non-finite
number values. Array validators also enforce `minItems` with `MJJBV-005` and
`maxItems` with `MJJBV-006`; array size errors report the array field path and
array item errors report indexed item paths.
Required nullable fields with `JsonField.absent()` use `MJJBV-017`.
Nested object validators delegate to generated private methods with the current
base path, so field and facet failures preserve paths such as
`$.profile.address.postalCode`.

Generated validators also enforce scalar `enum` and `const` constraints for
scalar fields and homogeneous scalar array items. Literal validation runs only
for present values; absent nullable or optional values are skipped. Explicit null
nullable fields match `enum` or `const` only when the schema literal set includes
`null`. No Java `enum` types are generated.

For tagged `oneOf` roots, validators accept the sealed root interface and
dispatch with explicit branch type checks. Branch field validation reuses the
same `MJJBV-*` codes and ordering as basic object validators. The branch type
itself represents the exactly-one supported branch; no reflection, annotations,
runtime subtype discovery, or generic one-of matching is used.

## Optional Schema Metadata Helpers

Metadata helper generation is opt-in through the public generator request. When
enabled, the generator emits one additional final utility class named after the
root type, such as `GeneratedBindingsJsonSchemaMetadata`.

```java
public final class GeneratedBindingsJsonSchemaMetadata {
  public static SchemaRootMetadata root() {}

  public static List<SchemaPropertyMetadata> properties() {}

  public static Optional<SchemaPropertyMetadata> property(String jsonName) {}

  public static List<SchemaBranchMetadata> branches() {}
}
```

The helper returns typed immutable metadata records from `runtime-core`. It may
include root, branch, and property schema pointers; Java type names; required,
nullable, and array flags; tagged `oneOf` tag metadata; and accepted annotation
values such as `title`, `description`, `$comment`, `examples`, `deprecated`,
`readOnly`, `writeOnly`, and `default`.

JSON-valued metadata is represented as deterministic compact JSON strings, not
as a runtime JSON tree. Metadata helpers are documentation and diagnostic aids
only. Generated models, readers, writers, and validators must not depend on
metadata helper presence for correctness, and no reflection, annotations,
ServiceLoader, runtime scanning, dynamic discovery, or schema interpretation is
introduced.
