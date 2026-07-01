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

import androidx.build.gitclient.getChangedFilesProvider
import kotlin.Suppress
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.extra
import org.gradle.work.DisableCachingByDefault

/**
 * Determines the affected projects based on changed files.
 *
 * This task is designed to run every time and identifies the projects impacted by changes in the
 * source files. It generates a list of Gradle task commands for the affected projects and writes
 * them to an output file.
 */
@DisableCachingByDefault(because = "The purpose of this task is to run each time")
abstract class ListAffectedProjectsTask : DefaultTask() {

    @get:Input abstract val changedFiles: ListProperty<String>

    @get:Input abstract val projectConsumersMap: MapProperty<String, Set<String>>

    @get:Input abstract val tasksToRun: ListProperty<String>

    @get:Input abstract val shouldRunOnDependentProjects: Property<Boolean>

    @get:OutputFile abstract val outputFile: RegularFileProperty

    @get:Internal
    val listProjectsServiceProvider: Provider<ListProjectsService> =
        ListProjectsService.registerOrGet(project)

    @Option(
        option = "baseCommit",
        description = "The base commit to compare changes against. Defaults to last merge commit.",
    )
    fun setBaseCommit(commit: String?) {
        changedFiles.set(project.getChangedFilesProvider(project.provider { commit }))
    }

    @Suppress("UNUSED")
    @Option(
        option = "tasksToRun",
        description = "Comma-separated list of tasks to run (e.g. 'bOS, allHostTests')",
    )
    fun setTasksRun(tasks: String) {
        tasksToRun.set(tasks.split(",").map(String::trim))
    }

    @Suppress("UNUSED")
    @Option(
        option = "runOnDependentProjects",
        description = "Boolean flag to also run tasks on dependent projects",
    )
    fun setRunOnDependentProjects(flag: String) {
        this.shouldRunOnDependentProjects.set(flag.toBoolean())
    }

    @TaskAction
    fun listAffectedProjects() {
        val changedFilesList = changedFiles.get()
        println("Changed files: $changedFilesList")
        val allProjects = listProjectsServiceProvider.get().allPossibleProjects
        val projectConsumers = projectConsumersMap.get()
        val tasks = tasksToRun.get()
        check(tasks.isNotEmpty()) { "tasksToRun cannot be empty" }

        val projectByFilePath = allProjects.associateBy({ it.filePath }, { it.gradlePath })

        val changedProjects =
            changedFilesList
                .mapNotNull { changedFile ->
                    when {
                        changedFile.startsWith("buildSrc/") -> ":buildSrc-tests"
                        "/src/" in changedFile -> {
                            val candidate = changedFile.substringBefore("/src/")
                            projectByFilePath[candidate]
                        }
                        else -> {
                            val sortedProjects =
                                allProjects
                                    .map { it.filePath to it.gradlePath }
                                    .sortedByDescending { it.first.length }
                            sortedProjects
                                .firstOrNull { (projectFilePath, _) ->
                                    changedFile.startsWith(projectFilePath)
                                }
                                ?.second
                        }
                    }
                }
                .toSet()

        val affectedProjects =
            if (shouldRunOnDependentProjects.get()) {
                changedProjects.flatMap { findAllProjectsDependingOn(it, projectConsumers) }.toSet()
            } else {
                changedProjects
            }

        val commands =
            affectedProjects
                // TODO(b/396611615): Remove when :docs-tip-of-tree can run bOS locally
                .filterNot { it == ":docs-tip-of-tree" }
                .flatMap { project -> tasks.map { task -> "$project:$task" } }

        with(outputFile.get().asFile) {
            parentFile.mkdirs()
            writeText(commands.joinToString(" "))
        }
    }
}

private fun findAllProjectsDependingOn(
    projectPath: String,
    projectConsumers: Map<String, Collection<String>>,
): Set<String> {
    val result = mutableSetOf<String>()
    val toBeTraversed = ArrayDeque<String>().apply { add(projectPath) }

    while (toBeTraversed.isNotEmpty()) {
        val path = toBeTraversed.removeFirst()
        if (result.add(path)) {
            projectConsumers[path]?.let { dependents -> toBeTraversed.addAll(dependents) }
        }
    }
    return result
}

internal fun Project.registerListAffectedProjectsTask() =
    tasks.register("listAffectedProjects", ListAffectedProjectsTask::class.java) { task ->
        task.tasksToRun.convention(listOf("bOS"))
        task.shouldRunOnDependentProjects.convention(false)
        task.setBaseCommit(null)

        @Suppress("UNCHECKED_CAST")
        task.projectConsumersMap.set(
            (gradle.extra["allProjectConsumers"] as Map<String, Set<String>>)
        )

        task.outputFile.set(layout.buildDirectory.file("changedProjects.txt"))

        // Always run task
        task.outputs.upToDateWhen { false }
    }
