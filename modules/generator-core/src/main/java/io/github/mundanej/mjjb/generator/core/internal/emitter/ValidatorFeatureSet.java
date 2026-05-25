package io.github.mundanej.mjjb.generator.core.internal.emitter;

import static io.github.mundanej.mjjb.generator.core.internal.emitter.BindingTraversal.allFields;
import static io.github.mundanej.mjjb.generator.core.internal.emitter.BindingTraversal.allMaps;
import static io.github.mundanej.mjjb.generator.core.internal.emitter.BindingTraversal.allObjects;

import io.github.mundanej.mjjb.generator.core.internal.binding.BindingModel;
import io.github.mundanej.mjjb.generator.core.internal.binding.JavaScalarType;
import java.util.Objects;

record ValidatorFeatureSet(
    boolean numberField,
    boolean arrayWithMinItems,
    boolean arrayWithMaxItems,
    boolean minPropertiesConstraint,
    boolean maxPropertiesConstraint,
    boolean minLengthFacet,
    boolean maxLengthFacet,
    boolean patternFacet,
    boolean formatFacet,
    boolean minimumFacet,
    boolean maximumFacet,
    boolean exclusiveMinimumFacet,
    boolean exclusiveMaximumFacet,
    boolean multipleOfFacet,
    boolean uniqueItemsConstraint,
    boolean enumConstraint,
    boolean constConstraint,
    boolean numericFacet,
    boolean numberLiteralConstraint,
    boolean numberUniqueItems) {
  static ValidatorFeatureSet from(BindingModel model) {
    Objects.requireNonNull(model, "model");
    return new ValidatorFeatureSet(
        numberField(model),
        arrayWithMinItems(model),
        arrayWithMaxItems(model),
        minPropertiesConstraint(model),
        maxPropertiesConstraint(model),
        minLengthFacet(model),
        maxLengthFacet(model),
        patternFacet(model),
        formatFacet(model),
        minimumFacet(model),
        maximumFacet(model),
        exclusiveMinimumFacet(model),
        exclusiveMaximumFacet(model),
        multipleOfFacet(model),
        uniqueItemsConstraint(model),
        enumConstraint(model),
        constConstraint(model),
        numericFacet(model),
        numberLiteralConstraint(model),
        numberUniqueItems(model));
  }

  boolean requiresBigDecimal() {
    return numericFacet || numberLiteralConstraint || numberUniqueItems;
  }

  private static boolean numberField(BindingModel model) {
    return allFields(model).stream().anyMatch(field -> field.scalarType() == JavaScalarType.NUMBER)
        || allMaps(model).stream()
            .anyMatch(map -> !map.object() && map.scalarType() == JavaScalarType.NUMBER);
  }

  private static boolean arrayWithMinItems(BindingModel model) {
    return allFields(model).stream()
            .anyMatch(field -> field.array() && field.valueType().minItems().isPresent())
        || allMaps(model).stream()
            .anyMatch(map -> map.array() && map.valueType().minItems().isPresent());
  }

  private static boolean arrayWithMaxItems(BindingModel model) {
    return allFields(model).stream()
            .anyMatch(field -> field.array() && field.valueType().maxItems().isPresent())
        || allMaps(model).stream()
            .anyMatch(map -> map.array() && map.valueType().maxItems().isPresent());
  }

  private static boolean uniqueItemsConstraint(BindingModel model) {
    return allFields(model).stream()
            .anyMatch(field -> field.array() && field.valueType().uniqueItems())
        || allMaps(model).stream().anyMatch(map -> map.array() && map.valueType().uniqueItems());
  }

  private static boolean minPropertiesConstraint(BindingModel model) {
    return allObjects(model).stream()
        .anyMatch(object -> object.validationConstraints().minProperties().isPresent());
  }

  private static boolean maxPropertiesConstraint(BindingModel model) {
    return allObjects(model).stream()
        .anyMatch(object -> object.validationConstraints().maxProperties().isPresent());
  }

  private static boolean minLengthFacet(BindingModel model) {
    return allFields(model).stream()
            .anyMatch(field -> field.valueType().facets().minLength().isPresent())
        || allMaps(model).stream().anyMatch(map -> map.valueType().facets().minLength().isPresent())
        || allObjects(model).stream()
            .anyMatch(
                object ->
                    object
                        .validationConstraints()
                        .propertyNames()
                        .map(facets -> facets.minLength().isPresent())
                        .orElse(false));
  }

  private static boolean maxLengthFacet(BindingModel model) {
    return allFields(model).stream()
            .anyMatch(field -> field.valueType().facets().maxLength().isPresent())
        || allMaps(model).stream().anyMatch(map -> map.valueType().facets().maxLength().isPresent())
        || allObjects(model).stream()
            .anyMatch(
                object ->
                    object
                        .validationConstraints()
                        .propertyNames()
                        .map(facets -> facets.maxLength().isPresent())
                        .orElse(false));
  }

  private static boolean patternFacet(BindingModel model) {
    return allFields(model).stream()
            .anyMatch(field -> field.valueType().facets().pattern().isPresent())
        || allMaps(model).stream().anyMatch(map -> map.valueType().facets().pattern().isPresent())
        || allObjects(model).stream()
            .anyMatch(
                object ->
                    object
                        .validationConstraints()
                        .propertyNames()
                        .map(facets -> facets.pattern().isPresent())
                        .orElse(false));
  }

  private static boolean formatFacet(BindingModel model) {
    return allFields(model).stream()
            .anyMatch(field -> field.valueType().facets().format().isPresent())
        || allMaps(model).stream().anyMatch(map -> map.valueType().facets().format().isPresent())
        || allObjects(model).stream()
            .anyMatch(
                object ->
                    object
                        .validationConstraints()
                        .propertyNames()
                        .map(facets -> facets.format().isPresent())
                        .orElse(false));
  }

  private static boolean minimumFacet(BindingModel model) {
    return allFields(model).stream()
            .anyMatch(field -> field.valueType().facets().minimum().isPresent())
        || allMaps(model).stream().anyMatch(map -> map.valueType().facets().minimum().isPresent());
  }

  private static boolean maximumFacet(BindingModel model) {
    return allFields(model).stream()
            .anyMatch(field -> field.valueType().facets().maximum().isPresent())
        || allMaps(model).stream().anyMatch(map -> map.valueType().facets().maximum().isPresent());
  }

  private static boolean exclusiveMinimumFacet(BindingModel model) {
    return allFields(model).stream()
            .anyMatch(field -> field.valueType().facets().exclusiveMinimum().isPresent())
        || allMaps(model).stream()
            .anyMatch(map -> map.valueType().facets().exclusiveMinimum().isPresent());
  }

  private static boolean exclusiveMaximumFacet(BindingModel model) {
    return allFields(model).stream()
            .anyMatch(field -> field.valueType().facets().exclusiveMaximum().isPresent())
        || allMaps(model).stream()
            .anyMatch(map -> map.valueType().facets().exclusiveMaximum().isPresent());
  }

  private static boolean multipleOfFacet(BindingModel model) {
    return allFields(model).stream()
            .anyMatch(field -> field.valueType().facets().multipleOf().isPresent())
        || allMaps(model).stream()
            .anyMatch(map -> map.valueType().facets().multipleOf().isPresent());
  }

  private static boolean numericFacet(BindingModel model) {
    return allFields(model).stream()
            .anyMatch(field -> field.valueType().facets().hasNumericFacets())
        || allMaps(model).stream().anyMatch(map -> map.valueType().facets().hasNumericFacets());
  }

  private static boolean enumConstraint(BindingModel model) {
    return allFields(model).stream().anyMatch(field -> field.valueType().literals().hasEnum())
        || allMaps(model).stream().anyMatch(map -> map.valueType().literals().hasEnum());
  }

  private static boolean constConstraint(BindingModel model) {
    return allFields(model).stream().anyMatch(field -> field.valueType().literals().hasConst())
        || allMaps(model).stream().anyMatch(map -> map.valueType().literals().hasConst());
  }

  private static boolean numberLiteralConstraint(BindingModel model) {
    return allFields(model).stream()
            .anyMatch(
                field ->
                    field.scalarType() == JavaScalarType.NUMBER
                        && (field.valueType().literals().hasEnum()
                            || field.valueType().literals().hasConst()))
        || allMaps(model).stream()
            .anyMatch(
                map ->
                    !map.object()
                        && map.scalarType() == JavaScalarType.NUMBER
                        && (map.valueType().literals().hasEnum()
                            || map.valueType().literals().hasConst()));
  }

  private static boolean numberUniqueItems(BindingModel model) {
    return allFields(model).stream()
            .anyMatch(
                field ->
                    field.array()
                        && field.valueType().uniqueItems()
                        && field.scalarType() == JavaScalarType.NUMBER)
        || allMaps(model).stream()
            .anyMatch(
                map ->
                    map.array()
                        && map.valueType().uniqueItems()
                        && map.scalarType() == JavaScalarType.NUMBER);
  }
}
