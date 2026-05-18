package io.github.mundanej.mjjb.generator.gradle;

import io.github.mundanej.mjjb.generator.api.GeneratorRequest;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

/** Gradle DSL extension for JSON Schema to Java generation. */
public class MjjbExtension {
  private final ConfigurableFileCollection schemas;
  private final DirectoryProperty outputDirectory;
  private final Property<String> profile;
  private final Property<String> defaultPackage;
  private final MapProperty<String, String> packageMappings;

  public MjjbExtension(ObjectFactory objects) {
    schemas = objects.fileCollection();
    outputDirectory = objects.directoryProperty();
    profile = objects.property(String.class).convention("JSP-DATA-2020-12");
    defaultPackage = objects.property(String.class).convention(GeneratorRequest.DEFAULT_PACKAGE);
    packageMappings = objects.mapProperty(String.class, String.class);
  }

  public void schema(Object path) {
    schemas.from(path);
  }

  public void packageMapping(String schemaId, String packageName) {
    packageMappings.put(schemaId, packageName);
  }

  public ConfigurableFileCollection getSchemas() {
    return schemas;
  }

  public DirectoryProperty getOutputDirectory() {
    return outputDirectory;
  }

  public Property<String> getProfile() {
    return profile;
  }

  public Property<String> getDefaultPackage() {
    return defaultPackage;
  }

  public MapProperty<String, String> getPackageMappings() {
    return packageMappings;
  }
}
