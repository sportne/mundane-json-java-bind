package io.github.mundanej.mjjb.generator.api;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "io.github.mundanej.mjjb.generator.api",
    importOptions = ImportOption.DoNotIncludeTests.class)
final class GeneratorApiArchitectureTest {
  @ArchTest
  static final ArchRule generatorApiOnlyDependsOnApiAndJdk =
      classes()
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage("io.github.mundanej.mjjb.generator.api..", "java..");

  @ArchTest
  static final ArchRule generatorApiDoesNotExposeImplementationTypes =
      noClasses()
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "io.github.mundanej.mjjb.generator.core..",
              "io.github.mundanej.mjjb.schema..",
              "io.github.mundanej.mjjb.parser..",
              "io.github.mundanej.mjjb.generator.cli..",
              "io.github.mundanej.mjjb.generator.gradle..",
              "org.gradle..");

  @ArchTest
  static final ArchRule generatorApiPublicStaticFieldsAreFinal =
      fields().that().arePublic().and().areStatic().should().beFinal().allowEmptyShould(true);
}
