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

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/** Finds the outputs of every task and saves this mapping into a file */
@CacheableTask
abstract class ListTaskOutputsTask : DefaultTask() {
    @OutputFile val outputFile: RegularFileProperty = project.objects.fileProperty()
    @Input val removePrefixes: MutableList<String> = mutableListOf()
    @get:Nested abstract val producers: ListProperty<TaskOutputProducer>

    init {
        group = "Help"
        project.gradle.taskGraph.whenReady {
            val taskOutputProducerList = mutableListOf<TaskOutputProducer>()
            project.allprojects { otherProject ->
                otherProject.tasks.forEach { task ->
                    project.objects.newInstance(TaskOutputProducer::class.java).apply {
                        taskPath.set(task.path)
                        taskClass.set(task::class.qualifiedName ?: task::class.java.name)
                        validate.set(shouldValidateTaskOutput(task))
                        val fileElements = task.outputs.files.elements
                        outputPaths.set(
                            fileElements.map { set ->
                                set.map { it.asFile.invariantSeparatorsPath }
                            }
                        )
                        taskOutputProducerList.add(this)
                    }
                }
            }
            producers.set(taskOutputProducerList)
        }
    }

    fun removePrefix(prefix: String) {
        removePrefixes.add("$prefix/")
    }

    @TaskAction
    fun exec() {
        val outputText = computeOutputText(producers.get())
        val outputFile = outputFile.get()
        outputFile.asFile.writeText(outputText)
    }

    private fun computeOutputText(producers: List<TaskOutputProducer>): String {
        val tasksByOutput: MutableMap<String, TaskOutputProducer> = hashMapOf()
        for (producer in producers) {
            for (path in producer.outputPaths.get()) {
                val existing = tasksByOutput[path]
                if (existing != null) {
                    if (existing.validate.get() && producer.validate.get()) {
                        throw GradleException(
                            "Output file $path was declared as an output of multiple tasks: " +
                                "${producer.taskPath.get()} and ${existing.taskPath.get()}"
                        )
                    }
                    if (existing.taskPath.get() > producer.taskPath.get()) continue
                }
                tasksByOutput[path] = producer
            }
        }
        return formatTasks(tasksByOutput, removePrefixes)
    }

    // Given a map from output file path to Task, formats into a String
    private fun formatTasks(
        tasksByOutput: MutableMap<String, TaskOutputProducer>,
        removePrefixes: List<String>,
    ): String {
        val messages: MutableList<String> = mutableListOf()
        for ((path, task) in tasksByOutput) {
            var filePath = path
            for (prefix in removePrefixes) {
                filePath = filePath.removePrefix(prefix)
            }

            messages.add(
                formatInColumns(
                    listOf(
                        filePath,
                        " - " + task.taskPath.get() + " (" + task.taskClass.get() + ")",
                    )
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
}

// TODO(149103692): remove all elements of this set
private val taskNamesKnownToDuplicateOutputs =
    setOf(
        // Instead of adding new elements to this set, prefer to disable unused tasks when possible

        // b/308798582
        "transformNonJvmMainCInteropDependenciesMetadataForIde",
        "transformAppleMainCInteropDependenciesMetadataForIde",
        "transformAppleTestCInteropDependenciesMetadataForIde",
        "transformDarwinTestCInteropDependenciesMetadataForIde",
        "transformDarwinMainCInteropDependenciesMetadataForIde",
        "transformCommonMainCInteropDependenciesMetadataForIde",
        "transformCommonTestCInteropDependenciesMetadataForIde",
        "transformIosMainCInteropDependenciesMetadataForIde",
        "transformIosTestCInteropDependenciesMetadataForIde",
        "transformNativeTestCInteropDependenciesMetadataForIde",
        "transformNativeMainCInteropDependenciesMetadataForIde",
        "transformUnixMainCInteropDependenciesMetadataForIde",
        "transformUnixTestCInteropDependenciesMetadataForIde",
        "transformLinuxMainCInteropDependenciesMetadataForIde",
        "transformLinuxTestCInteropDependenciesMetadataForIde",
        "transformNonIosNativeTestCInteropDependenciesMetadataForIde",
        "transformNonJvmCommonMainCInteropDependenciesMetadataForIde",

        // The following tests intentionally have the same output of golden images
        "updateGoldenDesktopTest",
        "updateGoldenDebugUnitTest",

        // The following tasks have the same output file:
        // ../../prebuilts/androidx/javascript-for-kotlin/yarn.lock
        "kotlinRestoreYarnLock",
        "kotlinWasmRestoreYarnLock",
        "kotlinNpmInstall",
        "kotlinWasmNpmInstall",
        "kotlinUpgradePackageLock",
        "kotlinWasmUpgradePackageLock",
        "kotlinUpgradeYarnLock",
        "kotlinWasmUpgradeYarnLock",
        "kotlinStorePackageLock",
        "kotlinWasmStorePackageLock",
        "kotlinStoreYarnLock",
        "kotlinWasmStoreYarnLock",

        // The following tasks have the same output file:
        // $OUT_DIR/androidx/build/wasm/yarn.lock
        "wasmKotlinRestoreYarnLock",
        "wasmKotlinNpmInstall",
        "wasmKotlinUpgradePackageLock",
        "wasmKotlinStorePackageLock",
        "wasmKotlinUpgradeYarnLock",
        "wasmKotlinStoreYarnLock",

        // The following tasks have the same output configFile file:
        // projectBuildDir/js/packages/projectName-wasm-js/webpack.config.js
        // Remove when https://youtrack.jetbrains.com/issue/KT-70029 / b/361319689 is resolved
        // and set configFile location for each task
        "wasmJsBrowserDevelopmentWebpack",
        "wasmJsBrowserDevelopmentRun",
        "wasmJsBrowserProductionWebpack",
        "wasmJsBrowserProductionRun",
        "jsTestTestDevelopmentExecutableCompileSync",

        // https://youtrack.jetbrains.com/issue/KT-79936
        // $OUT_DIR/.gradle/nodejs/node-v22.13.0-darwin-arm64.hash
        "kotlinNodeJsSetup",
        "kotlinWasmNodeJsSetup",
        // $OUT_DIR/.gradle/yarn/yarn-v1.22.17.hash
        "wasmKotlinYarnSetup",
        "kotlinYarnSetup",

        // $OUT_DIR/.gradle/binaryen/binaryen-version_122.hash
        "kotlinBinaryenSetup",
        "kotlinWasmBinaryenSetup",
    )

fun shouldValidateTaskOutput(task: Task): Boolean {
    if (!task.enabled) {
        return false
    }
    return !taskNamesKnownToDuplicateOutputs.contains(task.name)
}

/** Nested input describing each projects tasks and its outputs */
abstract class TaskOutputProducer {

    @get:Input abstract val taskPath: Property<String>

    @get:Input abstract val taskClass: Property<String>

    @get:Input abstract val validate: Property<Boolean>

    /**
     * A collection of output paths from various tasks.
     *
     * This property intentionally avoids using a [org.gradle.api.file.FileCollection] to prevent
     * creating a direct task dependency between the producer tasks and the [ListTaskOutputsTask].
     * By storing the paths as strings, we can inspect the output locations without coupling the
     * tasks in the execution graph.
     */
    @get:Input abstract val outputPaths: ListProperty<String>
}
