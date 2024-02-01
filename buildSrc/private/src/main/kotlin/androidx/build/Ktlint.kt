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

import androidx.build.logging.TERMINAL_RED
import androidx.build.logging.TERMINAL_RESET
import java.io.File
import java.nio.file.Paths
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations
import org.gradle.process.JavaExecSpec

val bundlingAttribute: Attribute<String> =
    Attribute.of("org.gradle.dependency.bundling", String::class.java)

/** JVM Args needed to run it on JVM 17+ */
private fun JavaExecSpec.addKtlintJvmArgs() {
    this.jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
}

private fun Project.getKtlintConfiguration(): ConfigurableFileCollection {
    return files(
        configurations.findByName("ktlint")
            ?: configurations.create("ktlint") {
                val version = getVersionByName("ktlint")
                val dependency = dependencies.create("com.pinterest:ktlint:$version")
                it.dependencies.add(dependency)
                it.attributes.attribute(bundlingAttribute, "external")
                it.isCanBeConsumed = false
            }
    )
}

private val DisabledRules =
    listOf(
            // TODO: reenable when https://github.com/pinterest/ktlint/issues/1221 is resolved
            "indent",
            // TODO: reenable when 'indent' is also enabled, meanwhile its to keep the status-quo
            //       see: https://github.com/pinterest/ktlint/releases/tag/0.45.0
            "wrapping",
            // Upgrade to 0.49.1 introduced new checks. TODO: fix and re-enable them.
            "trailing-comma-on-call-site",
            "trailing-comma-on-declaration-site",
            "argument-list-wrapping",
            "kdoc-wrapping",
            "comment-wrapping",
            "property-wrapping",
            "no-empty-first-line-in-method-block",
            "multiline-if-else",
            "annotation",
            "spacing-between-declarations-with-annotations",
            "spacing-between-declarations-with-comments",
            "spacing-around-angle-brackets",
            "annotation-spacing",
            "modifier-list-spacing",
            "double-colon-spacing",
            "fun-keyword-spacing",
            "function-return-type-spacing",
            "unary-op-spacing",
            "function-type-reference-spacing",
            "block-comment-initial-star-alignment",
            "package-name",
            "class-naming",
            "no-semi",
            "filename",
        )
        .joinToString(",")

private val ExcludedDirectories =
    listOf(
        "test-data",
        "external",
    )

private val ExcludedDirectoryGlobs = ExcludedDirectories.map { "**/$it/**/*.kt" }
private const val MainClass = "com.pinterest.ktlint.Main"
private const val InputDir = "src"
private const val IncludedFiles = "**/*.kt"

fun Project.configureKtlint() {
    val lintProvider =
        tasks.register("ktlint", KtlintCheckTask::class.java) { task ->
            task.report.set(layout.buildDirectory.file("reports/ktlint/report.xml"))
            task.ktlintClasspath.from(getKtlintConfiguration())
        }
    tasks.register("ktlintFormat", KtlintFormatTask::class.java) { task ->
        task.report.set(layout.buildDirectory.file("reports/ktlint/format-report.xml"))
        task.ktlintClasspath.from(getKtlintConfiguration())
    }
    // afterEvaluate because Gradle's default "check" task doesn't exist yet
    afterEvaluate {
        // multiplatform projects with no enabled platforms do not actually apply the kotlin plugin
        // and therefore do not have the check task. They are skipped unless a platform is enabled.
        if (project.tasks.findByName("check") != null) {
            addToCheckTask(lintProvider)
            addToBuildOnServer(lintProvider)
        }
    }
}

@CacheableTask
abstract class BaseKtlintTask : DefaultTask() {
    @get:Inject abstract val execOperations: ExecOperations

    @get:Classpath abstract val ktlintClasspath: ConfigurableFileCollection

    @get:Inject abstract val objects: ObjectFactory

