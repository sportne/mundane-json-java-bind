package io.github.mundanej.mjjb.generator.core.internal.binding;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/** Java value shape selected for one generated object field. */
public record FieldValueType(
    JavaScalarType scalarType,
    Optional<ObjectBinding> objectBinding,
    boolean array,
    boolean nullable,
    OptionalLong minItems,
    OptionalLong maxItems,
    boolean uniqueItems,
    FacetConstraints facets,
    LiteralConstraints literals) {
  public FieldValueType(
      JavaScalarType scalarType,
      Optional<ObjectBinding> objectBinding,
      boolean array,
      boolean nullable,
      OptionalLong minItems,
      OptionalLong maxItems,
      FacetConstraints facets,
      LiteralConstraints literals) {
    this(scalarType, objectBinding, array, nullable, minItems, maxItems, false, facets, literals);
  }

  public FieldValueType {
    Objects.requireNonNull(objectBinding, "objectBinding");
    Objects.requireNonNull(minItems, "minItems");
    Objects.requireNonNull(maxItems, "maxItems");
    Objects.requireNonNull(facets, "facets");
    Objects.requireNonNull(literals, "literals");
    if (scalarType == null && objectBinding.isEmpty()) {
      throw new IllegalArgumentException("scalarType or objectBinding must be present");
    }
    if (scalarType != null && objectBinding.isPresent()) {
      throw new IllegalArgumentException("scalarType and objectBinding must not both be present");
    }
    if (!array && (minItems.isPresent() || maxItems.isPresent())) {
      throw new IllegalArgumentException("scalar fields must not have array item bounds");
    }
    if (!array && uniqueItems) {
      throw new IllegalArgumentException("scalar fields must not have uniqueItems");
    }
    if (objectBinding.isPresent()
        && (array
            || nullable
            || minItems.isPresent()
            || maxItems.isPresent()
            || uniqueItems
            || !facets.equals(FacetConstraints.EMPTY)
            || !literals.equals(LiteralConstraints.EMPTY))) {
      throw new IllegalArgumentException("object fields must not have scalar constraints");
    }
  }

  public static FieldValueType scalar(JavaScalarType scalarType) {
    return scalar(scalarType, FacetConstraints.EMPTY);
  }

  public static FieldValueType scalar(JavaScalarType scalarType, FacetConstraints facets) {
    return scalar(scalarType, facets, LiteralConstraints.EMPTY);
  }

  public static FieldValueType scalar(
      JavaScalarType scalarType, FacetConstraints facets, LiteralConstraints literals) {
    Objects.requireNonNull(scalarType, "scalarType");
    return new FieldValueType(
        scalarType,
        Optional.empty(),
        false,
        false,
        OptionalLong.empty(),
        OptionalLong.empty(),
        false,
        facets,
        literals);
  }

  public static FieldValueType nullableScalar(
      JavaScalarType scalarType, FacetConstraints facets, LiteralConstraints literals) {
    Objects.requireNonNull(scalarType, "scalarType");
    return new FieldValueType(
        scalarType,
        Optional.empty(),
        false,
        true,
        OptionalLong.empty(),
        OptionalLong.empty(),
        false,
        facets,
        literals);
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
    return array(scalarType, minItems, maxItems, facets, LiteralConstraints.EMPTY);
  }

  public static FieldValueType array(
      JavaScalarType scalarType,
      OptionalLong minItems,
      OptionalLong maxItems,
      FacetConstraints facets,
      LiteralConstraints literals) {
    return array(scalarType, minItems, maxItems, false, facets, literals);
  }

  public static FieldValueType array(
      JavaScalarType scalarType,
      OptionalLong minItems,
      OptionalLong maxItems,
      boolean uniqueItems,
      FacetConstraints facets,
      LiteralConstraints literals) {
    Objects.requireNonNull(scalarType, "scalarType");
    return new FieldValueType(
        scalarType,
        Optional.empty(),
        true,
        false,
        minItems,
        maxItems,
        uniqueItems,
        facets,
        literals);
  }

  public static FieldValueType nullableArray(
      JavaScalarType scalarType,
      OptionalLong minItems,
      OptionalLong maxItems,
      FacetConstraints facets,
      LiteralConstraints literals) {
    return nullableArray(scalarType, minItems, maxItems, false, facets, literals);
  }

  public static FieldValueType nullableArray(
      JavaScalarType scalarType,
      OptionalLong minItems,
      OptionalLong maxItems,
      boolean uniqueItems,
      FacetConstraints facets,
      LiteralConstraints literals) {
    Objects.requireNonNull(scalarType, "scalarType");
    return new FieldValueType(
        scalarType,
        Optional.empty(),
        true,
        true,
        minItems,
        maxItems,
        uniqueItems,
        facets,
        literals);
  }

  public static FieldValueType object(ObjectBinding objectBinding) {
    Objects.requireNonNull(objectBinding, "objectBinding");
    return new FieldValueType(
        null,
        Optional.of(objectBinding),
        false,
        false,
        OptionalLong.empty(),
        OptionalLong.empty(),
        false,
        FacetConstraints.EMPTY,
        LiteralConstraints.EMPTY);
  }

  public String requiredJavaType() {
    if (objectBinding.isPresent()) {
      return objectBinding.orElseThrow().javaTypeName();
    }
    if (nullable) {
      return "JsonField<" + nullableValueJavaType() + ">";
    }
    if (array) {
      return "List<" + scalarType.boxedJavaType() + ">";
    }
    return scalarType.requiredJavaType();
  }

  public String optionalJavaType() {
    if (objectBinding.isPresent()) {
      return "Optional<" + requiredJavaType() + ">";
    }
    if (nullable) {
      return requiredJavaType();
    }
    if (array) {
      return "Optional<" + requiredJavaType() + ">";
    }
    return scalarType.optionalJavaType();
  }

  public String nullableValueJavaType() {
    if (objectBinding.isPresent()) {
      return objectBinding.orElseThrow().javaTypeName();
    }
    if (array) {
      return "List<" + scalarType.boxedJavaType() + ">";
    }
    return scalarType.boxedJavaType();
  }
}
