/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.build.gradle.extraPropertyOrNull
import java.io.File
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.konan.target.Distribution

/** Helper class to override Konan prebuilts directories to use local konan prebuilts. */
object KonanPrebuiltsSetup {
    /**
     * Flag to notify we've updated the konan properties so that we can avoid re-doing it if
     * [configureKonanDirectory] call comes from multiple code paths.
     */
    private const val DID_SETUP_KONAN_PROPERTIES_FLAG = "androidx.didSetupKonanProperties"

    /**
     * Flag that causes konan to run in a separate process whose working directory is the compiling
     * project (i.e. frameworks/support/room/room-runtime) and not the root project
     * (frameworks/support).
     */
    private const val DISABLE_COMPILER_DAEMON_FLAG = "kotlin.native.disableCompilerDaemon"

    /**
     * Creates a Konan distribution with the given [prebuiltsDirectory] and [konanHome].
     *
     * @param prebuiltsDirectory The directory where AndroidX prebuilts are present. Can be `null`
     *   for playground builds which means we'll fetch Kotlin Native prebuilts from the internet
     *   using the Kotlin Gradle Plugin.
     */
    fun createKonanDistribution(prebuiltsDirectory: File?, konanHome: File) =
        Distribution(
            konanHome = konanHome.canonicalPath,
            onlyDefaultProfiles = false,
            propertyOverrides =
                prebuiltsDirectory?.let { mapOf("dependenciesUrl" to "file://${it.canonicalPath}") }
        )

    /** Returns `true` if the project's konan prebuilts is already configured. */
    fun isConfigured(project: Project): Boolean {
        return project.extensions.extraProperties.has(DID_SETUP_KONAN_PROPERTIES_FLAG)
    }

    /** Sets the konan distribution url to the prebuilts directory. */
    fun configureKonanDirectory(project: Project) {
        check(!isConfigured(project)) {
            "Konan prebuilts directories for project ${project.path} are already configured"
        }
        if (ProjectLayoutType.isPlayground(project)) {
            // playground does not use prebuilts
        } else {
            // set konan prebuilts download URLs to AndroidX prebuilts
            project.overrideKotlinNativeDistributionUrlToLocalDirectory()
            project.overrideKotlinNativeDependenciesUrlToLocalDirectory()
        }
        project.extensions.extraProperties.set(DID_SETUP_KONAN_PROPERTIES_FLAG, true)
    }

    private fun Project.overrideKotlinNativeDependenciesUrlToLocalDirectory() {
        val compilerDaemonDisabled =
            extraPropertyOrNull(DISABLE_COMPILER_DAEMON_FLAG)?.toString()?.toBoolean() == true
        val konanPrebuiltsFolder = getKonanPrebuiltsFolder()
        val rootBaseDir = if (compilerDaemonDisabled) projectDir else rootProject.projectDir
        // use relative path so it doesn't affect gradle remote cache.
        val relativeRootPath = konanPrebuiltsFolder.relativeTo(rootBaseDir).path
        val relativeProjectPath = konanPrebuiltsFolder.relativeTo(projectDir).path
        tasks.withType(KotlinNativeCompile::class.java).configureEach {
            it.compilerOptions.freeCompilerArgs.add(
                "-Xoverride-konan-properties=dependenciesUrl=file:$relativeRootPath"
            )
        }
        tasks.withType(CInteropProcess::class.java).configureEach {
            it.settings.extraOpts +=
                listOf("-Xoverride-konan-properties", "dependenciesUrl=file:$relativeProjectPath")
        }
    }

    private fun Project.overrideKotlinNativeDistributionUrlToLocalDirectory() {
        val relativePath =
            getKonanPrebuiltsFolder().resolve("nativeCompilerPrebuilts").relativeTo(projectDir).path
        val url = "file:$relativePath"
        extensions.extraProperties["kotlin.native.distribution.baseDownloadUrl"] = url
    }
}
