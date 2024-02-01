/*
 * Copyright 2019 The Android Open Source Project
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

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/** Finds the outputs of every task and saves this mapping into a file */
@CacheableTask
abstract class ListTaskOutputsTask : DefaultTask() {
    @OutputFile val outputFile: Property<File> = project.objects.property(File::class.java)
    @Input val removePrefixes: MutableList<String> = mutableListOf()
    @Input val tasks: MutableList<Task> = mutableListOf()

    @get:Input val outputText by lazy { computeOutputText() }

    init {
        group = "Help"
        // compute the output text when the taskgraph is ready so that the output text can be
        // saved in the configuration cache and not generate a configuration cache violation
        project.gradle.taskGraph.whenReady { outputText }
    }

    fun setOutput(f: File) {
        outputFile.set(f)
        description = "Finds the outputs of every task and saves the resulting mapping into $f"
    }

    fun removePrefix(prefix: String) {
        removePrefixes.add("$prefix/")
    }

    // Given a map from output file to Task, formats into a String
    private fun formatTasks(tasksByOutput: Map<File, Task>): String {
        val messages: MutableList<String> = mutableListOf()
        for ((output, task) in tasksByOutput) {
            var filePath = output.path
            for (prefix in removePrefixes) {
                filePath = filePath.removePrefix(prefix)
            }

            messages.add(
                formatInColumns(
                    listOf(filePath, " - " + task.path + " (" + task::class.qualifiedName + ")")
                )
            )
        }
        messages.sort()
        return messages.joinToString("\n")
    }

    // Given a list of columns, indents and joins them to be easy to read
    private fun formatInColumns(columns: List<String>): String {
        val components = mutableListOf<String>()
        var textLength = 0
        for (column in columns) {
            val roundedTextLength =
                if (textLength == 0) {
                    textLength
                } else {
                    ((textLength / 32) + 1) * 32
                }
            val extraSpaces = " ".repeat(roundedTextLength - textLength)
            components.add(extraSpaces)
            textLength = roundedTextLength
            components.add(column)
            textLength += column.length
        }
        return components.joinToString("")
    }

    fun computeOutputText(): String {
        val tasksByOutput = project.rootProject.findAllTasksByOutput()
        return formatTasks(tasksByOutput)
    }

    @TaskAction
    fun exec() {
        val outputFile = outputFile.get()
        outputFile.writeText(outputText)
    }
}

// TODO(149103692): remove all elements of this set
val taskNamesKnownToDuplicateOutputs =
    setOf(
        // Instead of adding new elements to this set, prefer to disable unused tasks when possible

        // b/308798582
        "transformNonJvmMainCInteropDependenciesMetadataForIde",
        "transformDarwinTestCInteropDependenciesMetadataForIde",
        "transformDarwinMainCInteropDependenciesMetadataForIde",
        "transformCommonMainCInteropDependenciesMetadataForIde",
        "transformCommonTestCInteropDependenciesMetadataForIde",
        "transformIosMainCInteropDependenciesMetadataForIde",
        "transformIosTestCInteropDependenciesMetadataForIde",
        "transformNativeTestCInteropDependenciesMetadataForIde",
        "transformNativeMainCInteropDependenciesMetadataForIde",

        // The following tests intentionally have the same output of golden images
        "updateGoldenDesktopTest",
        "updateGoldenDebugUnitTest"
    )

fun shouldValidateTaskOutput(task: Task): Boolean {
    if (!task.enabled) {
        return false
    }
    return !taskNamesKnownToDuplicateOutputs.contains(task.name)
}

// For this project and all subprojects, collects all tasks and creates a map keyed by their output
// files
fun Project.findAllTasksByOutput(): Map<File, Task> {
    // find list of all tasks
    val allTasks = mutableListOf<Task>()
    project.allprojects { otherProject ->
        otherProject.tasks.forEach { task -> allTasks.add(task) }
    }

    // group tasks by their outputs
    val tasksByOutput: MutableMap<File, Task> = hashMapOf()
    for (otherTask in allTasks) {
        for (otherTaskOutput in otherTask.outputs.files.files) {
            val existingTask = tasksByOutput[otherTaskOutput]
            if (existingTask != null) {
                if (shouldValidateTaskOutput(existingTask) && shouldValidateTaskOutput(otherTask)) {
                    throw GradleException(
                        "Output file " +
                            otherTaskOutput +
                            " was declared as an output of " +
                            "multiple tasks: " +
                            otherTask +
                            " and " +
                            existingTask
                    )
                }
                // if there is an exempt conflict, keep the alphabetically earlier task to ensure
                // consistency
                if (existingTask.path > otherTask.path) continue
            }
            tasksByOutput[otherTaskOutput] = otherTask
        }
    }
    return tasksByOutput
}
