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
import androidx.navigation.safe.args.generator.generateSafeArgs
import com.android.build.gradle.internal.tasks.IncrementalTask
import com.android.ide.common.resources.FileStatus
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import java.io.File

private const val MAPPING_FILE = "file_mappings.json"

open class ArgumentsGenerationTask : IncrementalTask() {
    @get:Input
    lateinit var rFilePackage: String

    @get:Input
    lateinit var applicationId: String

    @get:OutputDirectory
    lateinit var outputDir: File

    @get:InputFiles
    var navigationFiles: List<File> = emptyList()

    private fun generateArgs(navFiles: Collection<File>, out: File) = navFiles.map { file ->
        val output = generateSafeArgs(rFilePackage, applicationId, file, out)
        Mapping(file.relativeTo(project.projectDir).path, output.files) to output.errors
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

    override fun doFullTaskAction() {
        if (outputDir.exists() && !outputDir.deleteRecursively()) {
            project.logger.warn("Failed to clear directory for navigation arguments")
        }
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw GradleException("Failed to create directory for navigation arguments")
        }
        val (mappings, errors) = generateArgs(navigationFiles, outputDir)
        writeMappings(mappings)
        failIfErrors(errors)
    }

    override fun doIncrementalTaskAction(changedInputs: MutableMap<File, FileStatus>) {
        super.doIncrementalTaskAction(changedInputs)
        val oldMapping = readMappings()
        val navFiles = changedInputs.filter { (_, status) -> status != FileStatus.REMOVED }.keys
        val (newMapping, errors) = generateArgs(navFiles, outputDir)
        val newJavaFiles = newMapping.flatMap { it.javaFiles }.toSet()
        val (modified, unmodified) = oldMapping.partition {
            File(project.projectDir, it.navFile) in changedInputs
        }
        modified.flatMap { it.javaFiles }
                .filter { name -> name !in newJavaFiles }
                .forEach { javaName ->
                    val fileName = "${javaName.replace('.', File.separatorChar)}.java"
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

    override fun isIncremental() = true
}

private fun ErrorMessage.toClickableText() = "$path:$line:$column: error: $message"

private data class Mapping(val navFile: String, val javaFiles: List<String>)
