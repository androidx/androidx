/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.build

import com.android.Version.ANDROID_GRADLE_PLUGIN_VERSION
import java.io.File
import java.nio.file.Files
import org.gradle.api.GradleException

/** Defines and enforces the environment that managed IDEs must run in. */
object BuildEnvironment {
    /** Environment variable carrying the path to the JDK 21. */
    const val ANDROIDX_JDK21 = "ANDROIDX_JDK21"

    /** Environment variable used to scope the build to a named project subset. */
    const val ANDROIDX_PROJECTS = "ANDROIDX_PROJECTS"

    /** Environment variable used to scope the build to projects matching path prefixes. */
    const val PROJECT_PREFIX = "PROJECT_PREFIX"

    /**
     * Environment variable carrying the AGP version an IDE was launched against. settings.gradle
     * requires it to be present for any IDE-initiated build, and [AndroidXRootImplPlugin] requires
     * it to match [expectedAgpVersion].
     */
    const val EXPECTED_AGP_VERSION = "EXPECTED_AGP_VERSION"

    /**
     * The AGP version on the current buildscript classpath. This is the single definition of the
     * value propagated to IDEs as [EXPECTED_AGP_VERSION] and re-checked when the IDE invokes
     * Gradle, which prevents version mismatch after a repo sync while an IDE is still running.
     */
    val expectedAgpVersion: String
        get() = ANDROID_GRADLE_PLUGIN_VERSION

    /**
     * Environment variables that every managed IDE process must be launched with, on top of the
     * environment it inherits from gradlew.
     */
    fun ideEnvironment(): Map<String, String> =
        mapOf(
            // This environment variable prevents the IDE from showing inspection warnings for
            // nullability issues, if the context is deprecated. It is consumed by
            // InteroperabilityDetector.kt
            "ANDROID_LINT_NULLNESS_IGNORE_DEPRECATED" to "true",
            // This environment variable is read by AndroidXRootImplPlugin to ensure that
            // IDE-initiated Gradle tasks are run against the same version of AGP that was used to
            // start the IDE, which prevents version mismatch after repo sync.
            EXPECTED_AGP_VERSION to expectedAgpVersion,
        ) + platformSpecificEnvironmentProperties()

    /** `true` if the build is scoped to a subset of projects (see settings.gradle). */
    fun hasProjectScope(): Boolean =
        System.getenv().containsKey(ANDROIDX_PROJECTS) ||
            System.getenv().containsKey(PROJECT_PREFIX)

    /**
     * Requires the build to be scoped via [ANDROIDX_PROJECTS] or [PROJECT_PREFIX].
     *
     * When neither is set, settings.gradle includes the entire ~900-project graph, which no IDE
     * handles well, so IDE launchers must treat "neither set" as an error rather than a silent full
     * import.
     */
    fun requireProjectScope(ide: String, launchTask: String) {
        if (hasProjectScope()) return
        throw GradleException(
            """
            Please specify which set of projects you'd like to open in $ide
            with ANDROIDX_PROJECTS=MAIN ./gradlew $launchTask
            or PROJECT_PREFIX=:room3: ./gradlew $launchTask

            For possible options see settings.gradle
            """
                .trimIndent()
        )
    }

    /** Ensure that we can launch IDE without issue. */
    fun validateEnvironment(ide: String) {
        if (System.getenv().containsKey("SSH_CLIENT") && !System.getenv().containsKey("DISPLAY")) {
            throw GradleException(
                """
                $ide must be run from a graphical session.

                Could not read DISPLAY environment variable.  If you are using SSH into a remote
                machine, consider using either ssh -X or switching to Chrome Remote Desktop.
                """
                    .trimIndent()
            )
        }
    }

    fun platformSpecificEnvironmentProperties(): Map<String, String> {
        return if (System.getenv("QT_QPA_PLATFORM") == "wayland") {
            // Emulators don't work on Wayland natively, make them go through XWayland
            mapOf("QT_QPA_PLATFORM" to "xcb")
        } else {
            emptyMap()
        }
    }

    /** Attempts to symlink the system-images and emulator SDK directories to a canonical SDK. */
    fun setupSymlinksIfNeeded(localSdkPath: File) {
        val paths = listOf("system-images", "emulator")
        if (!localSdkPath.canonicalFile.exists()) {
            // We probably got the support root folder wrong. Fail gracefully.
            return
        }

        val relativeSdkPath =
            when (val osType = getOperatingSystem()) {
                OperatingSystem.MAC -> "Library/Android/sdk"
                OperatingSystem.LINUX -> "Android/Sdk"
                else -> {
                    println("Failed to locate canonical SDK, unsupported operating system: $osType")
                    return
                }
            }

        val canonicalSdkPath = File(System.getenv("HOME"), relativeSdkPath)
        if (!canonicalSdkPath.exists()) {
            // In the future, we might want to try a little harder to locate a canonical SDK
            // path.
            println("Failed to locate canonical SDK, not found at: $canonicalSdkPath")
            return
        }

        paths.forEach { path ->
            val link = File(localSdkPath.canonicalFile, path)
            val target = File(canonicalSdkPath, path)
            if (!target.exists()) {
                println("Skipping canonical SDK symlink creation, not found at: $target")
            } else if (!link.exists()) {
                println("Creating canonical SDK symlink for $target...")
                Files.createSymbolicLink(link.toPath(), target.toPath())
            }
        }
    }
}
