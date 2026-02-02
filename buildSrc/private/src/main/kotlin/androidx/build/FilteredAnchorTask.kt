/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.build

import androidx.build.FilteredAnchorTask.Companion.GLOBAL_TASK_NAME
import androidx.build.FilteredAnchorTask.Companion.PROP_PATH_PREFIX
import androidx.build.FilteredAnchorTask.Companion.PROP_TASK_NAME
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "This is an anchor task that does no work.")
abstract class FilteredAnchorTask : DefaultTask() {
    init {
        group = "Help"
        description =
            "Runs tasks with a name specified by -P$PROP_TASK_NAME= for projects with " +
                "a path prefix specified by -P$PROP_PATH_PREFIX="
    }

    @get:Input abstract var pathPrefix: String

    @get:Input abstract var taskName: String

    @TaskAction
    fun exec() {
        if (dependsOn.isEmpty()) {
            throw GradleException(
                "Failed to find any filterable tasks with name \"$taskName\" " +
                    "and path prefixed with \"$pathPrefix\""
            )
        }
    }

    companion object {
        const val GLOBAL_TASK_NAME = "filterTasks"
        const val PROP_PATH_PREFIX = "androidx.pathPrefix"
        const val PROP_TASK_NAME = "androidx.taskName"
    }
}

/**
 * Offers the specified [taskProviders] to the global [FilteredAnchorTask], adding them if they
 * match the requested path prefix and task name.
 */
internal fun Project.addFilterableTasks(vararg taskProviders: TaskProvider<*>?) {
    if (
        providers.gradleProperty(PROP_PATH_PREFIX).isPresent &&
            providers.gradleProperty(PROP_TASK_NAME).isPresent
    ) {
        val pathPrefixes = (properties[PROP_PATH_PREFIX] as String).split(",")
        if (pathPrefixes.any { pathPrefix -> relativePathForFiltering().startsWith(pathPrefix) }) {
            val taskName = properties[PROP_TASK_NAME] as String
            taskProviders
                .find { taskProvider -> taskName == taskProvider?.name }
                ?.let { taskProvider ->
                    rootProject.tasks.named(GLOBAL_TASK_NAME).configure { task ->
                        task.dependsOn(taskProvider)
                    }
                }
        }
    }
}

/**
 * Registers the global [FilteredAnchorTask] if the required command-line properties are set.
 *
 * For example, to run `checkApi` for all projects under `core/core/`: ./gradlew filterTasks
 * -Pandroidx.taskName=checkApi -Pandroidx.pathPrefix=core/core/
 */
internal fun Project.maybeRegisterFilterableTask() {
    if (
        providers.gradleProperty(PROP_TASK_NAME).isPresent &&
            providers.gradleProperty(PROP_PATH_PREFIX).isPresent
    ) {
        tasks.register(GLOBAL_TASK_NAME, FilteredAnchorTask::class.java) { task ->
            task.pathPrefix = properties[PROP_PATH_PREFIX] as String
            task.taskName = properties[PROP_TASK_NAME] as String
        }
    }
}

/**
 * Returns an AndroidX-relative path for the [Project], inserting the root project directory when
 * run in a Playground context such that paths are consistent with the AndroidX context.
 */
internal fun Project.relativePathForFiltering(): String =
    if (ProjectLayoutType.isPlayground(project)) {
        "${projectDir.relativeTo(getSupportRootFolder())}/"
    } else {
        "${projectDir.relativeTo(rootDir)}/"
    }
