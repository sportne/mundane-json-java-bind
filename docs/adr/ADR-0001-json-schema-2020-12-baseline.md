# ADR-0001: JSON Schema Draft 2020-12 Baseline

## Status

Accepted.

## Decision

The project uses JSON Schema Draft 2020-12 Core and Validation as the
truth-driving specification baseline.

## Consequences

The implementation may define strict generated-binding profiles, but accepted
keyword behavior must trace to the official specification. Unsupported behavior
must fail deterministically with exact schema locations.
