package io.github.mundanej.mjjb.runtime;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "io.github.mundanej.mjjb.runtime",
    importOptions = ImportOption.DoNotIncludeTests.class)
final class RuntimeArchitectureTest {
  @ArchTest
  static final ArchRule runtimeDoesNotUseReflection =
      noClasses()
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("java.lang.reflect..", "java.lang.invoke..");

  @ArchTest
  static final ArchRule runtimeDoesNotUseDynamicDiscovery =
      noClasses().should().dependOnClassesThat().haveFullyQualifiedName("java.util.ServiceLoader");
}
