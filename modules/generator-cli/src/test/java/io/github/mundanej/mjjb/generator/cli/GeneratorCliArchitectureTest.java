package io.github.mundanej.mjjb.generator.cli;

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
    packages = "io.github.mundanej.mjjb.generator.cli",
    importOptions = ImportOption.DoNotIncludeTests.class)
final class GeneratorCliArchitectureTest {
  @ArchTest
  static final ArchRule cliOnlyDependsOnGeneratorAndJdk =
      classes()
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage(
              "io.github.mundanej.mjjb.generator.cli..",
              "io.github.mundanej.mjjb.generator.api..",
              "io.github.mundanej.mjjb.generator.core..",
              "java..");

  @ArchTest
  static final ArchRule cliDoesNotUseReflectionOrProcessSpawning =
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
  static final ArchRule cliDoesNotDeclareNativeMethodsOrFinalizers =
      noMethods().should().haveModifier(JavaModifier.NATIVE).orShould().haveName("finalize");

  @ArchTest
  static final ArchRule cliPublicStaticFieldsAreFinal =
      fields().that().arePublic().and().areStatic().should().beFinal().allowEmptyShould(true);
}
