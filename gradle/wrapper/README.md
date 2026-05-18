# Gradle Wrapper

`gradle-wrapper.properties` pins Gradle 9.5.1. The standard Gradle wrapper
scripts and `gradle-wrapper.jar` are committed so contributors can run the build
without a system Gradle installation.

For strict offline repositories, pre-provision the Gradle 9.5.1 distribution or
point `distributionUrl` at an approved internal mirror before invoking
`./gradlew --offline`.
