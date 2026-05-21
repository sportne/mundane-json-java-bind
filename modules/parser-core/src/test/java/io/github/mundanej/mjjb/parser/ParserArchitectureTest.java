package io.github.mundanej.mjjb.parser;

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
    packages = "io.github.mundanej.mjjb.parser",
    importOptions = ImportOption.DoNotIncludeTests.class)
final class ParserArchitectureTest {
  @ArchTest
  static final ArchRule parserOnlyDependsOnRuntimeAndJdk =
      classes()
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage(
              "io.github.mundanej.mjjb.parser..", "io.github.mundanej.mjjb.runtime..", "java..");

  @ArchTest
  static final ArchRule parserDoesNotUseReflectionOrMethodHandles =
      noClasses()
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("java.lang.reflect..", "java.lang.invoke..");

  @ArchTest
  static final ArchRule parserDoesNotUseDynamicDiscoveryOrClassLoading =
      noClasses()
          .should()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.util.ServiceLoader")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.lang.ClassLoader")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.net.URLClassLoader")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.lang.reflect.Proxy");

  @ArchTest
  static final ArchRule parserDoesNotUseNativeProcessOrSerializationHooks =
      noClasses()
          .should()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.lang.ProcessBuilder")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.lang.Runtime")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.io.ObjectInputStream")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.io.ObjectOutputStream")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("sun.misc.Unsafe")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("jdk.internal.misc.Unsafe");

  @ArchTest
  static final ArchRule parserDoesNotDeclareNativeMethodsOrFinalizers =
      noMethods().should().haveModifier(JavaModifier.NATIVE).orShould().haveName("finalize");

  @ArchTest
  static final ArchRule parserPublicStaticFieldsAreFinal =
      fields().that().arePublic().and().areStatic().should().beFinal().allowEmptyShould(true);
}
