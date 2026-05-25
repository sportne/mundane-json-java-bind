package io.github.mundanej.mjjb.generator.core.internal.emitter;

import io.github.mundanej.mjjb.generator.core.internal.binding.BindingModel;
import io.github.mundanej.mjjb.generator.core.internal.binding.FieldBinding;
import io.github.mundanej.mjjb.generator.core.internal.binding.MapBinding;
import io.github.mundanej.mjjb.generator.core.internal.binding.ObjectBinding;
import io.github.mundanej.mjjb.generator.core.internal.binding.TaggedUnionBranch;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class BindingTraversal {
  private BindingTraversal() {}

  static List<FieldBinding> allFields(BindingModel model) {
    Objects.requireNonNull(model, "model");
    ArrayList<FieldBinding> fields = new ArrayList<>();
    for (ObjectBinding object : traversalRoots(model)) {
      collectFields(object, fields);
    }
    return List.copyOf(fields);
  }

  static List<MapBinding> allMaps(BindingModel model) {
    Objects.requireNonNull(model, "model");
    ArrayList<MapBinding> maps = new ArrayList<>();
    for (ObjectBinding object : traversalRoots(model)) {
      collectMaps(object, maps);
    }
    return List.copyOf(maps);
  }

  static List<ObjectBinding> nestedObjects(BindingModel model) {
    Objects.requireNonNull(model, "model");
    ArrayList<ObjectBinding> objects = new ArrayList<>();
    for (ObjectBinding object : traversalRoots(model)) {
      collectNestedObjects(object, objects);
    }
    return List.copyOf(objects);
  }

  static List<ObjectBinding> allObjects(BindingModel model) {
    Objects.requireNonNull(model, "model");
    ArrayList<ObjectBinding> objects = new ArrayList<>();
    for (ObjectBinding object : traversalRoots(model)) {
      objects.add(object);
      collectNestedObjects(object, objects);
    }
    return List.copyOf(objects);
  }

  static List<MapBinding> objectMaps(ObjectBinding object) {
    Objects.requireNonNull(object, "object");
    ArrayList<MapBinding> maps = new ArrayList<>();
    object.patternProperties().ifPresent(maps::add);
    object.additionalProperties().ifPresent(maps::add);
    return List.copyOf(maps);
  }

  private static List<ObjectBinding> traversalRoots(BindingModel model) {
    if (model.taggedUnion().isEmpty()) {
      return List.of(model.rootObject());
    }
    return model.taggedUnion().orElseThrow().branches().stream()
        .map(TaggedUnionBranch::object)
        .toList();
  }

  private static void collectFields(ObjectBinding object, List<FieldBinding> fields) {
    fields.addAll(object.fields());
    for (FieldBinding field : object.fields()) {
      field.valueType().objectBinding().ifPresent(nested -> collectFields(nested, fields));
    }
    object
        .patternProperties()
        .flatMap(map -> map.valueType().objectBinding())
        .ifPresent(nested -> collectFields(nested, fields));
    object
        .additionalProperties()
        .flatMap(map -> map.valueType().objectBinding())
        .ifPresent(nested -> collectFields(nested, fields));
  }

  private static void collectMaps(ObjectBinding object, List<MapBinding> maps) {
    maps.addAll(objectMaps(object));
    for (FieldBinding field : object.fields()) {
      field.valueType().objectBinding().ifPresent(nested -> collectMaps(nested, maps));
    }
    object
        .patternProperties()
        .flatMap(map -> map.valueType().objectBinding())
        .ifPresent(nested -> collectMaps(nested, maps));
    object
        .additionalProperties()
        .flatMap(map -> map.valueType().objectBinding())
        .ifPresent(nested -> collectMaps(nested, maps));
  }

  private static void collectNestedObjects(ObjectBinding object, List<ObjectBinding> objects) {
    for (FieldBinding field : object.fields()) {
      field
          .valueType()
          .objectBinding()
          .ifPresent(
              nested -> {
                objects.add(nested);
                collectNestedObjects(nested, objects);
              });
    }
    object
        .patternProperties()
        .flatMap(map -> map.valueType().objectBinding())
        .ifPresent(
            nested -> {
              objects.add(nested);
              collectNestedObjects(nested, objects);
            });
    object
        .additionalProperties()
        .flatMap(map -> map.valueType().objectBinding())
        .ifPresent(
            nested -> {
              objects.add(nested);
              collectNestedObjects(nested, objects);
            });
  }
}
