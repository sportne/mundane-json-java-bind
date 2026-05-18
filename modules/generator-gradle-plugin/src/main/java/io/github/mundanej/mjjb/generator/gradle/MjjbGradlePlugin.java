package io.github.mundanej.mjjb.generator.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;

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
    TaskProvider<MjjbGenerateTask> generateTask =
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
                  task.getRootTypeName().set(extension.getRootTypeName());
                  task.getPackageMappings().set(extension.getPackageMappings());
                });
    project
        .getPlugins()
        .withId(
            "java",
            plugin -> {
              JavaPluginExtension javaExtension =
                  project.getExtensions().getByType(JavaPluginExtension.class);
              javaExtension
                  .getSourceSets()
                  .named(
                      SourceSet.MAIN_SOURCE_SET_NAME,
                      sourceSet -> sourceSet.getJava().srcDir(extension.getOutputDirectory()));
              project
                  .getTasks()
                  .named(JavaPlugin.COMPILE_JAVA_TASK_NAME)
                  .configure(task -> task.dependsOn(generateTask));
            });
  }
}
