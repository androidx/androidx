/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.annotation.keep

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import com.android.build.api.variant.Variant
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.KotlinMultiplatformAndroidPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

internal const val GENERATED_KEEP_RULES = "androidx.annotation.keep.rules.pro"

@Suppress("unused") // Plugin class
class AnnotationKeepPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create(
            "annotationKeep",
            AnnotationKeepExtension::class.java,
            project,
        )
        project.plugins.configureEach { plugin ->
            when (plugin) {
                // Application
                is AppPlugin -> configureAndroidApplication(project)
                // Library
                is LibraryPlugin -> configureAndroidLibrary(project)
                // Kotlin Multiplatform Android Library
                is KotlinMultiplatformAndroidPlugin ->
                    configureKotlinMultiplatformAndroidLibrary(project)
            }
        }
    }

    private fun configureAndroidApplication(project: Project) {
        val androidComponents =
            project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)

        androidComponents.onVariants { variant ->
            // We don't really need to rewrite classes to remove annotations here.
            // This makes things a _lot_ easier and faster, given we don't have compatibility
            // concerns. Annotations are compile time only, so there is no runtime overhead as well.
            val keepRulesTask = registerExtractKeepRulesTask(project, variant)
            variant.proguardFiles.add(keepRulesTask.flatMap { it.outputKeepRules })
        }
    }

    private fun configureAndroidLibrary(project: Project) {
        val libraryComponents =
            project.extensions.getByType(LibraryAndroidComponentsExtension::class.java)
        libraryComponents.onVariants { variant ->
            val keepRulesTask = registerExtractKeepRulesTask(project, variant)
            variant.consumerProguardFiles.add(keepRulesTask.flatMap { it.outputKeepRules })
        }
    }

    private fun configureKotlinMultiplatformAndroidLibrary(project: Project) {
        val kmpComponents =
            project.extensions.getByType(KotlinMultiplatformAndroidComponentsExtension::class.java)
        kmpComponents.onVariants { variant ->
            val keepRulesTask = registerExtractKeepRulesTask(project, variant)
            variant.consumerProguardFiles.add(keepRulesTask.flatMap { it.outputKeepRules })
        }
    }

    private fun registerExtractKeepRulesTask(
        project: Project,
        variant: Variant,
    ): TaskProvider<ExtractKeepRulesTask> {
        val keepRulesTask =
            project.tasks.register(
                "${variant.name}ExtractKeepRules",
                ExtractKeepRulesTask::class.java,
            ) { task ->
                task.group = "Build"
                task.description =
                    "Extracts keep rules from annotations for the ${variant.name} variant."
                task.outputKeepRules.set(
                    project.layout.buildDirectory.file(
                        "generated/${variant.name}/$GENERATED_KEEP_RULES"
                    )
                )
            }

        variant.artifacts
            .forScope(ScopedArtifacts.Scope.PROJECT)
            .use(keepRulesTask)
            .toGet(
                ScopedArtifact.CLASSES,
                ExtractKeepRulesTask::inputJars,
                ExtractKeepRulesTask::inputDirectories,
            )

        return keepRulesTask
    }
}
