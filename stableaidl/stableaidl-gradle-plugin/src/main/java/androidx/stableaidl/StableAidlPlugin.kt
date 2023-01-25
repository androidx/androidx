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

import androidx.stableaidl.tasks.StableAidlCompile
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.DslExtension
import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.utils.usLocaleCapitalize
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

private const val PLUGIN_DIRNAME = "stable-aidl"
private const val GENERATED_PATH = "generated/source/$PLUGIN_DIRNAME"

@Suppress("unused", "UnstableApiUsage")
abstract class StableAidlPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
            ?: throw GradleException("Stable AIDL plugin requires Android Gradle Plugin")
        val base = project.extensions.getByType(BaseExtension::class.java)
            ?: throw GradleException("Stable AIDL plugin requires Android Gradle Plugin")

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

        val variantNameToGeneratingTask =
            mutableMapOf<String, Pair<TaskProvider<StableAidlCompile>, Provider<Directory>>>()

        androidComponents.onVariants { variant ->
            val sourceDir = variant.sources.getByName(SOURCE_TYPE_STABLE_AIDL)
            val importsDir = variant.sources.getByName(SOURCE_TYPE_STABLE_AIDL_IMPORTS)
            val outputDir = project.layout.buildDirectory.dir("$GENERATED_PATH/${variant.name}")
            val apiDirName = "$API_DIR/aidl${variant.name.usLocaleCapitalize()}"
            val builtApiDir = project.layout.buildDirectory.dir(apiDirName)
            val lastReleasedApiDir =
                project.layout.projectDirectory.dir("$apiDirName/$RELEASED_API_DIR")
            val lastCheckedInApiDir =
                project.layout.projectDirectory.dir("$apiDirName/$CURRENT_API_DIR")

            val compileAidlApiTask = registerCompileAidlApi(
                project,
                base,
                variant,
                sourceDir,
                importsDir,
                outputDir
            )
            val generateAidlApiTask = registerGenerateAidlApi(
                project,
                base,
                variant,
                sourceDir,
                importsDir,
                builtApiDir,
                compileAidlApiTask
            )
            val checkAidlApiReleaseTask = registerCheckApiAidlRelease(
                project,
                base,
                variant,
                importsDir,
                lastReleasedApiDir,
                generateAidlApiTask
            )
            registerCheckAidlApi(
                project,
                base,
                variant,
                importsDir,
                lastCheckedInApiDir,
                generateAidlApiTask,
                checkAidlApiReleaseTask
            )
            registerUpdateAidlApi(
                project,
                variant,
                lastCheckedInApiDir,
                generateAidlApiTask
            )

            variantNameToGeneratingTask[variant.name] = Pair(compileAidlApiTask, outputDir)
        }

        // AndroidComponentsExtension doesn't expose the APIs we need yet.
        base.onVariants { variant ->
            variantNameToGeneratingTask[variant.name]?.let { (compileAidlApiTask, outputDir) ->
                variant.registerJavaGeneratingTask(compileAidlApiTask, outputDir.get().asFile)
            }
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

@Suppress("DEPRECATION") // For BaseVariant should be replaced in later studio versions
internal fun BaseExtension.onVariants(
    action: (com.android.build.gradle.api.BaseVariant) -> Unit
) = when (this) {
    is AppExtension -> applicationVariants.all(action)
    is LibraryExtension -> libraryVariants.all(action)
    else -> throw GradleException(
        "androidx.stableaidl plugin must be used with Android app, library or feature plugin"
    )
}
