/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.build.VerifyRelocatedDependenciesTask.Companion.ALLOWED_CONFIGURATIONS
import androidx.build.uptodatedness.cacheEvenIfNoOutputs
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

/** Ensures specified libraries are always relocated/jarjarred */
@CacheableTask
abstract class VerifyRelocatedDependenciesTask : DefaultTask() {

    @get:Input abstract val allDependencies: ListProperty<Pair<String, List<String>>>

    @Internal val projectPath: String = project.path

    @Internal val librariesToCheck: List<String> = listOf("protobuf-javalite", "protobuf-java")

    @TaskAction
    fun check() {
        if (projectPath == ":benchmark:benchmark-baseline-profile-gradle-plugin") {
            return
        }
        val violations =
            allDependencies.get().filter { (_, artifacts) ->
                librariesToCheck.any { artifacts.contains(it) }
            }

        if (violations.isNotEmpty()) {
            val message = buildString {
                appendLine("The following configurations contain disallowed dependencies:")
                violations.forEach { (configurationName, artifacts) ->
                    appendLine("Configuration: $configurationName")
                    artifacts.forEach { artifact ->
                        if (librariesToCheck.contains(artifact)) {
                            appendLine("  - $artifact")
                        }
                    }
                }
                appendLine(
                    "Publishing $projectPath is not allowed until the above dependencies are " +
                        "relocated. Consider using the AndroidXRepackagePlugin."
                )
            }
            throw GradleException(message)
        }
    }

    internal companion object {
        const val TASK_NAME = "verifyRelocatedDependencies"
        val ALLOWED_CONFIGURATIONS = listOf("compileOnly", "repackage")
    }
}

internal fun Project.registerValidateRelocatedDependenciesTask() =
    tasks
        .register(
            VerifyRelocatedDependenciesTask.TASK_NAME,
            VerifyRelocatedDependenciesTask::class.java,
        ) {
            val depsProvider: Provider<List<Pair<String, List<String>>>> =
                project.providers.provider {
                    project.configurations
                        .filter { configuration ->
                            configuration.isPublished() &&
                                !configuration.isCanBeResolved &&
                                configuration.name !in ALLOWED_CONFIGURATIONS
                        }
                        .map { configuration ->
                            configuration.name to
                                configuration.allDependencies.map { dependency -> dependency.name }
                        }
                }
            it.allDependencies.set(depsProvider)
            it.cacheEvenIfNoOutputs()
        }
        .also { addToBuildOnServer(it) }
