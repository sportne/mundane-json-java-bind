package io.github.mundanej.mjjb.testkit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "io.github.mundanej.mjjb.testkit",
    importOptions = ImportOption.DoNotIncludeTests.class)
final class TestingSupportArchitectureTest {
  @ArchTest
  static final ArchRule testingSupportOnlyDependsOnRuntimeAndJdk =
      classes()
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage(
              "io.github.mundanej.mjjb.testkit..", "io.github.mundanej.mjjb.runtime..", "java..");

  @ArchTest
  static final ArchRule testingSupportDoesNotDependOnGeneratorParserOrTooling =
      noClasses()
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "io.github.mundanej.mjjb.generator..",
              "io.github.mundanej.mjjb.parser..",
              "io.github.mundanej.mjjb.schema..");

  @ArchTest
  static final ArchRule testingSupportPublicStaticFieldsAreFinal =
      fields().that().arePublic().and().areStatic().should().beFinal().allowEmptyShould(true);
}
