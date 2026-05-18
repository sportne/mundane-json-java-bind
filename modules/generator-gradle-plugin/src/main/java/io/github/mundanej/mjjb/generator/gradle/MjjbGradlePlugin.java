package io.github.mundanej.mjjb.generator.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/** Gradle plugin wiring for JSON Schema to Java generation. */
public final class MjjbGradlePlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {
    MjjbExtension extension =
        project.getExtensions().create("mjjb", MjjbExtension.class, project.getObjects());
    extension
        .getOutputDirectory()
        .convention(
            project.getLayout().getBuildDirectory().dir("generated/sources/mjjb/main/java"));
    project
        .getTasks()
        .register(
            "generateMjjb",
            MjjbGenerateTask.class,
            task -> {
              task.getSchemas().from(extension.getSchemas());
              task.getOutputDirectory().set(extension.getOutputDirectory());
              task.getProfile().set(extension.getProfile());
              task.getDefaultPackage().set(extension.getDefaultPackage());
              task.getPackageMappings().set(extension.getPackageMappings());
            });
  }
}
