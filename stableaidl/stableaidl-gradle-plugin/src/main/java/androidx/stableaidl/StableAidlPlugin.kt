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

package androidx.stableaidl

import androidx.stableaidl.api.StableAidlExtension
import com.android.build.api.dsl.SdkComponents
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.DslExtension
import com.android.build.api.variant.Variant
import com.android.build.gradle.BaseExtension
import com.android.utils.usLocaleCapitalize
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

private const val DEFAULT_VARIANT_NAME = "release"
private const val EXTENSION_NAME = "stableaidl"
private const val PLUGIN_DIRNAME = "stable_aidl"
private const val GENERATED_PATH = "generated/source/$PLUGIN_DIRNAME"
private const val INTERMEDIATES_PATH = "intermediates/${PLUGIN_DIRNAME}_parcelable"

@Suppress("unused", "UnstableApiUsage")
abstract class StableAidlPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
            ?: throw GradleException("Stable AIDL plugin requires Android Gradle Plugin")

        val extension = project.extensions.create(
            EXTENSION_NAME,
            StableAidlExtensionImpl::class.java
        )

        // Obtain the AIDL executable and framework AIDL file paths using private APIs. See
        // b/268237729 for public API request, after which we can obtain them from SdkComponents.
        val base = project.extensions.getByType(BaseExtension::class.java)
            ?: throw GradleException("Stable AIDL plugin requires Android Gradle Plugin")
        val aidlExecutable = androidComponents.sdkComponents.aidl(base)
        val aidlFramework = androidComponents.sdkComponents.aidlFramework(base)

        // Extend the android sourceSet.
        androidComponents.registerSourceType(SOURCE_TYPE_STABLE_AIDL)
        androidComponents.registerSourceType(SOURCE_TYPE_STABLE_AIDL_IMPORTS)

        // Extend AGP's project (e.g. android) and buildType DSLs.
        androidComponents.registerExtension(
            DslExtension.Builder("stableAidl")
                .extendProjectWith(StableAidlProjectDslExtension::class.java)
                .extendBuildTypeWith(StableAidlBuildTypeDslExtension::class.java)
                .build()
        ) { variantExtensionConfig ->
            // Propagate project and buildType configuration to variant.
            project.objects.newInstance(
                StableAidlVariantExtension::class.java
            ).also { variantExtension ->
                variantExtension.version.set(
                    variantExtensionConfig.buildTypeExtension(
                        StableAidlBuildTypeDslExtension::class.java
                    ).version
                        ?: variantExtensionConfig.projectExtension(
                            StableAidlProjectDslExtension::class.java
                        ).version
                )
            }
        }

        androidComponents.onVariants { variant ->
            val sourceDir = variant.sources.getByName(SOURCE_TYPE_STABLE_AIDL)
            val importsDir = variant.sources.getByName(SOURCE_TYPE_STABLE_AIDL_IMPORTS)
            val depImports = project.getAidlArtifactsOnCompileClasspath(variant)
            val outputDir = project.layout.buildDirectory.dir(
                "$GENERATED_PATH/${variant.name}")
            val packagedDir = project.layout.buildDirectory.dir(
                "$INTERMEDIATES_PATH/${variant.name}/out")

            val apiDirName = "$API_DIR/aidl${variant.name.usLocaleCapitalize()}"
            val builtApiDir = project.layout.buildDirectory.dir(apiDirName)
            val lastReleasedApiDir =
                project.layout.projectDirectory.dir("$apiDirName/$RELEASED_API_DIR")
            val lastCheckedInApiDir =
                project.layout.projectDirectory.dir("$apiDirName/$CURRENT_API_DIR")

            val compileAidlApiTask = registerCompileAidlApi(
                project,
                variant,
                aidlExecutable,
                aidlFramework,
                sourceDir,
                packagedDir,
                importsDir,
                depImports,
                outputDir
            )

            // To avoid using the same output directory as AGP's AidlCompile task, we need to
            // register a post-processing task to copy packaged parcelable headers into the AAR.
            registerPackageAidlApi(
                project,
                variant,
                compileAidlApiTask
            )

            val generateAidlApiTask = registerGenerateAidlApi(
                project,
                variant,
                aidlExecutable,
                aidlFramework,
                sourceDir,
                importsDir,
                depImports,
                builtApiDir,
                compileAidlApiTask
            )
            val checkAidlApiReleaseTask = registerCheckApiAidlRelease(
                project,
                variant,
                aidlExecutable,
                aidlFramework,
                importsDir,
                depImports,
                lastReleasedApiDir,
                generateAidlApiTask
            )
            val checkAidlApiTask = registerCheckAidlApi(
                project,
                variant,
                aidlExecutable,
                aidlFramework,
                importsDir,
                depImports,
                lastCheckedInApiDir,
                generateAidlApiTask,
                checkAidlApiReleaseTask
            )
            val updateAidlApiTask = registerUpdateAidlApi(
                project,
                variant,
                lastCheckedInApiDir,
                generateAidlApiTask
            )

            if (variant.name == DEFAULT_VARIANT_NAME) {
                extension.updateTaskProvider = updateAidlApiTask
                extension.checkTaskProvider = checkAidlApiTask
            }

            extension.importSourceDirs.add(
                variant.sources.getByName(SOURCE_TYPE_STABLE_AIDL_IMPORTS)
            )

            extension.allTasks[variant.name] = setOf(
                compileAidlApiTask,
                generateAidlApiTask,
                checkAidlApiReleaseTask,
                checkAidlApiTask,
                updateAidlApiTask
            )
        }
    }
}

