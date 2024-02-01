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

import java.io.File
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.konan.target.Distribution

/**
 * Helper class to override Konan prebuilts directories to use local konan prebuilts.
 */
object KonanPrebuiltsSetup {
    /**
     * Flag to notify we've updated the konan properties so that we can avoid re-doing it
     * if [configureKonanDirectory] call comes from multiple code paths.
     */
    private const val DID_SETUP_KONAN_PROPERTIES_FLAG = "androidx.didSetupKonanProperties"

    /**
     * Creates a Konan distribution with the given [prebuiltsDirectory] and [konanHome].
     */
    fun createKonanDistribution(
        prebuiltsDirectory: File,
        konanHome: File
    ) = Distribution(
        konanHome = konanHome.canonicalPath,
        onlyDefaultProfiles = false,
        propertyOverrides = mapOf(
            "dependenciesUrl" to "file://${prebuiltsDirectory.canonicalPath}"
        )
    )

    /**
     * Returns `true` if the project's konan prebuilts is already configured.
     */
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
        val konanPrebuiltsFolder = getKonanPrebuiltsFolder()
        // use relative path so it doesn't affect gradle remote cache.
        val relativeRootPath = konanPrebuiltsFolder.relativeTo(rootProject.projectDir).path
        val relativeProjectPath = konanPrebuiltsFolder.relativeTo(projectDir).path
        tasks.withType(KotlinNativeCompile::class.java).configureEach {
            it.kotlinOptions.freeCompilerArgs +=
                listOf("-Xoverride-konan-properties=dependenciesUrl=file:$relativeRootPath")
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
