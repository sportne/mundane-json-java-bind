# TASK-0004: Schema Syntax Model and Parser

Task ID: `TASK-0004`
Status: `complete`
Gate: Schema frontend
Depends on: `TASK-0002`
Specification references: JSON Schema Draft 2020-12 Core sections on schema documents, vocabularies, and JSON Pointers
Target modules: `schema-model`, `generator-core`

Allowed files:
- `modules/schema-model/src/main/java/io/github/mundanej/mjjb/schema/model/**`
- `modules/schema-model/src/test/java/io/github/mundanej/mjjb/schema/model/**`
- `modules/generator-core/src/main/java/io/github/mundanej/mjjb/generator/core/**`
- `modules/generator-core/src/test/java/io/github/mundanej/mjjb/generator/core/**`

Forbidden files:
- Generated Java emitters beyond consuming the schema model.

Expected behavior:
- Schema documents are parsed into a syntax model with exact JSON Pointer locations.
- The schema parser distinguishes object property names from string values.
- Invalid JSON Schema input produces deterministic diagnostics.

Tests to add/update:
- Syntax model tests for objects, arrays, escaped property names, nested locations, and invalid JSON.
- Generator tests proving string values that mention unsupported keyword names do not trigger unsupported diagnostics.

Documentation to update:
- Standards baseline and architecture docs if parser boundaries change.

Commands to run:
- `./gradlew :modules:schema-model:check :modules:generator-core:check --console=plain`
- `./gradlew qualityGate --console=plain`

Acceptance criteria:
- All schema diagnostics include exact schema JSON Pointer values.
- Raw text keyword scanning is fully removed from generator behavior.

Rollback notes:
- Restore prior generator validation behavior and remove schema syntax model changes.
