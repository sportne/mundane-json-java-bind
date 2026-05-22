# Release Verification

The release dry-run lane verifies local publication readiness without signing,
uploading to Maven Central, or publishing to any remote repository.

## Commands

Run the default JVM gate first:

```bash
./gradlew qualityGate --console=plain
```

Print the planned artifact coordinates:

```bash
./gradlew printPublishedArtifacts --console=plain
```

Run the local release dry-run:

```bash
./gradlew releaseDryRun --console=plain
```

The dry-run publishes public artifacts to:

```text
build/release-dry-run/maven
```

## Published Artifacts

| Project | Coordinate |
|---|---|
| `:modules:mjjb-bom` | `io.github.mundanej:mjjb-bom` |
| `:modules:runtime-core` | `io.github.mundanej:mjjb-runtime-core` |
| `:modules:parser-core` | `io.github.mundanej:mjjb-parser-core` |
| `:modules:schema-model` | `io.github.mundanej:mjjb-schema-model` |
| `:modules:generator-api` | `io.github.mundanej:mjjb-generator-api` |
| `:modules:generator-core` | `io.github.mundanej:mjjb-generator-core` |
| `:modules:generator-cli` | `io.github.mundanej:mjjb-cli` |
| `:modules:generator-gradle-plugin` | `io.github.mundanej:mjjb-gradle-plugin` |
| `:modules:testing-support` | `io.github.mundanej:mjjb-testing-support` |

`releaseDryRun` verifies that each published POM has project description,
BSD 3-Clause license metadata, developer metadata, and SCM metadata. It also
checks that Java artifacts include binary, sources, and javadoc jars, and that
the BOM aligns every public non-BOM module.

## Version Override

The default local version is `0.1.0-SNAPSHOT`. To test another version without
editing repository files, pass the Gradle property:

```bash
./gradlew releaseDryRun -Pmjjb.version=0.1.0 --console=plain
```

## Non-Goals

- No signing keys are configured.
- No Maven Central or Sonatype upload is performed.
- No remote publishing repository is configured.
- No generated dry-run output under `build/` is intended to be committed.
