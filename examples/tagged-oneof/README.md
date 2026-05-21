# Tagged oneOf Example

This example demonstrates the supported tagged `oneOf` root form from
`src/main/schema/tagged-oneof.schema.json`.

The generated sources are checked in under `generated-src/main/java` so the
example can be built and tested directly:

```bash
./gradlew :examples:tagged-oneof:check --console=plain
```

The schema generates a sealed root interface with nested branch records. The
example covers tag-first reading and writing, branch validation, scalar facets,
homogeneous arrays, nullable fields, and deterministic tag diagnostics.
