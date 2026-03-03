plugins {
    java
    application
    id("com.gradleup.shadow") version "8.3.6"
}

group = "at.lzito.workflowmanager"
version = "1.0.0"

// Build outputs go to the Linux filesystem to avoid WSL/NTFS chmod failures.
// The final JAR is copied back to the project's build/libs/ for convenience.
layout.buildDirectory.set(file("/tmp/wm-build"))

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

// Fat JAR: fixed filename so the self-updater always knows what to replace.
// Version is embedded in the manifest (Implementation-Version) so no resource
// filtering is needed — avoids NTFS chmod failures on WSL.
tasks.shadowJar {
    archiveBaseName = "workflow-manager"
    archiveClassifier = ""
    archiveVersion = ""          // produces workflow-manager.jar (no version suffix)
    manifest {
        attributes["Implementation-Version"] = project.version
    }
    mergeServiceFiles()
}

// Make `gradle build` produce the fat jar
tasks.build {
    dependsOn(tasks.shadowJar)
}
