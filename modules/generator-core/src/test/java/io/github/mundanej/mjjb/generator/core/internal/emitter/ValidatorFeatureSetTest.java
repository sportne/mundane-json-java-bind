package io.github.mundanej.mjjb.generator.core.internal.emitter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjjb.generator.core.internal.binding.BindingBuildResult;
import io.github.mundanej.mjjb.generator.core.internal.binding.BindingModel;
import io.github.mundanej.mjjb.generator.core.internal.binding.BindingModelBuilder;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxParseResult;
import io.github.mundanej.mjjb.schema.model.SchemaSyntaxParser;
import org.junit.jupiter.api.Test;

final class ValidatorFeatureSetTest {
  @Test
  void detectsScalarArrayObjectMapPatternAndNumericLiteralFeatures() {
    ValidatorFeatureSet features =
        ValidatorFeatureSet.from(
            build(
                """
                {
                  "type": "object",
                  "minProperties": 1,
                  "maxProperties": 9,
                  "propertyNames": {"pattern": "^[A-Za-z0-9_-]+$"},
                  "properties": {
                    "id": {"type": "string", "minLength": 1, "maxLength": 8, "format": "uuid"},
                    "score": {
                      "type": "number",
                      "minimum": 0,
                      "maximum": 10,
                      "exclusiveMinimum": -1,
                      "exclusiveMaximum": 11,
                      "multipleOf": 0.25,
                      "enum": [1.5, 2.0],
                      "const": 1.5
                    },
                    "tags": {
                      "type": "array",
                      "items": {"type": "number"},
                      "minItems": 1,
                      "maxItems": 3,
                      "uniqueItems": true
                    }
                  },
                  "patternProperties": {
                    "^x-": {"type": "integer", "minimum": 1}
                  },
                  "required": ["id", "score", "tags"],
                  "additionalProperties": {"type": "string", "enum": ["ok"]}
                }
                """));

    assertTrue(features.numberField());
    assertTrue(features.arrayWithMinItems());
    assertTrue(features.arrayWithMaxItems());
    assertTrue(features.minPropertiesConstraint());
    assertTrue(features.maxPropertiesConstraint());
    assertTrue(features.minLengthFacet());
    assertTrue(features.maxLengthFacet());
    assertTrue(features.patternFacet());
    assertTrue(features.formatFacet());
    assertTrue(features.minimumFacet());
    assertTrue(features.maximumFacet());
    assertTrue(features.exclusiveMinimumFacet());
    assertTrue(features.exclusiveMaximumFacet());
    assertTrue(features.multipleOfFacet());
    assertTrue(features.uniqueItemsConstraint());
    assertTrue(features.enumConstraint());
    assertTrue(features.constConstraint());
    assertTrue(features.numericFacet());
    assertTrue(features.numberLiteralConstraint());
    assertTrue(features.numberUniqueItems());
    assertTrue(features.requiresBigDecimal());
  }

  @Test
  void detectsFeaturesInsideTaggedUnionBranches() {
    ValidatorFeatureSet features =
        ValidatorFeatureSet.from(
            build(
                """
                {
                  "oneOf": [
                    {
                      "type": "object",
                      "properties": {
                        "kind": {"type": "string", "const": "card"},
                        "amount": {"type": "number", "minimum": 0}
                      },
                      "required": ["kind", "amount"],
                      "additionalProperties": false
                    },
                    {
                      "type": "object",
                      "properties": {
                        "kind": {"type": "string", "const": "bank"},
                        "iban": {"type": "string", "minLength": 8}
                      },
                      "required": ["kind", "iban"],
                      "additionalProperties": false
                    }
                  ]
                }
                """));

    assertTrue(features.numberField());
    assertTrue(features.minimumFacet());
    assertTrue(features.minLengthFacet());
    assertTrue(features.requiresBigDecimal());
  }

  @Test
  void omitsUnusedFeaturesForClosedStringOnlyObject() {
    ValidatorFeatureSet features =
        ValidatorFeatureSet.from(
            build(
                """
                {
                  "type": "object",
                  "properties": {
                    "id": {"type": "string"}
                  },
                  "required": ["id"],
                  "additionalProperties": false
                }
                """));

    assertFalse(features.numberField());
    assertFalse(features.arrayWithMinItems());
    assertFalse(features.patternFacet());
    assertFalse(features.numericFacet());
    assertFalse(features.requiresBigDecimal());
  }

  private static BindingModel build(String schema) {
    SchemaSyntaxParseResult parseResult = SchemaSyntaxParser.parse(schema);
    assertTrue(parseResult.diagnostics().isEmpty());
    BindingBuildResult buildResult =
        new BindingModelBuilder()
            .build(parseResult.root(), "example.generated", "GeneratedBindings");
    assertTrue(buildResult.diagnostics().isEmpty());
    return buildResult.model().orElseThrow();
  }
}
