package io.github.mundanej.mjjb.generator.core.internal.binding;

import io.github.mundanej.mjjb.schema.model.JsonPointer;
import java.util.Objects;

/** Diagnostic raised while building binding IR. */
public record BindingDiagnostic(String code, String message, JsonPointer pointer) {
  public static final String ROOT_TYPE_CODE = "MJJBG-BINDING-ROOT-TYPE";
  public static final String ADDITIONAL_PROPERTIES_CODE = "MJJBG-BINDING-ADDITIONAL-PROPERTIES";
  public static final String MISSING_PROPERTY_TYPE_CODE = "MJJBG-BINDING-MISSING-PROPERTY-TYPE";
  public static final String UNSUPPORTED_PROPERTY_TYPE_CODE =
      "MJJBG-BINDING-UNSUPPORTED-PROPERTY-TYPE";
  public static final String MISSING_ARRAY_ITEMS_CODE = "MJJBG-BINDING-MISSING-ARRAY-ITEMS";
  public static final String MISSING_ARRAY_ITEM_TYPE_CODE = "MJJBG-BINDING-MISSING-ARRAY-ITEM-TYPE";
  public static final String INVALID_ARRAY_BOUNDS_CODE = "MJJBG-BINDING-INVALID-ARRAY-BOUNDS";
  public static final String UNSUPPORTED_LITERAL_CONSTRAINT_CODE =
      "MJJBG-BINDING-UNSUPPORTED-LITERAL-CONSTRAINT";
  public static final String INVALID_LITERAL_CONSTRAINT_CODE =
      "MJJBG-BINDING-INVALID-LITERAL-CONSTRAINT";
  public static final String UNSUPPORTED_ONE_OF_CODE = "MJJBG-BINDING-UNSUPPORTED-ONEOF";
  public static final String UNKNOWN_REQUIRED_CODE = "MJJBG-BINDING-UNKNOWN-REQUIRED";
  public static final String NAME_COLLISION_CODE = "MJJBG-BINDING-NAME-COLLISION";

  public BindingDiagnostic {
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(message, "message");
    Objects.requireNonNull(pointer, "pointer");
    if (code.isBlank()) {
      throw new IllegalArgumentException("code must not be blank");
    }
    if (message.isBlank()) {
      throw new IllegalArgumentException("message must not be blank");
    }
  }
}
