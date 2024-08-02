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
import androidx.room.gradle.RoomExtension.Companion.findPair
import androidx.room.gradle.RoomGradlePlugin.Companion.check
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

        val configureTask: (Task) -> RoomArgumentProvider = { task ->
            val (matchedName, schemaDirectory) = findSchemaDirectory(project, roomExtension, target)
            common.configureTaskWithSchema(
                project,
                roomExtension,
                matchedName,
                schemaDirectory,
                task
            )
        }
        target.compilations.configureEach { kotlinCompilation ->
            configureKspTasks(project, kotlinCompilation, configureTask)
        }
    }

    private fun findSchemaDirectory(
        project: Project,
        roomExtension: RoomExtension,
        target: KotlinTarget
    ): Pair<RoomExtension.MatchName, String> {
        val schemaDirectories = roomExtension.schemaDirectories
        val matchedPair =
            schemaDirectories.findPair(target.targetName)
                ?: schemaDirectories.findPair(RoomExtension.ALL_MATCH.actual)
        project.check(matchedPair != null, isFatal = true) {
            "No matching Room schema directory for the KSP target '${target.targetName}'."
        }
        val (matchedName, schemaDirectoryProvider) = matchedPair
        val schemaDirectory = schemaDirectoryProvider.get()
        project.check(schemaDirectory.isNotEmpty()) {
            "The Room schema directory path for the KSP target '${target.targetName}' must " +
                "not be empty."
        }
        return matchedName to schemaDirectory
    }

    private fun configureKspTasks(
        project: Project,
        kotlinCompilation: KotlinCompilation<*>,
        configureBlock: (Task) -> RoomArgumentProvider
    ) =
        project.plugins.withId("com.google.devtools.ksp") {
            project.tasks.withType(KspTask::class.java) { task ->
                // Same naming strategy as KSP, based off Kotlin compile task.
                // https://github.com/google/ksp/blob/main/gradle-plugin/src/main/kotlin/com/google/devtools/ksp/gradle/KspAATask.kt#L151
                val kspTaskName = kotlinCompilation.compileKotlinTaskName.replace("compile", "ksp")
                if (task.name == kspTaskName) {
                    val argProvider = configureBlock.invoke(task)
                    task.commandLineArgumentProviders.add(argProvider)
                }
            }
        }
}
