# Release Verification

The release dry-run lane verifies local publication readiness without signing
or uploading to Maven Central. Public release artifacts are distributed only as
GitHub Release assets.

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

The GitHub Release workflow packages that repository as:

```text
mjjb-<version>-maven-repository.zip
mjjb-<version>-maven-repository.zip.sha256
```

To consume a GitHub-only release, download and unzip the repository asset, then
point Gradle or Maven at the unpacked `maven` directory.

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

## Release Version

The repository version is `1.0.0`. To test another version without editing
repository files, pass the Gradle property:

```bash
./gradlew releaseDryRun -Pmjjb.version=1.0.0 --console=plain
```

## GitHub Release Workflow

GitHub release publication is defined in
`../../.github/workflows/release.yml`. It runs on `v*` tags and can also be
started manually with a version input. The workflow:

1. runs `releaseDryRun` for the tag version;
2. zips `build/release-dry-run/maven`;
3. uploads the zip and SHA-256 file as workflow artifacts;
4. creates or updates the matching GitHub Release with those two assets.

## Non-Goals

- No signing keys are configured.
- No Maven Central or Sonatype upload is performed.
- No remote Maven publishing repository is configured.
- No generated dry-run output under `build/` is intended to be committed.
