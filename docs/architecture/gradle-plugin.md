# Gradle Plugin Architecture

The Gradle plugin id is `io.github.mundanej.mjjb`. It registers a cacheable
`generateMjjb` task and an `mjjb` extension for JSON Schema to Java source
generation.

```groovy
plugins {
  id 'java'
  id 'io.github.mundanej.mjjb'
}

mjjb {
  schema('src/main/schema/binding.schema.json')
  outputDirectory.set(layout.buildDirectory.dir('generated/sources/mjjb/main/java'))
  defaultPackage.set('com.example.generated')
  rootTypeName.set('ExampleBinding')
  profile.set('JSP-DATA-2020-12')
}
```

The extension exposes schema files, output directory, profile token, default
package, root type name, and package mappings. `rootTypeName` defaults to
`GeneratedBindings` and accepts a simple Java top-level type identifier.

`generateMjjb` declares schema files as relative-path-sensitive inputs, package
settings as scalar inputs, package mappings as an input map, and the generated
source root as its output directory. Generation writes to a temporary task
directory first, then replaces the final output directory only after generation
succeeds.

When the consuming project applies the Java plugin, the generated source
directory is added to the `main` Java source set and `compileJava` depends on
`generateMjjb`. The MJJB plugin does not apply Java automatically and does not
add runtime dependencies; consuming projects must provide `runtime-core` on the
compile classpath.

Gradle-side diagnostics are deterministic:

- `MJJB-GRADLE-001`: unsupported profile token.
- `MJJB-GRADLE-002`: invalid default package name.
- `MJJB-GRADLE-003`: invalid root type name.

Generator diagnostics are surfaced through the shared manifest-line format.
