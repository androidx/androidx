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
import androidx.room.gradle.RoomGradlePlugin.Companion.isKspTask
import androidx.room.gradle.RoomSchemaCopyTask
import androidx.room.gradle.toOptions
import kotlin.io.path.Path
import kotlin.io.path.notExists
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory

internal class CommonIntegration(
    private val projectLayout: ProjectLayout,
    private val objectFactory: ObjectFactory,
) {
    fun configureTaskWithSchema(
        project: Project,
        roomExtension: RoomExtension,
        matchName: RoomExtension.MatchName,
        schemaDirectory: String,
        task: Task
    ): RoomArgumentProvider {
        val schemaDirectoryPath = Path(schemaDirectory)
        if (schemaDirectoryPath.notExists()) {
            project.check(schemaDirectoryPath.toFile().mkdirs()) {
                "Unable to create directory: $schemaDirectoryPath"
            }
        }

        val schemaInputDir = objectFactory.directoryProperty().apply {
            set(project.file(schemaDirectoryPath))
        }
        val schemaOutputDir =
            projectLayout.buildDirectory.dir("intermediates/room/schemas/${task.name}")

        val copyTask = roomExtension.copyTasks.getOrPut(matchName) {
            project.tasks.register(
                "copyRoomSchemas${matchName.actual.capitalize()}",
                RoomSchemaCopyTask::class.java
            ) {
                it.schemaDirectory.set(schemaInputDir)
            }
        }
        copyTask.configure { it.variantSchemaOutputDirectories.from(schemaOutputDir) }
        task.finalizedBy(copyTask)

        return RoomArgumentProvider(
            forKsp = task.isKspTask(),
            schemaInputDir = schemaInputDir,
            schemaOutputDir = schemaOutputDir,
            options = roomExtension.toOptions()
        )
    }
}
