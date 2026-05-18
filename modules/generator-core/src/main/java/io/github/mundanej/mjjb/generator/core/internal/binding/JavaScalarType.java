package io.github.mundanej.mjjb.generator.core.internal.binding;

/** Scalar Java type selected for the first object binding slice. */
public enum JavaScalarType {
  STRING("string", "String", "Optional<String>"),
  INTEGER("integer", "long", "Optional<Long>"),
  NUMBER("number", "double", "Optional<Double>"),
  BOOLEAN("boolean", "boolean", "Optional<Boolean>");

  private final String schemaType;
  private final String requiredJavaType;
  private final String optionalJavaType;

  JavaScalarType(String schemaType, String requiredJavaType, String optionalJavaType) {
    this.schemaType = schemaType;
    this.requiredJavaType = requiredJavaType;
    this.optionalJavaType = optionalJavaType;
  }

  public String schemaType() {
    return schemaType;
  }

  public String requiredJavaType() {
    return requiredJavaType;
  }

  public String optionalJavaType() {
    return optionalJavaType;
  }
}
