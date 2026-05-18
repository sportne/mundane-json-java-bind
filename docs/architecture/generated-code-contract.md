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

## Example Shape

```java
public record User(String id, Optional<String> name, JsonField<String> nickname) {
  public User {
    Objects.requireNonNull(id, "id");
    name = Objects.requireNonNull(name, "name");
    nickname = Objects.requireNonNull(nickname, "nickname");
  }
}
```
