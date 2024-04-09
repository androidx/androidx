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

package androidx.room.gradle.integration

import androidx.room.gradle.RoomArgumentProvider
import androidx.room.gradle.RoomExtension
import androidx.room.gradle.RoomGradlePlugin.Companion.capitalize
import androidx.room.gradle.RoomGradlePlugin.Companion.check
import androidx.room.gradle.RoomGradlePlugin.Companion.findPair
import com.android.build.api.AndroidPluginVersion
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ComponentIdentity
import com.android.build.api.variant.HasAndroidTest
import com.google.devtools.ksp.gradle.KspTaskJvm
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.internal.KaptTask

internal class AndroidPluginIntegration(
    private val common: CommonIntegration
) {
    private val agpPluginIds = listOf(
        "com.android.application",
        "com.android.library",
        "com.android.dynamic-feature"
    )

    fun withAndroid(project: Project, roomExtension: RoomExtension) {
        agpPluginIds.forEach { agpPluginId ->
            project.plugins.withId(agpPluginId) {
                configureRoomForAndroid(project, roomExtension)
            }
        }
    }

    private fun configureRoomForAndroid(project: Project, roomExtension: RoomExtension) {
        // TODO(b/277899741): Validate version of Room supports the AP options configured by plugin.
        val componentsExtension =
            project.extensions.findByType(AndroidComponentsExtension::class.java)
        project.check(componentsExtension != null, isFatal = true) {
            "Could not find the Android Gradle Plugin (AGP) extension."
        }
        project.check(componentsExtension.pluginVersion >= AndroidPluginVersion(7, 3)) {
            "The Room Gradle plugin is only compatible with Android Gradle plugin (AGP) " +
                "version 7.3.0 or higher (found ${componentsExtension.pluginVersion})."
        }
        componentsExtension.onVariants { variant ->
            project.check(roomExtension.schemaDirectories.isNotEmpty(), isFatal = true) {
                "The Room Gradle plugin was applied but no schema location was specified. " +
                    "Use the `room { schemaDirectory(...) }` DSL to specify one."
            }
            configureAndroidVariant(project, roomExtension, variant)
            @Suppress("DEPRECATION")
            // TODO(b/328835662): Remove usage of deprecated API
            variant.unitTest?.let { configureAndroidVariant(project, roomExtension, it) }
            if (variant is HasAndroidTest) {
                variant.androidTest?.let { configureAndroidVariant(project, roomExtension, it) }
            }
        }
    }

    private fun configureAndroidVariant(
        project: Project,
        roomExtension: RoomExtension,
        variant: ComponentIdentity
    ) {
        val configureTask: (Task, ComponentIdentity) -> RoomArgumentProvider =
            { task, variantIdentity ->
                // Find schema location for variant from user declared location with priority:
                // * Full variant name specified, e.g. `schemaLocation("demoDebug", "...")`
                // * Flavor name, e.g. `schemaLocation("demo", "...")`
                // * Build type name, e.g. `schemaLocation("debug", "...")`
                // * All variants location, e.g. `schemaLocation("...")`
                // Due to Kotlin Multiplatform projects, user declared locations are also checked
                // with the 'android' prefix, i.e. `schemaLocation("androidDemo", "...")`,
                // `schemaLocation("androidDebug", "...")`, etc.
                val schemaDirectories = roomExtension.schemaDirectories
                val kmpPrefix = "android"
                val matchedPair = schemaDirectories.findPair(variantIdentity.name)
                    ?: schemaDirectories.findPair(kmpPrefix + variantIdentity.name.capitalize())
                    ?: variantIdentity.flavorName?.let {
                        schemaDirectories.findPair(it)
                            ?: schemaDirectories.findPair(kmpPrefix + it.capitalize())
                    }
                    ?: variantIdentity.buildType?.let {
                        schemaDirectories.findPair(it)
                            ?: schemaDirectories.findPair(kmpPrefix + it.capitalize())
                    }
                    ?: schemaDirectories.findPair(kmpPrefix)
                    ?: schemaDirectories.findPair(RoomExtension.ALL_MATCH.actual)
                project.check(matchedPair != null, isFatal = true) {
                    "No matching Room schema directory for Android variant " +
                        "'${variantIdentity.name}'."
                }
                val (matchedName, schemaDirectoryProvider) = matchedPair
                val schemaDirectory = schemaDirectoryProvider.get()
                project.check(schemaDirectory.isNotEmpty()) {
                    "The Room schema directory path for Android variant " +
                        "'${variantIdentity.name}' must not be empty."
                }
                common.configureTaskWithSchema(
                    project, roomExtension, matchedName, schemaDirectory, task
                )
            }
        val androidVariantTaskNames = AndroidVariantsTaskNames(variant.name, variant)
        configureJavaTasks(project, androidVariantTaskNames, configureTask)
        configureKaptTasks(project, androidVariantTaskNames, configureTask)
        configureKspTasks(project, androidVariantTaskNames, configureTask)

        // TODO: Consider also setting up the androidTest and test source set to include the
        //  relevant schema location so users can use MigrationTestHelper without additional
        //  configuration.
    }

    private fun configureJavaTasks(
        project: Project,
        androidVariantsTaskNames: AndroidVariantsTaskNames,
        configureBlock: (Task, ComponentIdentity) -> RoomArgumentProvider
    ) = project.tasks.withType(JavaCompile::class.java) { task ->
        androidVariantsTaskNames.withJavaCompile(task.name)?.let { variantIdentity ->
            val argProvider = configureBlock.invoke(task, variantIdentity)
            task.options.compilerArgumentProviders.add(argProvider)
        }
    }

    private fun configureKaptTasks(
        project: Project,
        androidVariantsTaskNames: AndroidVariantsTaskNames,
        configureBlock: (Task, ComponentIdentity) -> RoomArgumentProvider
    ) = project.plugins.withId("kotlin-kapt") {
        project.tasks.withType(KaptTask::class.java) { task ->
            androidVariantsTaskNames.withKaptTask(task.name)?.let { variantIdentity ->
                val argProvider = configureBlock.invoke(task, variantIdentity)
                // TODO: Update once KT-58009 is fixed.
                try {
                    // Because of KT-58009, we need to add a `listOf(argProvider)` instead
                    // of `argProvider`.
                    task.annotationProcessorOptionProviders.add(listOf(argProvider))
                } catch (e: Throwable) {
                    // Once KT-58009 is fixed, adding `listOf(argProvider)` will fail, we will
                    // pass `argProvider` instead, which is the correct way.
                    task.annotationProcessorOptionProviders.add(argProvider)
                }
            }
        }
    }

    private fun configureKspTasks(
        project: Project,
        androidVariantsTaskNames: AndroidVariantsTaskNames,
        configureBlock: (Task, ComponentIdentity) -> RoomArgumentProvider
    ) = project.plugins.withId("com.google.devtools.ksp") {
        project.tasks.withType(KspTaskJvm::class.java) { task ->
            androidVariantsTaskNames.withKspTaskJvm(task.name)?.let { variantIdentity ->
                val argProvider = configureBlock.invoke(task, variantIdentity)
                task.commandLineArgumentProviders.add(argProvider)
            }
        }
    }

    internal class AndroidVariantsTaskNames(
        private val variantName: String,
        private val variantIdentity: ComponentIdentity
    ) {
        private val javaCompileName by lazy {
            "compile${variantName.capitalize()}JavaWithJavac"
        }

        private val kaptTaskName by lazy {
            "kapt${variantName.capitalize()}Kotlin"
        }

        private val kspTaskJvmName by lazy {
            "ksp${variantName.capitalize()}Kotlin"
        }

        private val kspTaskAndroidName by lazy {
            "ksp${variantName.capitalize()}KotlinAndroid"
        }

        fun withJavaCompile(taskName: String) =
            if (taskName == javaCompileName) variantIdentity else null

        fun withKaptTask(taskName: String) =
            if (taskName == kaptTaskName) variantIdentity else null

        fun withKspTaskJvm(taskName: String) =
            if (taskName == kspTaskJvmName || taskName == kspTaskAndroidName) {
                variantIdentity
            } else {
                null
            }
    }
}