/**
 * Directory under the project root in which various types of API files are stored.
 */
internal const val API_DIR = "api"

/**
 * Directory under [API_DIR] where the current (work-in-progress) API files are stored.
 */
internal const val CURRENT_API_DIR = "current"

/**
 * Directory under [API_DIR] where the released (frozen) API files are stored.
 */
internal const val RELEASED_API_DIR = "released"

/**
 * Source type for Stable AIDL files.
 */
internal const val SOURCE_TYPE_STABLE_AIDL = "stableAidl"

/**
 * Source type for AIDL files available to Stable AIDL files as imports. This should only be used
 * for shadowing framework files, and should not be used once the compile SDK has been annotated to
 * work with Stable ADIL.
 */
internal const val SOURCE_TYPE_STABLE_AIDL_IMPORTS = "stableAidlImports"

internal fun SdkComponents.aidl(baseExtension: BaseExtension): Provider<RegularFile> =
    sdkDirectory.map {
        it.dir("build-tools").dir(baseExtension.buildToolsVersion).file(
            if (java.lang.System.getProperty("os.name").startsWith("Windows")) {
                "aidl.exe"
            } else {
                "aidl"
            }
        )
    }

internal fun SdkComponents.aidlFramework(baseExtension: BaseExtension): Provider<RegularFile> =
    sdkDirectory.map {
        it.dir("platforms")
            .dir(baseExtension.compileSdkVersion!!)
            .file("framework.aidl")
    }

/**
 * Returns the AIDL import directories for the given variant of the project.
 */
internal fun Project.getAidlArtifactsOnCompileClasspath(variant: Variant): List<FileCollection> {
    val incoming = project.configurations.findByName("${variant.name}CompileClasspath")?.incoming
    val aidlFiles = incoming?.artifactView { config ->
            config.attributes(ArtifactType.AIDL)
        }?.artifacts?.artifactFiles
    val stableAidlFiles = incoming?.artifactView { config ->
            config.attributes(ArtifactType.STABLE_AIDL)
        }?.artifacts?.artifactFiles
    return listOfNotNull(aidlFiles, stableAidlFiles)
}

/**
 * When the Stable AIDL plugin is applies to the project, runs the specified [lambda] with access to
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
