# Charter

`mundane-json-java-bind` exists to do one thing: generate explicit Java binding
code from JSON Schema.

## Goals

- Consume JSON Schema Draft 2020-12 schemas.
- Generate Java 21 model, reader, writer, and validator source.
- Keep generated code readable and deterministic.
- Avoid runtime reflection and runtime discovery.
- Preserve GraalVM Native Image compatibility by design.
- Keep runtime dependencies at zero third-party libraries for the default path.

## Non-goals

- A Jackson replacement.
- A general-purpose object mapper.
- A runtime schema interpreter.
- A dependency injection, web, configuration, OpenAPI, MCP, protobuf, or plugin
  framework.
- Full JSON Schema compliance in the first profile.
