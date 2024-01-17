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

import androidx.build.uptodatedness.cacheEvenIfNoOutputs
import com.facebook.ktfmt.format.Formatter
import com.facebook.ktfmt.format.Formatter.format
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.intellij.lang.annotations.Language

fun Project.configureKtfmt() {
    tasks.register("ktFormat", KtfmtFormatTask::class.java)
    tasks.register("ktCheck", KtfmtCheckTask::class.java) { task -> task.cacheEvenIfNoOutputs() }
}

private val ExcludedDirectories =
    listOf(
        "test-data",
        "external",
    )

private val ExcludedDirectoryGlobs = ExcludedDirectories.map { "**/$it/**/*.kt" }
private const val InputDir = "src"
private const val IncludedFiles = "**/*.kt"

@CacheableTask
abstract class BaseKtfmtTask : DefaultTask() {
    @get:Inject abstract val objects: ObjectFactory

    @[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    fun getInputFiles(): FileTree {
        val projectDirectory = overrideDirectory
        val subdirectories = overrideSubdirectories
        if (projectDirectory == null || subdirectories.isNullOrEmpty()) {
            // If we have a valid override, use that as the default fileTree
            return objects.fileTree().setDir(InputDir).apply {
                include(IncludedFiles)
                exclude(ExcludedDirectoryGlobs)
            }
        }
        return objects.fileTree().setDir(projectDirectory).apply {
            subdirectories.forEach { include("$it/src/**/*.kt") }
        }
    }

    /** Allows overriding to use a custom directory instead of default [Project.getProjectDir]. */
    @get:Internal var overrideDirectory: File? = null

    /**
     * Used together with [overrideDirectory] to specify which specific subdirectories should be
     * analyzed.
     */
    @get:Internal var overrideSubdirectories: List<String>? = null

    protected fun runKtfmt(format: Boolean) {
        if (getInputFiles().files.isEmpty()) return
        runBlocking(Dispatchers.IO) {
            val result = processInputFiles()
            val incorrectlyFormatted = result.filter { !it.isCorrectlyFormatted }
            if (incorrectlyFormatted.isNotEmpty()) {
                if (format) {
                    incorrectlyFormatted.forEach { it.input.writeText(it.formattedCode) }
                } else {
                    error(
                        "Found ${incorrectlyFormatted.size} files that are not correctly " +
                            "formatted:\n" +
                            incorrectlyFormatted.map { it.input }.joinToString("\n")
                    )
                }
            }
        }
    }

    /** Run ktfmt on all the files in [getInputFiles] in parallel. */
    private suspend fun processInputFiles(): List<KtfmtResult> {
        return coroutineScope { getInputFiles().files.map { async { processFile(it) } }.awaitAll() }
    }

    /** Run ktfmt on the [input] file. */
    private fun processFile(input: File): KtfmtResult {
        // To hack around https://github.com/facebook/ktfmt/issues/406 we rewrite all the
        // @sample tags to @sample so that ktfmt would not move them around. We then
        // rewrite it back when returning the formatted code.
        val originCode = input.readText().replace(SAMPLE, PLACEHOLDER)
        val formattedCode = format(Formatter.KOTLINLANG_FORMAT, originCode)
        return KtfmtResult(
            input = input,
            isCorrectlyFormatted = originCode == formattedCode,
            formattedCode = formattedCode.replace(PLACEHOLDER, SAMPLE)
        )
    }
}

// Keep two of them the same length to make sure line wrapping works as expected
private const val SAMPLE = "@sample"
private const val PLACEHOLDER = "@sample"

internal data class KtfmtResult(
    val input: File,
    val isCorrectlyFormatted: Boolean,
    @Language("kotlin") val formattedCode: String,
)

@CacheableTask
abstract class KtfmtFormatTask : BaseKtfmtTask() {
    init {
        description = "Fix Kotlin code style deviations."
        group = "formatting"
    }

    // Format task rewrites inputs, so the outputs are the same as inputs.
    @OutputFiles fun getRewrittenFiles(): FileTree = getInputFiles()

    @TaskAction
    fun runFormat() {
        runKtfmt(format = true)
    }
}

@CacheableTask
abstract class KtfmtCheckTask : BaseKtfmtTask() {
    init {
        description = "Check Kotlin code style."
        group = "Verification"
    }

    @TaskAction
    fun runCheck() {
        runKtfmt(format = false)
    }
}
