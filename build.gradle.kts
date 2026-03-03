import java.io.ByteArrayOutputStream

plugins {
    java
    application
    id("com.gradleup.shadow") version "8.3.6"
}

group   = "at.lzito.workflowmanager"
version = findProperty("projectVersion") as String? ?: "0.0.0"

// On Linux/WSL, redirect build output to the Linux filesystem to avoid NTFS chmod failures.
// On native Windows (GitHub Actions CI) the default build/ directory is used.
if (System.getProperty("os.name", "").lowercase().contains("linux")) {
    layout.buildDirectory.set(file("/tmp/wm-build"))
}

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
    archiveBaseName    = "workflow-manager"
    archiveClassifier  = ""
    archiveVersion     = ""     // → workflow-manager.jar (no version suffix)
    manifest {
        attributes["Implementation-Version"] = project.version
    }
    mergeServiceFiles()
}

// Make `gradle build` produce the fat JAR
tasks.build {
    dependsOn(tasks.shadowJar)
}

// ─────────────────────────────────────────────────────────────────────────────
// release — build, tag, and publish a new GitHub release
//
// Usage:          gradle release -PprojectVersion=1.1.0
// Prerequisites:  gh CLI installed and authenticated (https://cli.github.com)
// ─────────────────────────────────────────────────────────────────────────────
tasks.register("release") {
    group       = "publishing"
    description = "Build, tag, and publish a new release. Usage: gradle release -PprojectVersion=x.y.z"

    dependsOn(tasks.named("shadowJar"))

    doLast {
        val ver = project.version.toString()
        val tag = "v$ver"

        // Runs a command, streams output to the console, throws on non-zero exit
        fun run(vararg cmd: String) {
            val exit = ProcessBuilder(*cmd)
                .inheritIO()
                .start()
                .waitFor()
            if (exit != 0) error("Command failed (exit $exit): ${cmd.joinToString(" ")}")
        }

        // Runs a command silently and returns stdout; returns null on non-zero exit
        fun capture(vararg cmd: String): String? {
            val proc = ProcessBuilder(*cmd).start()
            val out  = proc.inputStream.bufferedReader().readText().trim()
            return if (proc.waitFor() == 0) out else null
        }

        // ── Validate ──────────────────────────────────────────────────────────

        require(Regex("""\d+\.\d+\.\d+""").matches(ver)) {
            "projectVersion must be major.minor.patch (got: $ver)\nUsage: gradle release -PprojectVersion=1.1.0"
        }

        requireNotNull(capture("gh", "auth", "status")) {
            "gh CLI not found or not authenticated.\nInstall: https://cli.github.com  then run: gh auth login"
        }

        val dirty = capture("git", "status", "--porcelain")
        require(dirty.isNullOrBlank()) {
            "Working tree is dirty. Commit or stash changes first.\n$dirty"
        }

        require(capture("git", "rev-parse", tag) == null) {
            "Tag $tag already exists."
        }

        // ── Persist new version to gradle.properties ──────────────────────────

        val propsFile = file("gradle.properties")
        propsFile.writeText(
            propsFile.readText().replace(
                Regex("""^projectVersion=.*$""", RegexOption.MULTILINE),
                "projectVersion=$ver"
            )
        )

        // ── Commit, tag, push ─────────────────────────────────────────────────

        // Only commit if gradle.properties actually changed (i.e. version was bumped)
        val propsChanged = !capture("git", "diff", "--name-only", "gradle.properties").isNullOrBlank()
        if (propsChanged) {
            run("git", "add", "gradle.properties")
            run("git", "commit", "-m", "chore: release $tag")
        }
        run("git", "tag", "-a", tag, "-m", "Release $tag")
        run("git", "push")
        run("git", "push", "origin", tag)

        // ── Create GitHub release and attach JAR ──────────────────────────────

        val jar = file("/tmp/wm-build/libs/workflow-manager.jar")
        run("gh", "release", "create", tag,
            "--title", tag,
            "--generate-notes",
            "${jar.absolutePath}#workflow-manager.jar")

        println("\n✓ Released $tag")
        println("  https://github.com/LZito/Workflow_Manager/releases/tag/$tag")
    }
}
