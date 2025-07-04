/*
 * Copyright 2022 The Android Open Source Project
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

@file:Suppress("UnstableApiUsage")

package androidx.stableaidl

import androidx.stableaidl.api.StableAidlExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.dsl.SdkComponents
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.DslExtension
import com.android.build.api.variant.Variant
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.KotlinMultiplatformAndroidPlugin
import com.android.utils.usLocaleCapitalize
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

private const val DEFAULT_VARIANT_NAME = "release"
private const val DEFAULT_KMP_VARIANT_NAME = "androidMain"
private const val EXTENSION_NAME = "stableAidl"
private const val PLUGIN_DIRNAME = "stable_aidl"
private const val GENERATED_PATH = "generated/source/$PLUGIN_DIRNAME"
private const val INTERMEDIATES_PATH = "intermediates/${PLUGIN_DIRNAME}_parcelable"

@Suppress("unused")
abstract class StableAidlPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension =
            project.extensions.create(EXTENSION_NAME, StableAidlExtensionImpl::class.java)

        project.plugins.configureEach { plugin ->
            @Suppress("UnstableApiUsage") // for KotlinMultiplatformAndroidPlugin
            when (plugin) {
                is AppPlugin -> applyToAndroidAfterAgp(project, extension)
                is KotlinMultiplatformAndroidPlugin -> applyToAndroidAfterAgp(project, extension)
                is LibraryPlugin -> applyToAndroidAfterAgp(project, extension)
            }
        }
    }

    // Suppress UnstableApiUsage for SdkComponents.getAidl(), Aidl, and DSL extension methods
    @Suppress("UnstableApiUsage")
    private fun applyToAndroidAfterAgp(project: Project, extension: StableAidlExtensionImpl) {
        val androidComponents =
            project.extensions.findByType(AndroidComponentsExtension::class.java)
                ?: throw GradleException("Stable AIDL plugin requires Android Gradle Plugin")

        val aidl = androidComponents.sdkComponents.aidl.get()
        val aidlExecutable = aidl.executable
        val aidlVersion = aidl.version

        // Extend the android sourceSet.
        androidComponents.registerSourceType(SOURCE_TYPE_STABLE_AIDL)
        androidComponents.registerSourceType(SOURCE_TYPE_STABLE_AIDL_IMPORTS)

        // Register the DSL extensions.
        androidComponents.registerExtension(
            DslExtension.Builder(EXTENSION_NAME)
                .extendProjectWith(StableAidlProjectDslExtension::class.java)
                .extendBuildTypeWith(StableAidlBuildTypeDslExtension::class.java)
                .build()
        ) { config ->
            project.objects.newInstance(StableAidlVariantExtension::class.java, config, project)
        }

        // Set up per-variant tasks.
        androidComponents.onVariants { variant ->
            val sourceDir = variant.sources.getByName(SOURCE_TYPE_STABLE_AIDL)
            val importsDir = variant.sources.getByName(SOURCE_TYPE_STABLE_AIDL_IMPORTS)
            val depImports = variant.getAidlArtifactsOnCompileClasspath()
            val outputDir = project.layout.buildDirectory.dir("$GENERATED_PATH/${variant.name}")
            val packagedDir =
                project.layout.buildDirectory.dir("$INTERMEDIATES_PATH/${variant.name}/out")

            val apiDirName = "$API_DIR/aidl${variant.name.usLocaleCapitalize()}"
            val builtApiDir = project.layout.buildDirectory.dir(apiDirName)
            val frozenApiDir = project.layout.projectDirectory.dir("$apiDirName/$CURRENT_API_DIR")

            // The framework supports Stable AIDL definitions starting in SDK 36. Prior to that,
            // we'll need to use manually-defined stubs.
            val compileSdkProvider =
                project.provider {
                    @Suppress("deprecation") // TODO(aurimas): migrate to new API
                    project.extensions.findByType(CommonExtension::class.java)?.compileSdk
                        ?: project.extensions
                            .findByType(KotlinMultiplatformExtension::class.java)
                            ?.extensions
                            ?.findByType(KotlinMultiplatformAndroidLibraryTarget::class.java)
                            ?.compileSdk
                        ?: throw RuntimeException("Failed to obtain compileSdk")
                }
            val aidlFramework =
                compileSdkProvider.flatMap { compileSdk ->
                    if (compileSdk >= 36) {
                        aidl.framework
                    } else {
                        project.objects.fileProperty()
                    }
                }
            val shadowFramework =
                compileSdkProvider.flatMap { compileSdk ->
                    if (compileSdk < 36) {
                        extension.shadowFrameworkDir
                    } else {
                        project.objects.directoryProperty()
                    }
                }

            val compileAidlApiTask =
                registerCompileAidlApi(
                    project,
                    variant,
                    aidlExecutable,
                    aidlFramework,
                    shadowFramework,
                    aidlVersion,
                    sourceDir,
                    packagedDir,
                    importsDir,
                    depImports,
                    outputDir,
                )

            // To avoid using the same output directory as AGP's AidlCompile task, we need to
            // register a post-processing task to copy packaged parcelable headers into the AAR.
            registerPackageAidlApi(project, variant, compileAidlApiTask)

            val generateAidlApiTask =
                registerGenerateAidlApi(
                    project,
                    variant,
                    aidlExecutable,
                    aidlFramework,
                    shadowFramework,
                    aidlVersion,
                    sourceDir,
                    importsDir,
                    depImports,
                    builtApiDir,
                    compileAidlApiTask,
                )
            val checkAidlApiReleaseTask =
                registerCheckApiAidlRelease(
                    project,
                    variant,
                    aidlExecutable,
                    aidlFramework,
                    shadowFramework,
                    importsDir,
                    depImports,
                    frozenApiDir,
                    generateAidlApiTask,
                )
            val checkAidlApiTask =
                registerCheckAidlApi(
                    project,
                    variant,
                    aidlExecutable,
                    aidlFramework,
                    shadowFramework,
                    importsDir,
                    depImports,
                    frozenApiDir,
                    generateAidlApiTask,
                    checkAidlApiReleaseTask,
                )
            val updateAidlApiTask =
                registerUpdateAidlApi(
                    project,
                    variant,
                    frozenApiDir,
                    generateAidlApiTask,
                    checkAidlApiReleaseTask,
                )

            if (variant.name == DEFAULT_VARIANT_NAME || variant.name == DEFAULT_KMP_VARIANT_NAME) {
                extension.updateTaskProvider = updateAidlApiTask
                extension.checkTaskProvider = checkAidlApiTask
            }

            extension.importSourceDirs.add(
                variant.sources.getByName(SOURCE_TYPE_STABLE_AIDL_IMPORTS)
            )

            extension.allTasks[variant.name] =
                setOf(
                    compileAidlApiTask,
                    generateAidlApiTask,
                    checkAidlApiReleaseTask,
                    checkAidlApiTask,
                    updateAidlApiTask,
                )
        }
    }
}

/** Directory under the project root in which various types of API files are stored. */
internal const val API_DIR = "api"

