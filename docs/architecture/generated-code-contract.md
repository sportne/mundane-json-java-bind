# Generated Code Contract

Generated source must be production-quality Java 21.

## Rules

1. Use records for object models where the binding shape is representable.
2. Use final classes only when records cannot express the binding cleanly.
3. Required non-null values use direct Java types.
4. Optional non-null values use `Optional<T>`.
5. Nullable or absent-vs-null-sensitive values use `JsonField<T>`.
6. Arrays use immutable `List<T>` values with defensive copies.
7. Readers use explicit token handling and reject duplicate properties.
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
