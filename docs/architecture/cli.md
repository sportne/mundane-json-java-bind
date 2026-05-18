# CLI Architecture

The generator CLI is the user-facing entry point for producing Java source from
supported JSON Schema inputs.

```bash
mjjb generate --schema <schema.json> --out <dir> [--package <package>] [--root-type <TypeName>] [--profile JSP-DATA-2020-12]
```

`--schema` may be provided more than once. The current generator emits the first
supported binding until multi-schema generation is deliberately widened. `--out`
selects the source root. `--package` defaults to
`io.github.mundanej.mjjb.generated`. `--root-type` defaults to
`GeneratedBindings` and accepts a simple Java top-level type identifier, not a
qualified name. `--profile` currently accepts `JSP-DATA-2020-12`.

The CLI validates Java package and root type names before generation. Package
segments and the root type must be valid Java identifiers and must not be Java
keywords.

Exit codes are stable:

- `0`: generation succeeded.
- `1`: generation failed after argument parsing, such as missing schema files,
  invalid JSON, unsupported schema keywords, or unsupported binding shapes.
- `2`: CLI argument parsing or validation failed.

Successful generation writes one `Generated <path>` line per emitted source on
standard output. Diagnostics are written to standard error. Generator diagnostics
use the shared manifest line format so CLI output remains deterministic.

For the basic object binding milestone, a successful run emits four sources
under the requested package path:

- `<RootType>.java`
- `<RootType>JsonWriter.java`
- `<RootType>JsonReader.java`
- `<RootType>JsonValidator.java`

Generated sources depend on runtime APIs only. The CLI itself depends on the
generator API and core implementation and does not change Gradle plugin
behavior.
