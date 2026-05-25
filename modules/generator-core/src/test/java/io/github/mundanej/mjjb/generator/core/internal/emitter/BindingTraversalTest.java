package io.github.mundanej.mjjb.generator.core.internal.emitter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mundanej.mjjb.generator.core.internal.binding.BindingModel;
import io.github.mundanej.mjjb.generator.core.internal.binding.FieldBinding;
import io.github.mundanej.mjjb.generator.core.internal.binding.FieldValueType;
import io.github.mundanej.mjjb.generator.core.internal.binding.JavaScalarType;
import io.github.mundanej.mjjb.generator.core.internal.binding.MapBinding;
import io.github.mundanej.mjjb.generator.core.internal.binding.ObjectBinding;
import io.github.mundanej.mjjb.generator.core.internal.binding.ObjectValidationConstraints;
import io.github.mundanej.mjjb.generator.core.internal.binding.SchemaAnnotationsBinding;
import io.github.mundanej.mjjb.generator.core.internal.binding.TaggedUnionBinding;
import io.github.mundanej.mjjb.generator.core.internal.binding.TaggedUnionBranch;
import io.github.mundanej.mjjb.schema.model.JsonPointer;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class BindingTraversalTest {
  @Test
  void traversesFieldsMapsAndNestedObjectsInDeterministicOrder() {
    ObjectBinding address =
        object(
            "Address",
            List.of(field("street", FieldValueType.scalar(JavaScalarType.STRING))),
            Optional.of(map("addressExtras", "AddressExtra")));
    ObjectBinding rootPatternValue =
        object("RootPatternValue", List.of(field("patternName", scalar())));
    ObjectBinding rootAdditionalValue =
        object("RootAdditionalValue", List.of(field("extraName", scalar())));
    ObjectBinding root =
        object(
            "Root",
            List.of(field("profile", FieldValueType.object(address)), field("id", scalar())),
            Optional.of(patternMap("rootPattern", rootPatternValue, "^x-")),
            Optional.of(map("rootAdditional", rootAdditionalValue)));
    BindingModel model = new BindingModel("example.generated", "Root", root);

    assertEquals(
        List.of("profile", "id", "street", "value", "patternName", "extraName"), fields(model));
    assertEquals(List.of("rootPattern", "rootAdditional", "addressExtras"), maps(model));
    assertEquals(
        List.of("Address", "AddressExtra", "RootPatternValue", "RootAdditionalValue"),
        nestedObjects(model));
    assertEquals(
        List.of("Root", "Address", "AddressExtra", "RootPatternValue", "RootAdditionalValue"),
        allObjects(model));
  }

  @Test
  void taggedUnionTraversalUsesBranchesAsRoots() {
    ObjectBinding root = object("Root", List.of(field("ignored", scalar())));
    ObjectBinding cardDetail = object("CardDetail", List.of(field("last4", scalar())));
    ObjectBinding bankDetail = object("BankDetail", List.of(field("iban", scalar())));
    ObjectBinding card =
        object("Card", List.of(field("kind", scalar()), field("detail", cardDetail)));
    ObjectBinding bank =
        object("Bank", List.of(field("kind", scalar()), field("detail", bankDetail)));
    BindingModel model =
        new BindingModel(
            "example.generated",
            "Payment",
            root,
            Optional.of(
                new TaggedUnionBinding(
                    "kind",
                    JsonPointer.ROOT.property("oneOf"),
                    List.of(
                        new TaggedUnionBranch("card", card),
                        new TaggedUnionBranch("bank", bank)))));

    assertEquals(List.of("kind", "detail", "last4", "kind", "detail", "iban"), fields(model));
    assertEquals(List.of("CardDetail", "BankDetail"), nestedObjects(model));
    assertEquals(List.of("Card", "CardDetail", "Bank", "BankDetail"), allObjects(model));
  }

  private static List<String> fields(BindingModel model) {
    return BindingTraversal.allFields(model).stream().map(FieldBinding::javaFieldName).toList();
  }

  private static List<String> maps(BindingModel model) {
    return BindingTraversal.allMaps(model).stream().map(MapBinding::javaFieldName).toList();
  }

  private static List<String> nestedObjects(BindingModel model) {
    return BindingTraversal.nestedObjects(model).stream().map(ObjectBinding::javaTypeName).toList();
  }

  private static List<String> allObjects(BindingModel model) {
    return BindingTraversal.allObjects(model).stream().map(ObjectBinding::javaTypeName).toList();
  }

  private static ObjectBinding object(String typeName, List<FieldBinding> fields) {
    return object(typeName, fields, Optional.empty());
  }

  private static ObjectBinding object(
      String typeName, List<FieldBinding> fields, Optional<MapBinding> additionalProperties) {
    return object(typeName, fields, Optional.empty(), additionalProperties);
  }

  private static ObjectBinding object(
      String typeName,
      List<FieldBinding> fields,
      Optional<MapBinding> patternProperties,
      Optional<MapBinding> additionalProperties) {
    return new ObjectBinding(
        typeName,
        JsonPointer.ROOT.property(typeName),
        fields,
        patternProperties,
        additionalProperties,
        ObjectValidationConstraints.EMPTY,
        SchemaAnnotationsBinding.EMPTY);
  }

  private static FieldBinding field(String name, ObjectBinding object) {
    return field(name, FieldValueType.object(object));
  }

  private static FieldBinding field(String name, FieldValueType valueType) {
    return new FieldBinding(
        name, name, valueType, true, JsonPointer.ROOT.property("properties").property(name));
  }

  private static FieldValueType scalar() {
    return FieldValueType.scalar(JavaScalarType.STRING);
  }

  private static MapBinding map(String name, String valueTypeName) {
    return map(name, object(valueTypeName, List.of(field("value", scalar()))));
  }

  private static MapBinding map(String name, ObjectBinding object) {
    return MapBinding.additionalProperties(
        name, FieldValueType.object(object), JsonPointer.ROOT.property("additionalProperties"));
  }

  private static MapBinding patternMap(String name, ObjectBinding object, String pattern) {
    return MapBinding.patternProperties(
        name,
        FieldValueType.object(object),
        JsonPointer.ROOT.property("patternProperties").property(pattern),
        pattern);
  }
}
