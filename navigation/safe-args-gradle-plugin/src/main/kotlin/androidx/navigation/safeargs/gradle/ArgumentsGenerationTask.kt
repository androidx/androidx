/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.navigation.safeargs.gradle

import androidx.navigation.safe.args.generator.ErrorMessage
import androidx.navigation.safe.args.generator.SafeArgsGenerator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Provider
import org.gradle.api.resources.TextResource
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import java.io.File

private const val MAPPING_FILE = "file_mappings.json"

open class ArgumentsGenerationTask : DefaultTask() {
    @get:Input
    lateinit var rFilePackage: Provider<String>

    var applicationIdResource: TextResource? = null // null on AGP 3.2.1 and below

    var applicationId: String? = null // null on AGP 3.3.0 and above

    @get:Input
    var useAndroidX: Boolean = true

    @get:Input
    var generateKotlin: Boolean = false

    @get:OutputDirectory
    lateinit var outputDir: File

    @get:InputFiles
    lateinit var navigationFiles: Provider<List<File>>

    @get:OutputDirectory
    lateinit var incrementalFolder: File

    /**
     * Gets the app id from either the [applicationIdResource] if available or [applicationId].
     * The availability from which the app id string is retrieved from is based on the Android
     * Gradle Plugin version of the project.
     */
    @Input
    fun getApplicationIdResourceString() = applicationIdResource?.asString() ?: applicationId

    private fun generateArgs(navFiles: Collection<File>, out: File) = navFiles.map { file ->
        val output = SafeArgsGenerator(
            rFilePackage = rFilePackage.get(),
            applicationId = getApplicationIdResourceString() ?: "",
            navigationXml = file,
            outputDir = out,
            useAndroidX = useAndroidX,
            generateKotlin = generateKotlin).generate()
        Mapping(file.relativeTo(project.projectDir).path, output.fileNames) to output.errors
    }.unzip().let { (mappings, errorLists) -> mappings to errorLists.flatten() }

    private fun writeMappings(mappings: List<Mapping>) {
        File(incrementalFolder, MAPPING_FILE).writer().use { Gson().toJson(mappings, it) }
    }

    private fun readMappings(): List<Mapping> {
        val type = object : TypeToken<List<Mapping>>() {}.type
        val mappingsFile = File(incrementalFolder, MAPPING_FILE)
        if (mappingsFile.exists()) {
            return mappingsFile.reader().use { Gson().fromJson(it, type) }
        } else {
            return emptyList()
        }
    }

    @TaskAction
    internal fun taskAction(inputs: IncrementalTaskInputs) {
        if (inputs.isIncremental) {
            doIncrementalTaskAction(inputs)
        } else {
            project.logger.info("Unable do incremental execution: full task run")
            doFullTaskAction()
        }
    }

    private fun doFullTaskAction() {
        if (outputDir.exists() && !outputDir.deleteRecursively()) {
            project.logger.warn("Failed to clear directory for navigation arguments")
        }
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw GradleException("Failed to create directory for navigation arguments")
        }
        val (mappings, errors) = generateArgs(navigationFiles.get(), outputDir)
        writeMappings(mappings)
        failIfErrors(errors)
    }

    private fun doIncrementalTaskAction(inputs: IncrementalTaskInputs) {
        val modifiedFiles = mutableSetOf<File>()
        val removedFiles = mutableSetOf<File>()
        inputs.outOfDate { change -> modifiedFiles.add(change.file) }
        inputs.removed { change -> removedFiles.add(change.file) }

        val oldMapping = readMappings()
        val (newMapping, errors) = generateArgs(modifiedFiles, outputDir)
        val newJavaFiles = newMapping.flatMap { it.javaFiles }.toSet()
        val changedInputs = removedFiles + modifiedFiles
        val (modified, unmodified) = oldMapping.partition {
            File(project.projectDir, it.navFile) in changedInputs
        }
        modified.flatMap { it.javaFiles }
                .filter { name -> name !in newJavaFiles }
                .forEach { javaName ->
                    val fileExtension = if (generateKotlin) { ".kt" } else { ".java" }
                    val fileName =
                        "${javaName.replace('.', File.separatorChar)}$fileExtension"
                    val file = File(outputDir, fileName)
                    if (file.exists()) {
                        file.delete()
                    }
                }
        writeMappings(unmodified + newMapping)
        failIfErrors(errors)
    }

    private fun failIfErrors(errors: List<ErrorMessage>) {
        if (errors.isNotEmpty()) {
            val errString = errors.joinToString("\n") { it.toClickableText() }
            throw GradleException(
                    "androidx.navigation.safeargs plugin failed.\n " +
                            "Following errors found: \n$errString")
        }
    }
}

private fun ErrorMessage.toClickableText() = "$path:$line:$column " +
        "(${File(path).name}:$line): \n" +
        "error: $message"

private data class Mapping(val navFile: String, val javaFiles: List<String>)
