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
import androidx.room.gradle.RoomGradlePlugin.Companion.check
import androidx.room.gradle.toOptions
import com.google.devtools.ksp.gradle.KspTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

internal class KotlinMultiplatformPluginIntegration(private val common: CommonIntegration) {

    private val kgpPluginIds =
        listOf(
            "org.jetbrains.kotlin.jvm",
            "org.jetbrains.kotlin.android",
            "org.jetbrains.kotlin.multiplatform"
        )

    // Map of variant name to schema configuration
    private val configuredTargets = mutableMapOf<String, SchemaConfiguration>()

    fun withKotlin(project: Project, roomExtension: RoomExtension) {
        kgpPluginIds.forEach { kgpPluginId ->
            project.plugins.withId(kgpPluginId) {
                val kotlin = project.extensions.getByName("kotlin") as KotlinProjectExtension
                when (kotlin) {
                    is KotlinSingleTargetExtension<*> ->
                        configureRoomForKotlin(project, roomExtension, kotlin.target)
                    is KotlinMultiplatformExtension ->
                        kotlin.targets.configureEach {
                            configureRoomForKotlin(project, roomExtension, it)
                        }
                }
            }
        }
    }

    private fun configureRoomForKotlin(
        project: Project,
        roomExtension: RoomExtension,
        target: KotlinTarget
    ) {
        // Android KSP tasks are configured through the Android Gradle Plugin variant APIs.
        if (target.platformType == KotlinPlatformType.androidJvm) return

        forSchemaConfiguration(roomExtension, target) { newConfig ->
            val oldConfig = configuredTargets.put(target.name, newConfig)
            target.compilations.configureEach { kotlinCompilation ->
                val kotlinCompilationTaskNames = KotlinCompilationTaskNames(kotlinCompilation)
                common.configureSchemaCopyTask(
                    kotlinCompilationTaskNames.taskNames,
                    oldConfig,
                    newConfig
                )
            }
        }

        target.compilations.configureEach { kotlinCompilation ->
            val kotlinCompilationTaskNames = KotlinCompilationTaskNames(kotlinCompilation)
            val argProviderFactory: (Task) -> RoomArgumentProvider = { apTask ->
                val config = configuredTargets[target.name]
                project.check(config != null, isFatal = true) {
                    "No matching Room schema directory for the KSP target '${target.targetName}'."
                }

                apTask.finalizedBy(config.copyTask)
                common.createArgumentProvider(
                    schemaConfiguration = config,
                    roomOptions = roomExtension.toOptions(),
                    task = apTask
                )
            }
            configureKspTasks(project, kotlinCompilationTaskNames, argProviderFactory)
        }
    }

    /**
     * Call [block] when schema config for target matches, with the following priority:
     * * Target name specified, e.g. `schemaLocation("linuxX64", "...")`
     * * All targets location, e.g. `schemaLocation("...")`
     */
    private fun forSchemaConfiguration(
        roomExtension: RoomExtension,
        target: KotlinTarget,
        block: (SchemaConfiguration) -> Unit
    ) {
        var currentPriority = Int.MAX_VALUE
        roomExtension.schemaConfigurations.configureEach { config ->
            val newPriority =
                when {
                    config.matches(target.targetName) -> 0
                    config.matches(RoomExtension.ALL_MATCH) -> 1
                    else -> return@configureEach
                }
            if (currentPriority < newPriority) {
                return@configureEach
            }
            currentPriority = newPriority

            block.invoke(config)
        }
    }

    private fun configureKspTasks(
        project: Project,
        kotlinCompilationTaskNames: KotlinCompilationTaskNames,
        argumentProviderFactory: (Task) -> RoomArgumentProvider
    ) =
        project.plugins.withId("com.google.devtools.ksp") {
            project.tasks.withType(KspTask::class.java).configureEach { task ->
                if (kotlinCompilationTaskNames.isKspTask(task.name)) {
                    val argProvider = argumentProviderFactory.invoke(task)
                    task.commandLineArgumentProviders.add(argProvider)
                }
            }
        }

    private class KotlinCompilationTaskNames(kotlinCompilation: KotlinCompilation<*>) {
        // Using same naming strategy as KSP, based off Kotlin compile task.
        // TODO(https://github.com/google/ksp/issues/2083): Use proper API when available.
        private val kspTaskName = kotlinCompilation.compileKotlinTaskName.replace("compile", "ksp")

        val taskNames = setOf(kspTaskName)

        fun isKspTask(taskName: String) = taskName == kspTaskName
    }
}
