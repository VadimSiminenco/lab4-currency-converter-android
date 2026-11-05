plugins { id("org.jetbrains.kotlin.jvm") }
kotlin { jvmToolchain(17) }
dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.appium:java-client:9.5.0")
    testImplementation("org.seleniumhq.selenium:selenium-support:4.34.0")
}
// Java Client uses open Selenium ranges; resolve every Selenium component to the checked matrix entry.
configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.seleniumhq.selenium") useVersion("4.34.0")
    }
}
tasks.test {
    useJUnit()
    maxParallelForks = 1
    onlyIf { providers.gradleProperty("runE2e").orNull == "true" }
    systemProperty("e2e.serial",providers.gradleProperty("serial").orNull ?: "emulator-5554")
    systemProperty("e2e.server",providers.gradleProperty("appiumUrl").orNull ?: "http://127.0.0.1:4723")
    systemProperty("e2e.adb",providers.gradleProperty("adbPath").orNull ?: "adb")
    outputs.upToDateWhen { false }
    testLogging { events("passed","failed","skipped"); exceptionFormat=org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
}
