package io.github.mundanej.mjjb.generator.core.internal.emitter;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjjb.generator.core.internal.binding.BindingModel;
import io.github.mundanej.mjjb.generator.core.internal.binding.FacetConstraints;
import io.github.mundanej.mjjb.generator.core.internal.binding.FieldBinding;
import io.github.mundanej.mjjb.generator.core.internal.binding.FieldValueType;
import io.github.mundanej.mjjb.generator.core.internal.binding.JavaScalarType;
import io.github.mundanej.mjjb.generator.core.internal.binding.LiteralConstraints;
import io.github.mundanej.mjjb.generator.core.internal.binding.ObjectBinding;
import io.github.mundanej.mjjb.generator.core.internal.binding.TaggedUnionBinding;
import io.github.mundanej.mjjb.generator.core.internal.binding.TaggedUnionBranch;
import io.github.mundanej.mjjb.schema.model.JsonPointer;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

final class ModelSourceEmitterTest {
  @Test
  void emitsMinimalSourceForEmptyObject() {
    String source = emit(model("Empty", List.of()));

    assertContains(source, "package example.generated;");
    assertContains(source, "public record Empty() {}");
  }

  @Test
  void emitsRequiredOptionalAndNullableFieldSnippets() {
    String source =
        emit(
            model(
                "Sample",
                List.of(
                    field("name", FieldValueType.scalar(JavaScalarType.STRING), true),
                    field("count", FieldValueType.scalar(JavaScalarType.INTEGER), false),
                    field(
                        "nickname",
                        FieldValueType.nullableScalar(
                            JavaScalarType.STRING,
                            FacetConstraints.EMPTY,
                            LiteralConstraints.EMPTY),
                        true))));

    assertContains(source, "import io.github.mundanej.mjjb.runtime.JsonField;");
    assertContains(source, "import java.util.Objects;");
    assertContains(source, "import java.util.Optional;");
    assertContains(source, "String name,");
    assertContains(source, "Optional<Long> count,");
    assertContains(source, "JsonField<String> nickname) {");
    assertContains(source, "name = Objects.requireNonNull(name, \"name\");");
    assertContains(source, "count = Objects.requireNonNull(count, \"count\");");
    assertContains(source, "nickname = Objects.requireNonNull(nickname, \"nickname\");");
  }

  @Test
  void emitsArrayDefensiveCopyAndNullChecks() {
    String source =
        emit(
            model(
                "Arrays",
                List.of(
                    field(
                        "tags",
                        FieldValueType.array(
                            JavaScalarType.STRING, OptionalLong.empty(), OptionalLong.empty()),
                        true),
                    field(
                        "scores",
                        FieldValueType.array(
                            JavaScalarType.NUMBER, OptionalLong.empty(), OptionalLong.empty()),
                        false),
                    field(
                        "aliases",
                        FieldValueType.nullableArray(
                            JavaScalarType.STRING,
                            OptionalLong.empty(),
                            OptionalLong.empty(),
                            FacetConstraints.EMPTY,
                            LiteralConstraints.EMPTY),
                        true))));

    assertContains(source, "import io.github.mundanej.mjjb.runtime.JsonField;");
    assertContains(source, "import java.util.List;");
    assertContains(source, "import java.util.Objects;");
    assertContains(source, "import java.util.Optional;");
    assertContains(source, "List<String> tags,");
    assertContains(source, "Optional<List<Double>> scores,");
    assertContains(source, "JsonField<List<String>> aliases) {");
    assertContains(source, "tags = List.copyOf(Objects.requireNonNull(tags, \"tags\"));");
    assertContains(
        source, "scores = Objects.requireNonNull(scores, \"scores\").map(List::copyOf);");
    assertContains(source, "aliases = Objects.requireNonNull(aliases, \"aliases\");");
    assertContains(source, "if (aliases.hasValue()) {");
    assertContains(source, "aliases = JsonField.value(List.copyOf(aliases.requireValue()));");
  }

  @Test
  void emitsTaggedOneOfSealedInterfaceAndBranchRecords() {
    ObjectBinding person =
        object(
            "Person", List.of(field("kind", FieldValueType.scalar(JavaScalarType.STRING), true)));
    ObjectBinding organization =
        object(
            "Organization",
            List.of(field("kind", FieldValueType.scalar(JavaScalarType.STRING), true)));
    BindingModel model =
        new BindingModel(
            "example.generated",
            "Party",
            object("Party", List.of()),
            Optional.of(
                new TaggedUnionBinding(
                    "kind",
                    JsonPointer.ROOT.property("oneOf"),
                    List.of(
                        new TaggedUnionBranch("person", person),
                        new TaggedUnionBranch("organization", organization)))));

    String source = emit(model);

    assertContains(source, "public sealed interface Party permits");
    assertContains(source, "Party.Person,");
    assertContains(source, "Party.Organization {");
    assertContains(source, "record Person(");
    assertContains(source, "String kind) implements Party {");
    assertContains(source, "record Organization(");
    assertContains(source, "String kind) implements Party {");
  }

  private static String emit(BindingModel model) {
    return new ModelSourceEmitter().emit(model);
  }

  private static BindingModel model(String typeName, List<FieldBinding> fields) {
    return new BindingModel("example.generated", typeName, object(typeName, fields));
  }

  private static ObjectBinding object(String typeName, List<FieldBinding> fields) {
    return new ObjectBinding(typeName, JsonPointer.ROOT, fields);
  }

  private static FieldBinding field(String name, FieldValueType valueType, boolean required) {
    return new FieldBinding(
        name, name, valueType, required, JsonPointer.ROOT.property("properties").property(name));
  }

  private static void assertContains(String source, String expected) {
    assertTrue(
        source.contains(expected),
        () -> "Expected source to contain:\n" + expected + "\n\n" + source);
  }
}
