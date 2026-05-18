# Implementation Task Template

```text
Task ID:
Status:
Gate:
Depends on:
Specification references:
Target modules:
Allowed files:
Forbidden files:
Expected behavior:
Tests to add/update:
Documentation to update:
Commands to run:
Acceptance criteria:
Rollback notes:
```

## Rules

- Status must be `draft`, `ready-for-implementation`, `in-progress`,
  `complete`, `blocked`, or `human-gate-blocked`.
- Allowed files must be specific enough to avoid unrelated refactors.
- Forbidden files must call out product areas that should not change in a task.
- Commands must be runnable locally.
- Acceptance criteria must be objective.
- Runtime, generated-code, parser, CLI, Gradle plugin, Native Image, and
  architecture changes must include test and documentation impact.
