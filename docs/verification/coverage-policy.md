# Coverage Policy

All Java projects with compiled production classes use the same initial JaCoCo
verification thresholds.

| Aggregate line | Aggregate branch | Per-file line |
|---:|---:|---:|
| 60% | 40% | 40% |

Modules with no compiled production classes skip coverage verification.

The single threshold set is intentional. This project prefers simple,
predictable build rules over module-specific policy unless a later governance
task proves that extra complexity is necessary.