/** Directory under [API_DIR] where the frozen API files are stored. */
internal const val CURRENT_API_DIR = "current"

/** Source type for Stable AIDL files. */
internal const val SOURCE_TYPE_STABLE_AIDL = "stableAidl"

/**
 * Source type for AIDL files available to Stable AIDL files as imports. This should only be used
 * for shadowing framework files, and should not be used once the compile SDK has been annotated to
 * work with Stable ADIL.
 */
internal const val SOURCE_TYPE_STABLE_AIDL_IMPORTS = "stableAidlImports"

internal fun SdkComponents.aidl(): Provider<RegularFile> =
    @Suppress("UnstableApiUsage") aidl.flatMap { it.executable }

/** Returns the AIDL import directories for the given variant of the project. */
internal fun Variant.getAidlArtifactsOnCompileClasspath(): List<FileCollection> {
    val incoming = compileConfiguration.incoming
    val aidlFiles =
        incoming
            .artifactView { config -> config.attributes(ArtifactType.AIDL) }
            .artifacts
            .artifactFiles
    val stableAidlFiles =
        incoming
            .artifactView { config -> config.attributes(ArtifactType.STABLE_AIDL) }
            .artifacts
            .artifactFiles
    return listOfNotNull(aidlFiles, stableAidlFiles)
}

/**
 * When the Stable AIDL plugin is applied to the project, runs the specified [lambda] with access to
 * the plugin's public APIs via [StableAidlExtension].
 *
 * If the project does not have the Stable AIDL plugin applied, this is a no-op.
 */
fun Project.withStableAidlPlugin(lambda: (StableAidlExtension) -> Unit) {
    project.plugins.withId("androidx.stableaidl") { plugin ->
        (plugin as? StableAidlPlugin)?.let {
            project.extensions.findByType(StableAidlExtension::class.java)?.let { ext ->
                lambda(ext)
            } ?: throw GradleException("Failed to locate extension for StableAidlPlugin")
        } ?: throw GradleException("Plugin with ID \"androidx.stableaidl\" is not StableAidlPlugin")
    }
}
