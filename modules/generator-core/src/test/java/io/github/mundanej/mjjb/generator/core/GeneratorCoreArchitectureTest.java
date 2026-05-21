package io.github.mundanej.mjjb.generator.core;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "io.github.mundanej.mjjb.generator.core",
    importOptions = ImportOption.DoNotIncludeTests.class)
final class GeneratorCoreArchitectureTest {
  @ArchTest
  static final ArchRule generatorCoreOnlyDependsOnGeneratorSchemaRuntimeAndJdk =
      classes()
          .that()
          .resideOutsideOfPackage("io.github.mundanej.mjjb.generator.core.generated..")
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage(
              "io.github.mundanej.mjjb.generator.core..",
              "io.github.mundanej.mjjb.generator.api..",
              "io.github.mundanej.mjjb.schema.model..",
              "io.github.mundanej.mjjb.runtime..",
              "java..");

  @ArchTest
  static final ArchRule generatorCoreDoesNotDependOnParserOrUserTooling =
      noClasses()
          .that()
          .resideOutsideOfPackage("io.github.mundanej.mjjb.generator.core.generated..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "io.github.mundanej.mjjb.parser..",
              "io.github.mundanej.mjjb.generator.cli..",
              "io.github.mundanej.mjjb.generator.gradle..",
              "io.github.mundanej.mjjb.testkit..");

  @ArchTest
  static final ArchRule generatorCorePublicStaticFieldsAreFinal =
      fields().that().arePublic().and().areStatic().should().beFinal().allowEmptyShould(true);
}
