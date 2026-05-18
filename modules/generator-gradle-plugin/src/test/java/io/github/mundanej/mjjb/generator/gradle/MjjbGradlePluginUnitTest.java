package io.github.mundanej.mjjb.generator.gradle;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class MjjbGradlePluginUnitTest {
  @TempDir Path tempDir;

  @Test
  void registersExtensionAndGenerateTask() {
    Project project = ProjectBuilder.builder().build();

    project.getPluginManager().apply(MjjbGradlePlugin.class);

    assertNotNull(project.getExtensions().findByType(MjjbExtension.class));
    assertNotNull(project.getTasks().findByName("generateMjjb"));
  }

  @Test
  void generateTaskWritesGeneratedSources() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Path output = tempDir.resolve("generated");
    Files.writeString(
        schema, "{\"type\":\"object\",\"properties\":{},\"additionalProperties\":false}");
    Project project = ProjectBuilder.builder().build();
    project.getPluginManager().apply(MjjbGradlePlugin.class);
    MjjbExtension extension = project.getExtensions().getByType(MjjbExtension.class);
    extension.schema(schema);
    extension.getOutputDirectory().set(output.toFile());

    MjjbGenerateTask task = (MjjbGenerateTask) project.getTasks().getByName("generateMjjb");
    task.generate();

    assertTrue(
        Files.isRegularFile(
            output.resolve("io/github/mundanej/mjjb/generated/GeneratedBindings.java")));
  }

  @Test
  void generateTaskRejectsInvalidProfile() throws IOException {
    Path schema = tempDir.resolve("schema.json");
    Files.writeString(schema, "{\"type\":\"object\"}");
    Project project = ProjectBuilder.builder().build();
    project.getPluginManager().apply(MjjbGradlePlugin.class);
    MjjbExtension extension = project.getExtensions().getByType(MjjbExtension.class);
    extension.schema(schema);
    extension.getOutputDirectory().set(tempDir.resolve("out").toFile());
    extension.getProfile().set("NOPE");

    MjjbGenerateTask task = (MjjbGenerateTask) project.getTasks().getByName("generateMjjb");

    assertThrows(GradleException.class, task::generate);
  }
}
