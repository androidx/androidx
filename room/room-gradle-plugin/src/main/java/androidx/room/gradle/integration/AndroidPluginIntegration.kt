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
import androidx.room.gradle.RoomExtension.SchemaConfiguration
import androidx.room.gradle.RoomGradlePlugin.Companion.capitalize
import androidx.room.gradle.RoomGradlePlugin.Companion.check
import androidx.room.gradle.RoomSimpleCopyTask
import androidx.room.gradle.toOptions
import com.android.build.api.AndroidPluginVersion
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ComponentIdentity
import com.android.build.api.variant.HasUnitTest
import com.google.devtools.ksp.gradle.KspTaskJvm
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.internal.KaptTask

internal class AndroidPluginIntegration(private val common: CommonIntegration) {
    private val agpBasePluginId = "com.android.base"

    // Map of variant name to schema configuration
    private val configuredVariants = mutableMapOf<String, SchemaConfiguration>()

    fun withAndroid(project: Project, roomExtension: RoomExtension) {
        project.plugins.withId(agpBasePluginId) { configureRoomForAndroid(project, roomExtension) }
    }

    private fun configureRoomForAndroid(project: Project, roomExtension: RoomExtension) {
        // TODO(b/277899741): Validate version of Room supports the AP options configured by plugin.
        val componentsExtension =
            project.extensions.findByType(AndroidComponentsExtension::class.java)
        project.check(componentsExtension != null, isFatal = true) {
            "Could not find the Android Gradle Plugin (AGP) extension."
        }
        project.check(componentsExtension.pluginVersion >= AndroidPluginVersion(8, 1)) {
            "The Room Gradle plugin is only compatible with Android Gradle plugin (AGP) " +
                "version 8.1.0 or higher (found ${componentsExtension.pluginVersion})."
        }
        componentsExtension.onVariants { variant ->
            configureAndroidVariant(project, roomExtension, variant)
            (variant as? HasUnitTest)?.unitTest?.let {
                configureAndroidVariant(project, roomExtension, it)
            }
            @Suppress("DEPRECATION") // usage of HasAndroidTest
            (variant as? com.android.build.api.variant.HasAndroidTest)?.androidTest?.let {
                configureAndroidVariant(project, roomExtension, it)
            }
        }
        project.afterEvaluate {
            project.check(roomExtension.schemaConfigurations.isNotEmpty(), isFatal = true) {
                "The Room Gradle plugin was applied but no schema location was specified. " +
                    "Use the `room { schemaDirectory(...) }` DSL to specify one."
            }
        }
    }

    private fun configureAndroidVariant(
        project: Project,
        roomExtension: RoomExtension,
        variant: ComponentIdentity
    ) {
        val androidVariantTaskNames = AndroidVariantsTaskNames(variant.name)

        forSchemaConfiguration(roomExtension, variant) { newConfig ->
            val oldConfig = configuredVariants.put(variant.name, newConfig)
            common.configureSchemaCopyTask(androidVariantTaskNames.taskNames, oldConfig, newConfig)
        }

        val argProviderFactory: (Task) -> RoomArgumentProvider = factory@{ apTask ->
            val config = configuredVariants[variant.name]
            project.check(config != null, isFatal = true) {
                "No matching Room schema directory for Android variant '${variant.name}'."
            }

            apTask.finalizedBy(config.copyTask)
            common.createArgumentProvider(
                schemaConfiguration = config,
                roomOptions = roomExtension.toOptions(),
                task = apTask
            )
        }
        configureJavaTasks(project, androidVariantTaskNames, argProviderFactory)
        configureKaptTasks(project, androidVariantTaskNames, argProviderFactory)
        configureKspTasks(project, androidVariantTaskNames, argProviderFactory)

        // Wires a task that will copy schemas from user configured location to the AGP
        // generated directory to be used as assets inputs of an Android Test app, enabling
        // MigrationTestHelper to automatically pick them up.
        @Suppress("DEPRECATION") // Usage of AndroidTest
        if (variant is com.android.build.api.variant.AndroidTest) {
            variant.sources.assets?.addGeneratedSourceDirectory(
                project.tasks.register(
                    "copyRoomSchemasToAndroidTestAssets${variant.name.capitalize()}",
                    RoomSimpleCopyTask::class.java
                )
            ) {
                val config = configuredVariants[variant.name]
                project.check(config != null, isFatal = true) {
                    "No matching Room schema directory for Android variant '${variant.name}'."
                }
                it.inputDirectory.set(config.copyTask.flatMap { it.schemaDirectory })
                // Return the directory property AGP will set for the task to copy the schemas to
                // so that they are included as assets of the Android test app.
                return@addGeneratedSourceDirectory it.outputDirectory
            }
        }
    }

