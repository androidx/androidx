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
import androidx.room.gradle.RoomExtension.SchemaConfiguration
import androidx.room.gradle.RoomGradlePlugin.Companion.isKspTask
import androidx.room.gradle.RoomOptions
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

internal class CommonIntegration(
    private val projectLayout: ProjectLayout,
    private val providerFactory: ProviderFactory,
) {
    // Schema copy task name to annotation processing task names
    private val copyTaskToApTaskNames = mutableMapOf<String, MutableSet<String>>()

    // Annotation processing task name to `room.internal.schemaOutput` directory.
    private val apTaskSchemaOutputDirs = mutableMapOf<String, Provider<Directory>>()

    /**
     * Wires a new matching schema config copy task to have as inputs all possible annotation
     * processing task outputs that might generate schemas. Old schema matching schema config being
     * replaced (due to priority) is also
     */
    fun configureSchemaCopyTask(
        apTaskNames: Set<String>,
        oldSchemaConfig: SchemaConfiguration?,
        newSchemaConfig: SchemaConfiguration
    ) {
        // If a schema config is being replaced, unlink ap generating tasks from it.
        if (oldSchemaConfig != null) {
            copyTaskToApTaskNames
                .getOrPut(oldSchemaConfig.copyTask.name) { mutableSetOf() }
                .removeAll(apTaskNames)
        }

        // Add ap generating tasks to new schema config.
        copyTaskToApTaskNames
            .getOrPut(newSchemaConfig.copyTask.name) { mutableSetOf() }
            .addAll(apTaskNames)

        newSchemaConfig.copyTask.configure { copyTask ->
            // Reset the output directories as there might be various configure() lambdas registered
            // during the matching process.
            copyTask.apTaskSchemaOutputDirectories.empty()
            // Using the up-to-date copy task to AP task names directories, link ap task outputs
            // with copy task inputs.
            copyTaskToApTaskNames
                .getValue(copyTask.name)
                .map { apTaskName ->
                    apTaskSchemaOutputDirs.getOrPut(apTaskName) {
                        projectLayout.buildDirectory.dir("intermediates/room/schemas/$apTaskName")
                    }
                }
                .forEach { copyTask.apTaskSchemaOutputDirectories.add(it) }
        }
    }

    /**
     * Creates the argument provider for an annotation processing task, that will be used to wire
     * the copy task inputs / outputs.
     */
    fun createArgumentProvider(
        schemaConfiguration: SchemaConfiguration,
        roomOptions: RoomOptions,
        task: Task
    ): RoomArgumentProvider {
        return RoomArgumentProvider(
            forKsp = task.isKspTask(),
            schemaInputDir = schemaConfiguration.copyTask.flatMap { it.schemaDirectory },
            schemaOutputDir =
                providerFactory.provider { apTaskSchemaOutputDirs.getValue(task.name).get() },
            options = roomOptions
        )
    }
}