    @[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    fun getInputFiles(): FileTree? {
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

    @get:OutputFile abstract val report: RegularFileProperty

    protected fun getArgsList(shouldFormat: Boolean): List<String> {
        val arguments = mutableListOf("--code-style=android_studio")
        arguments.add("--log-level=error")
        if (shouldFormat) arguments.add("-F")
        arguments.add("--disabled_rules")
        arguments.add(DisabledRules)
        arguments.add("--reporter=plain")
        arguments.add("--reporter=checkstyle,output=${report.get().asFile.absolutePath}")

        overrideDirectory?.let {
            val subdirectories = overrideSubdirectories
            if (subdirectories.isNullOrEmpty()) return@let
            subdirectories.map { arguments.add("$it/$InputDir/$IncludedFiles") }
        } ?: arguments.add("$InputDir/$IncludedFiles")

        ExcludedDirectoryGlobs.mapTo(arguments) { "!$InputDir/$it" }
        return arguments
    }
}

@CacheableTask
abstract class KtlintCheckTask : BaseKtlintTask() {
    init {
        description = "Check Kotlin code style."
        group = "Verification"
    }

    @get:Internal val projectPath: String = project.path

    @TaskAction
    fun runCheck() {
        val result =
            execOperations.javaexec { javaExecSpec ->
                javaExecSpec.mainClass.set(MainClass)
                javaExecSpec.classpath = ktlintClasspath
                javaExecSpec.args = getArgsList(shouldFormat = false)
                overrideDirectory?.let { javaExecSpec.workingDir = it }
                javaExecSpec.isIgnoreExitValue = true
            }
        if (result.exitValue != 0) {
            println(
                """

                ********************************************************************************
                ${TERMINAL_RED}You can attempt to automatically fix these issues with:
                ./gradlew $projectPath:ktlintFormat$TERMINAL_RESET
                ********************************************************************************
                """
                    .trimIndent()
            )
            result.assertNormalExitValue()
        }
    }
}

@CacheableTask
abstract class KtlintFormatTask : BaseKtlintTask() {
    init {
        description = "Fix Kotlin code style deviations."
        group = "formatting"
    }

    @TaskAction
    fun runFormat() {
        execOperations.javaexec { javaExecSpec ->
            javaExecSpec.mainClass.set(MainClass)
            javaExecSpec.classpath = ktlintClasspath
            javaExecSpec.args = getArgsList(shouldFormat = true)
            javaExecSpec.addKtlintJvmArgs()
            overrideDirectory?.let { javaExecSpec.workingDir = it }
        }
    }
}

@CacheableTask
abstract class KtlintCheckFileTask : DefaultTask() {
    init {
        description = "Check Kotlin code style."
        group = "Verification"
    }

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

    @get:Inject abstract val execOperations: ExecOperations

    @get:Classpath abstract val ktlintClasspath: ConfigurableFileCollection

    @TaskAction
    fun runKtlint() {
        if (files.isEmpty()) throw StopExecutionException()
        val kotlinFiles =
            files.filter { file ->
                val isKotlinFile = file.endsWith(".kt") || file.endsWith(".ktx")
                val inExcludedDir =
                    Paths.get(file).any { subPath ->
                        ExcludedDirectories.contains(subPath.toString())
                    }

                isKotlinFile && !inExcludedDir
            }
        if (kotlinFiles.isEmpty()) throw StopExecutionException()
        val result =
            execOperations.javaexec { javaExecSpec ->
                javaExecSpec.mainClass.set(MainClass)
                javaExecSpec.classpath = ktlintClasspath
                val args = mutableListOf(
                    "--code-style=android_studio",
                    "--disabled_rules",
                    DisabledRules
                )
                args.addAll(kotlinFiles)
                if (format) args.add("-F")

                javaExecSpec.args = args
                javaExecSpec.addKtlintJvmArgs()
                javaExecSpec.isIgnoreExitValue = true
            }
        if (result.exitValue != 0) {
            println(
                """

                ********************************************************************************
                ${TERMINAL_RED}You can attempt to automatically fix these issues with:
                ./gradlew :ktlintCheckFile --format ${kotlinFiles.joinToString(separator = " "){ "--file $it" }}$TERMINAL_RESET
                ********************************************************************************
                """
                    .trimIndent()
            )
            result.assertNormalExitValue()
        }
    }
}

fun Project.configureKtlintCheckFile() {
    tasks.register("ktlintCheckFile", KtlintCheckFileTask::class.java) { task ->
        task.ktlintClasspath.from(getKtlintConfiguration())
    }
}