    /**
     * Call [block] when schema config for variant matches, with the following priority:
     * * Full variant name specified, e.g. `schemaLocation("demoDebug", "...")`
     * * Flavor name, e.g. `schemaLocation("demo", "...")`
     * * Build type name, e.g. `schemaLocation("debug", "...")`
     * * All variants location, e.g. `schemaLocation("...")`
     *
     * Due to Kotlin Multiplatform projects, user declared locations are also checked with the
     * 'android' prefix, i.e. `schemaLocation("androidDemo", "...")`,
     * `schemaLocation("androidDebug", "...")`, etc.
     */
    private fun forSchemaConfiguration(
        roomExtension: RoomExtension,
        variant: ComponentIdentity,
        block: (SchemaConfiguration) -> Unit
    ) {
        var currentPriority = Int.MAX_VALUE
        roomExtension.schemaConfigurations.configureEach { config ->
            val kmpPrefix = "android"
            val newPriority =
                when {
                    config.matches(variant.name) -> 0
                    config.matches(kmpPrefix + variant.name.capitalize()) -> 1
                    config.matches(variant.flavorName) -> 2
                    config.matches(variant.flavorName?.let { (kmpPrefix + it.capitalize()) }) -> 3
                    config.matches(variant.buildType) -> 4
                    config.matches(variant.buildType?.let { (kmpPrefix + it.capitalize()) }) -> 5
                    config.matches(kmpPrefix) -> 6
                    config.matches(RoomExtension.ALL_MATCH) -> 7
                    else -> return@configureEach
                }
            if (currentPriority < newPriority) {
                return@configureEach
            }
            currentPriority = newPriority

            block.invoke(config)
        }
    }

    private fun configureJavaTasks(
        project: Project,
        androidVariantsTaskNames: AndroidVariantsTaskNames,
        argumentProviderFactory: (Task) -> RoomArgumentProvider
    ) =
        project.tasks.withType(JavaCompile::class.java).configureEach { task ->
            if (androidVariantsTaskNames.isJavaCompile(task.name)) {
                val argProvider = argumentProviderFactory.invoke(task)
                task.options.compilerArgumentProviders.add(argProvider)
            }
        }

    private fun configureKaptTasks(
        project: Project,
        androidVariantsTaskNames: AndroidVariantsTaskNames,
        argumentProviderFactory: (Task) -> RoomArgumentProvider
    ) =
        project.plugins.withId("kotlin-kapt") {
            project.tasks.withType(KaptTask::class.java).configureEach { task ->
                if (androidVariantsTaskNames.isKaptTask(task.name)) {
                    val argProvider = argumentProviderFactory.invoke(task)
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
        argumentProviderFactory: (Task) -> RoomArgumentProvider
    ) =
        project.plugins.withId("com.google.devtools.ksp") {
            project.tasks.withType(KspTaskJvm::class.java).configureEach { task ->
                if (androidVariantsTaskNames.isKspTaskJvm(task.name)) {
                    val argProvider = argumentProviderFactory.invoke(task)
                    task.commandLineArgumentProviders.add(argProvider)
                }
            }
        }

    private class AndroidVariantsTaskNames(variantName: String) {
        private val javaCompileName = "compile${variantName.capitalize()}JavaWithJavac"

        private val kaptTaskName = "kapt${variantName.capitalize()}Kotlin"

        private val kspTaskJvmName = "ksp${variantName.capitalize()}Kotlin"

        private val kspTaskAndroidName = "ksp${variantName.capitalize()}KotlinAndroid"

        val taskNames = setOf(javaCompileName, kaptTaskName, kspTaskJvmName, kspTaskAndroidName)

        fun isJavaCompile(taskName: String) = taskName == javaCompileName

        fun isKaptTask(taskName: String) = taskName == kaptTaskName

        fun isKspTaskJvm(taskName: String) =
            taskName == kspTaskJvmName || taskName == kspTaskAndroidName
    }
}
