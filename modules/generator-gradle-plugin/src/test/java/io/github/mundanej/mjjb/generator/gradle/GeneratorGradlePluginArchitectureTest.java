package io.github.mundanej.mjjb.generator.gradle;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "io.github.mundanej.mjjb.generator.gradle",
    importOptions = ImportOption.DoNotIncludeTests.class)
final class GeneratorGradlePluginArchitectureTest {
  @ArchTest
  static final ArchRule gradlePluginOnlyDependsOnGeneratorGradleApiAndJdk =
      classes()
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage(
              "io.github.mundanej.mjjb.generator.gradle..",
              "io.github.mundanej.mjjb.generator.api..",
              "io.github.mundanej.mjjb.generator.core..",
              "org.gradle..",
              "javax.inject..",
              "java..");

  @ArchTest
  static final ArchRule gradlePluginDoesNotUseReflectionOrProcessSpawning =
      noClasses()
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("java.lang.reflect..", "java.lang.invoke..")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.lang.ProcessBuilder")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.lang.Runtime")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.lang.reflect.Proxy");

  @ArchTest
  static final ArchRule gradlePluginDoesNotDeclareNativeMethodsOrFinalizers =
      noMethods().should().haveModifier(JavaModifier.NATIVE).orShould().haveName("finalize");

  @ArchTest
  static final ArchRule gradlePluginPublicStaticFieldsAreFinal =
      fields().that().arePublic().and().areStatic().should().beFinal().allowEmptyShould(true);
}
