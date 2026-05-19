package io.github.mundanej.mjjb.generator.core.internal.binding;

/** Scalar Java type selected for the first object binding slice. */
public enum JavaScalarType {
  STRING("string", "String", "String", "Optional<String>"),
  INTEGER("integer", "long", "Long", "Optional<Long>"),
  NUMBER("number", "double", "Double", "Optional<Double>"),
  BOOLEAN("boolean", "boolean", "Boolean", "Optional<Boolean>");

  private final String schemaType;
  private final String requiredJavaType;
  private final String boxedJavaType;
  private final String optionalJavaType;

  JavaScalarType(
      String schemaType, String requiredJavaType, String boxedJavaType, String optionalJavaType) {
    this.schemaType = schemaType;
    this.requiredJavaType = requiredJavaType;
    this.boxedJavaType = boxedJavaType;
    this.optionalJavaType = optionalJavaType;
  }

  public String schemaType() {
    return schemaType;
  }

  public String requiredJavaType() {
    return requiredJavaType;
  }

  public String boxedJavaType() {
    return boxedJavaType;
  }

  public String optionalJavaType() {
    return optionalJavaType;
  }
}
