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

Future nullable or absent-vs-null-sensitive fields use `JsonField<T>`:

```java
public record User(String id, Optional<String> name, JsonField<String> nickname) {}
```

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

`number` fields use `Double.toString` after an explicit `Double.isFinite` check.
`integer` fields use `Long.toString`. Semantic numeric constraints remain
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

Reader diagnostics use stable `MJJBR-*` codes for generated-binding failures:
root type mismatch, trailing root content, duplicate property, unknown property,
missing required property, and scalar type mismatch. Parser failures retain
their `MJJBP-*` codes; generated scalar readers re-path those failures to the
active JSON instance field path where the reader knows it.

`integer` fields parse JSON number literals as Java `long` values and reject
decimal, exponent, and out-of-range literals. `number` fields parse Java
`double` values and reject non-finite results. Semantic numeric constraints
remain validator responsibility.
