# ADR-0002: Generated Code Uses No Runtime Reflection

## Status

Accepted.

## Decision

Generated readers, writers, validators, and models must not use runtime
reflection, annotation scanning, dynamic proxies, runtime code generation,
ServiceLoader discovery, or classpath scanning.

## Consequences

The generator emits explicit Java source for all binding behavior. Native Image
reachability metadata should not be required for the default generated path.
