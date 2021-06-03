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
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import java.io.File
import javax.inject.Inject

private const val MAPPING_FILE = "file_mappings.json"

abstract class ArgumentsGenerationTask @Inject constructor(
    private val projectLayout: ProjectLayout
) : DefaultTask() {
    @get:Input
    abstract val rFilePackage: Property<String>

    @get:Input
    abstract val applicationId: Property<String>

    @get:Input
    var useAndroidX: Boolean = true

    @get:Input
    var generateKotlin: Boolean = false

    @get:OutputDirectory
    lateinit var outputDir: File

    @get:Incremental
    @get:InputFiles
    lateinit var navigationFiles: FileCollection

    @get:OutputDirectory
    lateinit var incrementalFolder: File

    private fun generateArgs(navFiles: Collection<File>, out: File) = navFiles.map { file ->
        val output = SafeArgsGenerator(
            rFilePackage = rFilePackage.get(),
            applicationId = applicationId.orNull ?: "",
            navigationXml = file,
            outputDir = out,
            useAndroidX = useAndroidX,
            generateKotlin = generateKotlin
        ).generate()
        Mapping(
            file.relativeTo(
                projectLayout.projectDirectory.asFile
            ).path,
            output.fileNames
        ) to output.errors
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
    internal fun taskAction(inputs: InputChanges) {
        if (inputs.isIncremental) {
            doIncrementalTaskAction(inputs)
        } else {
            logger.info("Unable do incremental execution: full task run")
            doFullTaskAction()
        }
    }

    private fun doFullTaskAction() {
        if (outputDir.exists() && !outputDir.deleteRecursively()) {
            logger.warn("Failed to clear directory for navigation arguments")
        }
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw GradleException("Failed to create directory for navigation arguments")
        }
        val (mappings, errors) = generateArgs(navigationFiles.files, outputDir)
        writeMappings(mappings)
        failIfErrors(errors)
    }

    private fun doIncrementalTaskAction(inputs: InputChanges) {
        val modifiedFiles = mutableSetOf<File>()
        val removedFiles = mutableSetOf<File>()
        inputs.getFileChanges(navigationFiles).forEach { change ->
            if (change.changeType == ChangeType.MODIFIED || change.changeType == ChangeType.ADDED) {
                modifiedFiles.add(change.file)
            } else if (change.changeType == ChangeType.REMOVED) {
                removedFiles.add(change.file)
            }
        }

        val oldMapping = readMappings()
        val (newMapping, errors) = generateArgs(modifiedFiles, outputDir)
        val newJavaFiles = newMapping.flatMap { it.javaFiles }.toSet()
        val changedInputs = removedFiles + modifiedFiles
        val (modified, unmodified) = oldMapping.partition {
            File(projectLayout.projectDirectory.asFile, it.navFile) in changedInputs
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
                    "Following errors found: \n$errString"
            )
        }
    }
}

private fun ErrorMessage.toClickableText() = "$path:$line:$column " +
    "(${File(path).name}:$line): \n" +
    "error: $message"

private data class Mapping(val navFile: String, val javaFiles: List<String>)
