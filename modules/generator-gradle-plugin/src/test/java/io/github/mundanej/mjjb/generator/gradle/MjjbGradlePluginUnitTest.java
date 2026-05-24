package io.github.mundanej.mjjb.generator.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjjb.generator.api.GeneratorRequest;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceSet;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class MjjbGradlePluginUnitTest {
  @TempDir Path tempDir;

  @Test
  void registersExtensionAndGenerateTaskWithDefaults() throws IOException {
    Project project = newProject();

    project.getPluginManager().apply(MjjbGradlePlugin.class);

    MjjbExtension extension = project.getExtensions().findByType(MjjbExtension.class);
    assertNotNull(extension);
    assertEquals(GeneratorRequest.DEFAULT_ROOT_TYPE_NAME, extension.getRootTypeName().get());
    MjjbGenerateTask task = (MjjbGenerateTask) project.getTasks().findByName("generateMjjb");
    assertNotNull(task);
    assertEquals(GeneratorRequest.DEFAULT_ROOT_TYPE_NAME, task.getRootTypeName().get());
  }

  @Test
  void generatedSourceDirectoryIsWiredIntoJavaCompilation() throws IOException {
    Project project = newProject();
    project.getPluginManager().apply(MjjbGradlePlugin.class);
    project.getPluginManager().apply(JavaPlugin.class);

    MjjbExtension extension = project.getExtensions().getByType(MjjbExtension.class);
    MjjbGenerateTask generateTask = (MjjbGenerateTask) project.getTasks().getByName("generateMjjb");
    org.gradle.api.Task compileJava =
        project.getTasks().getByName(JavaPlugin.COMPILE_JAVA_TASK_NAME);
    JavaPluginExtension javaExtension =
        project.getExtensions().getByType(JavaPluginExtension.class);

    assertTrue(
        compileJava.getTaskDependencies().getDependencies(compileJava).contains(generateTask));
    assertTrue(
        javaExtension
            .getSourceSets()
            .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
            .getJava()
            .getSrcDirs()
            .contains(extension.getOutputDirectory().get().getAsFile()));
  }

  @Test
  void generateTaskDeclaresCacheableInputsAndOutputs() throws NoSuchMethodException {
    assertTrue(MjjbGenerateTask.class.isAnnotationPresent(CacheableTask.class));
    assertAnnotated("getSchemas", InputFiles.class);
    assertEquals(
        PathSensitivity.RELATIVE,
        MjjbGenerateTask.class.getMethod("getSchemas").getAnnotation(PathSensitive.class).value());
    assertAnnotated("getOutputDirectory", OutputDirectory.class);
    assertAnnotated("getProfile", Input.class);
    assertAnnotated("getDefaultPackage", Input.class);
    assertAnnotated("getRootTypeName", Input.class);
    assertAnnotated("getPackageMappings", Input.class);
  }

  @Test
  void generateTaskWritesCustomRootTypeSources() throws IOException {
    Path schema =
        writeSchema("{\"type\":\"object\",\"properties\":{},\"additionalProperties\":false}");
    Path output = tempDir.resolve("generated");
    Project project = newProject();
    project.getPluginManager().apply(MjjbGradlePlugin.class);
    MjjbExtension extension = project.getExtensions().getByType(MjjbExtension.class);
    extension.schema(schema);
    extension.getOutputDirectory().set(output.toFile());
    extension.getDefaultPackage().set("com.example.generated");
    extension.getRootTypeName().set("GradleBinding");

    MjjbGenerateTask task = (MjjbGenerateTask) project.getTasks().getByName("generateMjjb");
    task.generate();

    assertTrue(Files.isRegularFile(output.resolve("com/example/generated/GradleBinding.java")));
    assertTrue(
        Files.isRegularFile(output.resolve("com/example/generated/GradleBindingJsonWriter.java")));
    assertTrue(
        Files.isRegularFile(output.resolve("com/example/generated/GradleBindingJsonReader.java")));
    assertTrue(
        Files.isRegularFile(
            output.resolve("com/example/generated/GradleBindingJsonValidator.java")));
  }

  @Test
  void generateTaskHandlesMultipleSchemasInDeterministicPathOrder() throws IOException {
    Path schemaDirectory = Files.createDirectories(tempDir.resolve("schemas"));
    Path zetaSchema =
        writeSchema(
            schemaDirectory,
            "zeta.schema.json",
            "{\"type\":\"object\",\"properties\":{\"zeta\":{\"type\":\"string\"}},"
                + "\"additionalProperties\":false}");
    Path alphaSchema =
        writeSchema(
            schemaDirectory,
            "alpha.schema.json",
            "{\"type\":\"object\",\"properties\":{\"alpha\":{\"type\":\"string\"}},"
                + "\"additionalProperties\":false}");
    Path output = tempDir.resolve("generated-multiple");
    Project project = newProject();
    project.getPluginManager().apply(MjjbGradlePlugin.class);
    MjjbExtension extension = project.getExtensions().getByType(MjjbExtension.class);
    extension.schema(zetaSchema);
    extension.schema(alphaSchema);
    extension.getOutputDirectory().set(output.toFile());
    extension.getDefaultPackage().set("com.example.generated");
    extension.getRootTypeName().set("DeterministicBinding");

    MjjbGenerateTask task = (MjjbGenerateTask) project.getTasks().getByName("generateMjjb");
    task.generate();

    String generatedModel =
        Files.readString(output.resolve("com/example/generated/DeterministicBinding.java"));
    assertTrue(generatedModel.contains("alpha"));
    assertFalse(generatedModel.contains("zeta"));
  }

  @Test
  void generateTaskDeletesStaleGeneratedSourcesWhenReplacingOutput() throws IOException {
    Path schema =
        writeSchema("{\"type\":\"object\",\"properties\":{},\"additionalProperties\":false}");
    Path output = tempDir.resolve("generated-replace");
    Path generatedPackage = Files.createDirectories(output.resolve("com/example/generated"));
    Path stalePackage = Files.createDirectories(output.resolve("com/example/stale"));
    Path staleSamePackage = generatedPackage.resolve("OldBinding.java");
    Path staleOtherPackage = stalePackage.resolve("StaleBinding.java");
    Files.writeString(staleSamePackage, "stale");
    Files.writeString(staleOtherPackage, "stale");
    Project project = newProject();
    project.getPluginManager().apply(MjjbGradlePlugin.class);
    MjjbExtension extension = project.getExtensions().getByType(MjjbExtension.class);
    extension.schema(schema);
    extension.getOutputDirectory().set(output.toFile());
    extension.getDefaultPackage().set("com.example.generated");
    extension.getRootTypeName().set("FreshBinding");

    MjjbGenerateTask task = (MjjbGenerateTask) project.getTasks().getByName("generateMjjb");
    task.generate();

    assertTrue(Files.isRegularFile(output.resolve("com/example/generated/FreshBinding.java")));
    assertFalse(Files.exists(staleSamePackage));
    assertFalse(Files.exists(staleOtherPackage));
  }

  @Test
  void packageMappingsConfiguredOnExtensionArePropagatedToGenerateTask() throws IOException {
    Project project = newProject();
    project.getPluginManager().apply(MjjbGradlePlugin.class);
    MjjbExtension extension = project.getExtensions().getByType(MjjbExtension.class);
    extension.packageMapping("https://schemas.example.test/zeta", "com.example.zeta");
    extension.packageMapping("https://schemas.example.test/alpha", "com.example.alpha");

    MjjbGenerateTask task = (MjjbGenerateTask) project.getTasks().getByName("generateMjjb");

    assertEquals(
        Map.of(
            "https://schemas.example.test/alpha",
            "com.example.alpha",
            "https://schemas.example.test/zeta",
            "com.example.zeta"),
        task.getPackageMappings().get());
  }

  @Test
  void generateTaskRejectsInvalidProfile() throws IOException {
    MjjbGenerateTask task = configuredTask(writeSchema("{\"type\":\"object\"}"));
    task.getProfile().set("NOPE");

    GradleException exception = assertThrows(GradleException.class, task::generate);

    assertTrue(exception.getMessage().contains("MJJB-GRADLE-001"));
  }

  @Test
  void generateTaskRejectsInvalidDefaultPackage() throws IOException {
    MjjbGenerateTask task = configuredTask(writeSchema("{\"type\":\"object\"}"));
    task.getDefaultPackage().set("1.bad");

    GradleException exception = assertThrows(GradleException.class, task::generate);

    assertTrue(exception.getMessage().contains("MJJB-GRADLE-002"));
  }

  @Test
  void generateTaskRejectsJavaKeywordPackageSegment() throws IOException {
    MjjbGenerateTask task = configuredTask(writeSchema("{\"type\":\"object\"}"));
    task.getDefaultPackage().set("com.example.class");

    GradleException exception = assertThrows(GradleException.class, task::generate);

    assertTrue(exception.getMessage().contains("MJJB-GRADLE-002"));
  }

  @Test
  void generateTaskRejectsInvalidRootTypeName() throws IOException {
    MjjbGenerateTask task = configuredTask(writeSchema("{\"type\":\"object\"}"));
    task.getRootTypeName().set("1Bad");

    GradleException exception = assertThrows(GradleException.class, task::generate);

    assertTrue(exception.getMessage().contains("MJJB-GRADLE-003"));
  }

  @Test
  void generateTaskRejectsJavaKeywordRootTypeName() throws IOException {
    MjjbGenerateTask task = configuredTask(writeSchema("{\"type\":\"object\"}"));
    task.getRootTypeName().set("class");

    GradleException exception = assertThrows(GradleException.class, task::generate);

    assertTrue(exception.getMessage().contains("MJJB-GRADLE-003"));
  }

  @Test
  void generateTaskSurfacesMissingSchemaCollectionDiagnostics() throws IOException {
    Project project = newProject();
    project.getPluginManager().apply(MjjbGradlePlugin.class);
    MjjbExtension extension = project.getExtensions().getByType(MjjbExtension.class);
    extension.getOutputDirectory().set(tempDir.resolve("out-empty").toFile());
    MjjbGenerateTask task = (MjjbGenerateTask) project.getTasks().getByName("generateMjjb");

    GradleException exception = assertThrows(GradleException.class, task::generate);

    assertTrue(exception.getMessage().contains("MJJBG-GEN-001"));
  }

  @Test
  void generateTaskSurfacesMissingSchemaDiagnostics() throws IOException {
    MjjbGenerateTask task = configuredTask(tempDir.resolve("missing.json"));

    GradleException exception = assertThrows(GradleException.class, task::generate);

    assertTrue(exception.getMessage().contains("MJJBG-GEN-003"));
  }

  @Test
  void generateTaskSurfacesUnsupportedKeywordDiagnostics() throws IOException {
    MjjbGenerateTask task =
        configuredTask(
            writeSchema("{\"type\":\"object\",\"properties\":{\"id\":{\"$dynamicRef\":\"#x\"}}}"));

    GradleException exception = assertThrows(GradleException.class, task::generate);

    assertTrue(exception.getMessage().contains("MJJBG-SCHEMA-UNSUPPORTED-KEYWORD"));
  }

  @Test
  void testKitProjectGeneratesSourcesBeforeJavaCompilation() throws IOException {
    Path projectDir = Files.createDirectories(tempDir.resolve("testkit-project"));
    Files.writeString(projectDir.resolve("settings.gradle"), "rootProject.name = 'plugin-smoke'\n");
    Files.createDirectories(projectDir.resolve("src/main/schema"));
    Files.writeString(
        projectDir.resolve("src/main/schema/binding.schema.json"),
        "{\"type\":\"object\",\"properties\":{},\"additionalProperties\":false}");
    Files.createDirectories(projectDir.resolve("src/main/java/com/example/app"));
    Files.writeString(
        projectDir.resolve("src/main/java/com/example/app/UseGenerated.java"),
        """
        package com.example.app;

        import com.example.generated.PluginBinding;
        import com.example.generated.PluginBindingJsonValidator;

        public final class UseGenerated {
          public boolean valid() {
            return PluginBindingJsonValidator.validate(new PluginBinding()).isValid();
          }
        }
        """);
    Files.writeString(projectDir.resolve("build.gradle"), testKitBuildFile());

    BuildResult result =
        GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withArguments("compileJava", "--stacktrace")
            .build();

    assertEquals(TaskOutcome.SUCCESS, result.task(":generateMjjb").getOutcome());
    assertEquals(TaskOutcome.SUCCESS, result.task(":compileJava").getOutcome());
  }

  private MjjbGenerateTask configuredTask(Path schema) throws IOException {
    Project project = newProject();
    project.getPluginManager().apply(MjjbGradlePlugin.class);
    MjjbExtension extension = project.getExtensions().getByType(MjjbExtension.class);
    extension.schema(schema);
    extension.getOutputDirectory().set(tempDir.resolve("out").toFile());
    return (MjjbGenerateTask) project.getTasks().getByName("generateMjjb");
  }

  private Project newProject() throws IOException {
    return ProjectBuilder.builder()
        .withProjectDir(Files.createTempDirectory(tempDir, "project").toFile())
        .build();
  }

  private Path writeSchema(String schema) throws IOException {
    Path path = Files.createTempFile(tempDir, "schema", ".json");
    Files.writeString(path, schema);
    return path;
  }

  private Path writeSchema(Path directory, String fileName, String schema) throws IOException {
    Path path = directory.resolve(fileName);
    Files.writeString(path, schema);
    return path;
  }

  private static void assertAnnotated(String methodName, Class<? extends Annotation> annotation)
      throws NoSuchMethodException {
    Method method = MjjbGenerateTask.class.getMethod(methodName);
    assertNotNull(method.getAnnotation(annotation));
  }

  private static String testKitBuildFile() {
    return """
        plugins {
          id 'java'
          id 'io.github.mundanej.mjjb'
        }

        dependencies {
          implementation files(@RUNTIME_CORE_FILES@)
        }

        mjjb {
          schema('src/main/schema/binding.schema.json')
          outputDirectory.set(layout.buildDirectory.dir('generated/mjjb/main/java'))
          defaultPackage.set('com.example.generated')
          rootTypeName.set('PluginBinding')
        }
        """
        .replace("@RUNTIME_CORE_FILES@", runtimeCoreFilesExpression());
  }

  private static String runtimeCoreFilesExpression() {
    List<Path> entries = runtimeCoreClasspath();
    assertTrue(!entries.isEmpty(), "runtime-core classpath entries are required");
    return entries.stream()
        .map(MjjbGradlePluginUnitTest::quotedPath)
        .collect(Collectors.joining(", "));
  }

  private static List<Path> runtimeCoreClasspath() {
    List<Path> classpathEntries =
        Arrays.stream(System.getProperty("java.class.path", "").split(File.pathSeparator))
            .filter(entry -> !entry.isBlank())
            .map(Path::of)
            .filter(Files::exists)
            .filter(path -> path.toString().contains("runtime-core"))
            .toList();
    if (!classpathEntries.isEmpty()) {
      return classpathEntries;
    }
    Path currentDirectory = Path.of("").toAbsolutePath();
    return List.of(
            currentDirectory.resolve("modules/runtime-core/build/classes/java/main"),
            currentDirectory.resolve("../runtime-core/build/classes/java/main").normalize(),
            currentDirectory
                .resolve("../../modules/runtime-core/build/classes/java/main")
                .normalize(),
            currentDirectory.resolve(
                "modules/runtime-core/build/libs/mjjb-runtime-core-0.1.0-SNAPSHOT.jar"),
            currentDirectory
                .resolve("../runtime-core/build/libs/mjjb-runtime-core-0.1.0-SNAPSHOT.jar")
                .normalize())
        .stream()
        .filter(Files::exists)
        .toList();
  }

  private static String quotedPath(Path path) {
    return "'"
        + path.toAbsolutePath().normalize().toString().replace("\\", "\\\\").replace("'", "\\'")
        + "'";
  }
}
