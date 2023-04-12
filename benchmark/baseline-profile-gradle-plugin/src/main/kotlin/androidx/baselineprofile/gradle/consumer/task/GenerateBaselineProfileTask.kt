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

package androidx.baselineprofile.gradle.consumer.task

import androidx.baselineprofile.gradle.utils.TASK_NAME_SUFFIX
import androidx.baselineprofile.gradle.utils.maybeRegister
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault

private const val GENERATE_TASK_NAME = "generate"

/**
 * Creates the `generate<variant>BaselineProfile` task. Note that this task does nothing on its
 * own and it's only to start the generation process.
 */
internal inline fun <reified T : Task> maybeCreateGenerateTask(
    project: Project,
    variantName: String,
    lastTaskProvider: TaskProvider<*>? = null
) = project.tasks.maybeRegister<T>(GENERATE_TASK_NAME, variantName, TASK_NAME_SUFFIX) {
    it.group = "Baseline Profile"
    it.description = "Generates a baseline profile for the specified variants or dimensions."
    if (lastTaskProvider != null) it.dependsOn(lastTaskProvider)
}

@DisableCachingByDefault(because = "Not worth caching.")
abstract class MainGenerateBaselineProfileTask : DefaultTask() {

    init {
        group = "Baseline Profile"
        description = "Generates a baseline profile"
    }

    @TaskAction
    fun exec() {
        this.logger.warn(
            """
                The task `generateBaselineProfile` cannot currently support
                generation for all the variants when there are multiple build
                types without improvements planned for a future version of the
                Android Gradle Plugin.
                Until then, `generateBaselineProfile` will only generate
                baseline profiles for the variants of the release build type,
                behaving like `generateReleaseBaselineProfile`.
                If you intend to generate profiles for multiple build types
                you'll need to run separate gradle commands for each build type.
                For example: `generateReleaseBaselineProfile` and
                `generateAnotherReleaseBaselineProfile`.

                Details on https://issuetracker.google.com/issue?id=270433400.
                """.trimIndent()
        )
    }
}
