# Validation Architecture

Generated validators implement accepted Draft 2020-12 validation keyword
semantics directly in Java source.

Validation failures are returned as values through `ValidationResult`.
Parse/read failures use `JsonReadException`. Normal validation flow must not be
exception-driven.

Each validation error carries a stable code, human message, JSON instance path,
optional JSON location, and schema location where available.
