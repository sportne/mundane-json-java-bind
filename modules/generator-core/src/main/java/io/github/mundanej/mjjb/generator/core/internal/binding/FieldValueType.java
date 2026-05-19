package io.github.mundanej.mjjb.generator.core.internal.binding;

import java.util.Objects;
import java.util.OptionalLong;

/** Java value shape selected for one generated object field. */
public record FieldValueType(
    JavaScalarType scalarType,
    boolean array,
    OptionalLong minItems,
    OptionalLong maxItems,
    FacetConstraints facets) {
  public FieldValueType {
    Objects.requireNonNull(scalarType, "scalarType");
    Objects.requireNonNull(minItems, "minItems");
    Objects.requireNonNull(maxItems, "maxItems");
    Objects.requireNonNull(facets, "facets");
    if (!array && (minItems.isPresent() || maxItems.isPresent())) {
      throw new IllegalArgumentException("scalar fields must not have array item bounds");
    }
  }

  public static FieldValueType scalar(JavaScalarType scalarType) {
    return scalar(scalarType, FacetConstraints.EMPTY);
  }

  public static FieldValueType scalar(JavaScalarType scalarType, FacetConstraints facets) {
    return new FieldValueType(
        scalarType, false, OptionalLong.empty(), OptionalLong.empty(), facets);
  }

  public static FieldValueType array(
      JavaScalarType scalarType, OptionalLong minItems, OptionalLong maxItems) {
    return array(scalarType, minItems, maxItems, FacetConstraints.EMPTY);
  }

  public static FieldValueType array(
      JavaScalarType scalarType,
      OptionalLong minItems,
      OptionalLong maxItems,
      FacetConstraints facets) {
    return new FieldValueType(scalarType, true, minItems, maxItems, facets);
  }

  public String requiredJavaType() {
    if (array) {
      return "List<" + scalarType.boxedJavaType() + ">";
    }
    return scalarType.requiredJavaType();
  }

  public String optionalJavaType() {
    if (array) {
      return "Optional<" + requiredJavaType() + ">";
    }
    return scalarType.optionalJavaType();
  }
}
