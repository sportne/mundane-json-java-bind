# Formatting Rules

- Java source is formatted by Spotless using google-java-format.
- Gradle Groovy DSL files are formatted through the Spotless Groovy Gradle step.
- Markdown and structured text files covered by Spotless must use LF, trim
  trailing whitespace, and end with a newline.
- Generated source must also be formatted, deterministic, and readable.
