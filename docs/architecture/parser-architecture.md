# Parser Architecture

The v1 parser is custom and dependency-free.

`parser-core` provides a streaming tokenizer/parser and writer backed by
project-owned runtime interfaces. It tracks byte/character location data for
diagnostics and does not build a generic object mapper graph as the normal
generated-binding path.

Generated readers call explicit token methods, switch on property names, and
construct generated model records directly.
