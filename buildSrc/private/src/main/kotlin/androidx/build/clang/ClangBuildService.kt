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

package androidx.build.clang

import androidx.build.KonanPrebuiltsSetup
import androidx.build.ProjectLayoutType
import androidx.build.clang.ClangBuildService.Companion.obtain
import androidx.build.defaultAndroidConfig
import androidx.build.getKonanPrebuiltsFolder
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Optional
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.konan.target.PlatformManager

/**
 * A Gradle [BuildService] that provides access to native compiler tools (clang, linker, ar etc) to
 * build native sources for multiple native targets.
 *
 * You can obtain the instance via [obtain].
 *
 * @see ClangArchiveTask
 * @see ClangCompileTask
 * @see ClangLinkerTask
 */
abstract class ClangBuildService @Inject constructor(private val execOperations: ExecOperations) :
    BuildService<ClangBuildService.Parameters> {
    private val dist by lazy {
        // double check that we don't initialize konan distribution without prebuilts in AOSP
        check(
            parameters.projectLayoutType.get() == ProjectLayoutType.PLAYGROUND ||
                parameters.prebuilts.isPresent
        ) {
            """
            Prebuilts directory for Konan must be provided when the project is not a playground
            project.
            """
                .trimIndent()
        }
        KonanPrebuiltsSetup.createKonanDistribution(
            prebuiltsDirectory = parameters.prebuilts.orNull?.asFile,
            konanHome = parameters.konanHome.get().asFile,
        )
    }

    private val androidBuilder by lazy {
        AndroidPlatformBuilder(
            execOperations = execOperations,
            ndkDirectory = parameters.ndkDirectory.get().asFile,
            sdkLevel = parameters.androidMinSdk.get(),
        )
    }

    private val konanBuilder by lazy {
        KonanPlatformBuilder(
            execOperations = execOperations,
            platformManager = PlatformManager(dist),
        )
    }

    private fun getBuilder(target: NativeTarget): ClangPlatformBuilder {
        return if (target.isAndroid) androidBuilder else konanBuilder
    }

    /** @see ClangCompileTask */
    fun compile(parameters: ClangCompileParameters) {
        val target = parameters.target.get().asNativeTarget
        getBuilder(target).compile(parameters)
    }

    /** @see ClangArchiveTask */
    fun archiveLibrary(parameters: ClangArchiveParameters) {
        val target = parameters.target.get().asNativeTarget
        getBuilder(target).archiveLibrary(parameters)
    }

    /** @see ClangLinkerTask */
    fun runLinker(parameters: ClangLinkerParameters) {
        val target = parameters.target.get().asNativeTarget
        getBuilder(target).runLinker(parameters)
    }

    fun stdlibKlibDir(): Provider<Directory> {
        return parameters.konanHome.dir("klib/common/stdlib")
    }

    interface Parameters : BuildServiceParameters {
        /** KONAN_HOME parameter for initializing konan */
        val konanHome: DirectoryProperty

        /** Location if konan prebuilts. Can be null if this is a playground project */
        @get:Optional val prebuilts: DirectoryProperty

        /**
         * The type of the project (Playground vs AOSP main). This value is used to ensure we
         * initialize Konan distribution properly.
         */
        val projectLayoutType: Property<ProjectLayoutType>

        /** Location of the Android NDK. */
        val ndkDirectory: DirectoryProperty

        /** Android minSdk version to use for the NDK. */
        val androidMinSdk: Property<Int>
    }

    companion object {
        internal const val KEY = "clangBuildService"

        fun obtain(project: Project): Provider<ClangBuildService> {
            return project.gradle.sharedServices.registerIfAbsent(
                KEY,
                ClangBuildService::class.java,
            ) {
                check(project.plugins.hasPlugin(KotlinMultiplatformPluginWrapper::class.java)) {
                    "ClangBuildService can only be used in projects that applied the KMP plugin"
                }
                check(KonanPrebuiltsSetup.isConfigured(project)) {
                    "Konan prebuilt directories are not configured for project \"${project.path}\""
                }
                // TODO(b/539475193): Download Konan prebuilts only when building non-Android.
                val nativeCompilerDownloader = NativeCompilerDownloader(project)
                nativeCompilerDownloader.downloadIfNeeded()

                it.parameters.konanHome.set(nativeCompilerDownloader.compilerDirectory)
                it.parameters.projectLayoutType.set(ProjectLayoutType.from(project))
                it.parameters.androidMinSdk.set(project.defaultAndroidConfig.minSdk)

                val finalNdkDir = project.getNdkDirectory()
                checkNotNull(finalNdkDir) {
                    "NDK directory could not be resolved. Please ensure NDK is installed."
                }
                it.parameters.ndkDirectory.set(finalNdkDir)

                if (!ProjectLayoutType.isPlayground(project)) {
                    it.parameters.prebuilts.set(project.getKonanPrebuiltsFolder())
                }
            }
        }
    }
}
