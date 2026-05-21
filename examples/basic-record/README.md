# Basic Record Example

This example demonstrates a closed object binding generated from
`src/main/schema/basic-record.schema.json`.

The generated sources are checked in under `generated-src/main/java` so the
example can be built and tested directly:

```bash
./gradlew :examples:basic-record:check --console=plain
```

The schema contains required `id` and `count` fields plus optional scalar
fields. The example tests exercise generated reading, validation, writing, and
deterministic failure diagnostics.
