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
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

fun Project.configureKtfmt() {
    tasks.register("ktFormat", KtfmtFormatTask::class.java) { task ->
        task.ktfmtClasspath.from(getKtfmtConfiguration())
        task.cacheEvenIfNoOutputs()
    }
    tasks.register("ktCheck", KtfmtCheckTask::class.java) { task ->
        task.ktfmtClasspath.from(getKtfmtConfiguration())
        task.cacheEvenIfNoOutputs()
    }
}

private val ExcludedDirectories = listOf(
    "test-data",
    "external",
)

private val ExcludedDirectoryGlobs = ExcludedDirectories.map { "**/$it/**/*.kt" }
private const val MainClass = "com.facebook.ktfmt.cli.Main"
private const val InputDir = "src"
private const val IncludedFiles = "**/*.kt"

private fun Project.getKtfmtConfiguration(): ConfigurableFileCollection {
    return files(
        configurations.findByName("ktfmt") ?: configurations.create("ktfmt") {
            val version = getVersionByName("ktfmt")
            val dependency = dependencies.create("com.facebook:ktfmt:$version")
            it.dependencies.add(dependency)
            it.attributes.attribute(bundlingAttribute, "external")
        }
    )
}

@CacheableTask
abstract class BaseKtfmtTask : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Classpath
    abstract val ktfmtClasspath: ConfigurableFileCollection

    @get:Inject
    abstract val objects: ObjectFactory

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
            subdirectories.forEach {
                include("$it/src/**/*.kt")
            }
        }
    }

    /**
     * Allows overriding to use a custom directory instead of default [Project.getProjectDir].
     */
    @get:Internal
    var overrideDirectory: File? = null

    /**
     * Used together with [overrideDirectory] to specify which specific subdirectories should
     * be analyzed.
     */
    @get:Internal
    var overrideSubdirectories: List<String>? = null

    protected fun getArgsList(dryRun: Boolean): List<String> {
        val arguments = mutableListOf("--kotlinlang-style")
        if (dryRun) arguments.add("--dry-run")
        arguments.addAll(getInputFiles().files.map { it.absolutePath })
        return arguments
    }
}

@CacheableTask
abstract class KtfmtFormatTask : BaseKtfmtTask() {
    init {
        description = "Fix Kotlin code style deviations."
        group = "formatting"
    }

    @TaskAction
    fun runFormat() {
        if (getInputFiles().files.isEmpty()) return
        execOperations.javaexec { javaExecSpec ->
            javaExecSpec.mainClass.set(MainClass)
            javaExecSpec.classpath = ktfmtClasspath
            javaExecSpec.args = getArgsList(dryRun = false)
            javaExecSpec.jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
            overrideDirectory?.let { javaExecSpec.workingDir = it }
        }
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
        if (getInputFiles().files.isEmpty()) return
        val outputStream = ByteArrayOutputStream()
        execOperations.javaexec { javaExecSpec ->
            javaExecSpec.standardOutput = outputStream
            javaExecSpec.mainClass.set(MainClass)
            javaExecSpec.classpath = ktfmtClasspath
            javaExecSpec.args = getArgsList(dryRun = true)
            javaExecSpec.jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
            overrideDirectory?.let { javaExecSpec.workingDir = it }
        }
        val output = outputStream.toString()
        if (output.isNotEmpty()) {
            throw Exception("Failed check for the following files:\n$output")
        }
    }
}