plugins {
    java
    application
    id("com.gradleup.shadow") version "8.3.6"
}

group = "at.lzito.workflowmanager"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

application {
    mainClass = "at.lzito.workflowmanager.App"
}

repositories {
    mavenCentral()
}

dependencies {
    // Dark mode UI theme
    implementation("com.formdev:flatlaf:3.5.4")

    // Global hotkeys (system-wide, works when app is minimized)
    implementation("com.github.kwhat:jnativehook:2.2.2")

    // JSON config parsing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")

    // Test
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// Fat JAR: bundle all dependencies into one runnable jar
tasks.shadowJar {
    archiveBaseName = "workflow-manager"
    archiveClassifier = ""
    archiveVersion = version.toString()
    mergeServiceFiles()
}

// Make `gradle build` produce the fat jar
tasks.build {
    dependsOn(tasks.shadowJar)
}
