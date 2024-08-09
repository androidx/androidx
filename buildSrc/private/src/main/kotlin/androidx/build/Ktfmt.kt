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

import androidx.build.logging.TERMINAL_RED
import androidx.build.logging.TERMINAL_RESET
import androidx.build.uptodatedness.cacheEvenIfNoOutputs
import com.facebook.ktfmt.format.Formatter
import com.facebook.ktfmt.format.Formatter.format
import java.io.File
import java.nio.file.Paths
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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun Project.configureKtfmt() {
    tasks.register("ktFormat", KtfmtFormatTask::class.java)

    val ktCheckTask =
        tasks.register("ktCheck", KtfmtCheckTask::class.java) { task ->
            task.cacheEvenIfNoOutputs()
            // Workaround for https://github.com/gradle/gradle/issues/29205
            // Our ktfmt tasks declare "src" as an input, while our KotlinCompile tasks use
            // something like src/main/java as an input
            // Currently Gradle can sometimes get confused when loading a parent and child directory
            // at the same time, so we ask Gradle to avoid running both tasks in parallel
            task.mustRunAfter(project.tasks.withType(KotlinCompile::class.java))
        }

    // afterEvaluate because Gradle's default "check" task doesn't exist yet
    afterEvaluate {
        // multiplatform projects with no enabled platforms do not actually apply the kotlin plugin
        // and therefore do not have the check task. They are skipped unless a platform is enabled.
        if (tasks.findByName("check") != null) {
            addToCheckTask(ktCheckTask)
            addToBuildOnServer(ktCheckTask)
        }
    }
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

    @get:Internal val projectPath: String = project.path

    @[InputFiles PathSensitive(PathSensitivity.RELATIVE) SkipWhenEmpty]
    open fun getInputFiles(): FileTree {
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
                            incorrectlyFormatted.map { it.input }.joinToString("\n") +
                            """

                ********************************************************************************
                You can attempt to automatically fix these issues with:
                ./gradlew $projectPath:ktFormat
                ********************************************************************************
                """
                                .trimIndent()
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
        val originCode = input.readText()
        val formattedCode = format(Formatter.KOTLINLANG_FORMAT, originCode)
        return KtfmtResult(
            input = input,
            isCorrectlyFormatted = originCode == formattedCode,
            formattedCode = formattedCode
        )
    }
}

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

@CacheableTask
abstract class KtfmtCheckFileTask : BaseKtfmtTask() {
    init {
        description = "Check Kotlin code style."
        group = "Verification"
    }

    @get:Internal val projectDir = project.projectDir

    @get:Input
    @set:Option(
        option = "file",
        description =
            "File to check. This option can be used multiple times: --file file1.kt " +
                "--file file2.kt"
    )
    var files: List<String> = emptyList()

    @get:Input
    @set:Option(
        option = "format",
        description =
            "Use --format to auto-correct style violations (if some errors cannot be " +
                "fixed automatically they will be printed to stderr)"
    )
    var format = false

    override fun getInputFiles(): FileTree {
        if (files.isEmpty()) {
            return objects.fileTree().setDir(projectDir).apply { exclude("**") }
        }
        val kotlinFiles =
            files
                .filter { file ->
                    val isKotlinFile = file.endsWith(".kt") || file.endsWith(".ktx")
                    val inExcludedDir =
                        Paths.get(file).any { subPath ->
                            ExcludedDirectories.contains(subPath.toString())
                        }

                    isKotlinFile && !inExcludedDir
                }
                .map { it.replace("./", "**/") }

        if (kotlinFiles.isEmpty()) {
            return objects.fileTree().setDir(projectDir).apply { exclude("**") }
        }
        return objects.fileTree().setDir(projectDir).apply { include(kotlinFiles) }
    }

    @TaskAction
    fun runCheck() {
        try {
            runKtfmt(format = format)
        } catch (e: IllegalStateException) {
            val kotlinFiles =
                files.filter { file ->
                    val isKotlinFile = file.endsWith(".kt") || file.endsWith(".ktx")
                    val inExcludedDir =
                        Paths.get(file).any { subPath ->
                            ExcludedDirectories.contains(subPath.toString())
                        }

                    isKotlinFile && !inExcludedDir
                }
            error(
                """

                ********************************************************************************
                ${TERMINAL_RED}You can attempt to automatically fix these issues with:
                ./gradlew :ktCheckFile --format ${kotlinFiles.joinToString(separator = " "){ "--file $it" }}$TERMINAL_RESET
                ********************************************************************************
                """
                    .trimIndent()
            )
        }
    }
}

fun Project.configureKtfmtCheckFile() {
    tasks.register("ktCheckFile", KtfmtCheckFileTask::class.java)
}
