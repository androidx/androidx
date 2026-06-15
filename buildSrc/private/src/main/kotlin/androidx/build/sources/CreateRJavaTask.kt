/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.build.sources

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.Aapt2
import com.android.build.api.variant.AndroidComponents
import com.android.build.api.variant.LibraryVariant
import java.io.File
import kotlin.collections.plus
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.incremental.deleteDirectoryContents

/** Task which uses AAPT2 to generate R.java */
@CacheableTask
abstract class CreateRJavaTask : DefaultTask() {
    /** Location of aapt2 executable file. */
    @get:Nested abstract val aapt2: Property<Aapt2>

    /** Directories containing Android resource files for the project. */
    @get:[InputFiles PathSensitive(PathSensitivity.NONE)]
    abstract val resources: ConfigurableFileCollection

    /** The project's Android manifest. */
    @get:[InputFile PathSensitive(PathSensitivity.NONE)]
    abstract val manifest: RegularFileProperty

    /** Directory which will contain the output `R.java` file. */
    @get:OutputDirectory abstract val generatedR: DirectoryProperty

    @TaskAction
    fun generate() {
        // Clear any old files, so deleted resources don't stay in the docs.
        generatedR.asFile.get().deleteDirectoryContents()

        val resourceDirs = resources.files.filter { it.exists() }
        // Skip the task when there are no resources or manifest.
        if (resourceDirs.isNotEmpty() && manifest.get().asFile.exists()) {
            val compileDir = File(temporaryDir, "compile")
            compile(compileDir, resourceDirs)
            link(compileDir.listFiles()?.toList() ?: emptyList())
        }
    }

    /** Runs the AAPT2 compile step to convert XML files to .flat files. */
    private fun compile(compileDir: File, resourceDirs: List<File>) {
        compileDir.mkdirs()
        val args =
            resourceDirs.flatMap { listOf("--dir", it.absolutePath) } +
                listOf("-o", compileDir.absolutePath)
        runAapt2("compile", args)
    }

    /** Runs the AAPT2 link step to convert .flat files and */
    private fun link(compiledResourcesFiles: List<File>) {
        val args =
            compiledResourcesFiles.map { it.absolutePath } +
                listOf(
                    "--java",
                    generatedR.asFile.get().absolutePath,
                    // AAPT2 also generates other outputs.
                    "-o",
                    File(temporaryDir, "link").absolutePath,
                    "--manifest",
                    manifest.get().asFile.absolutePath,
                    // These arguments skip verifying resource references.
                    "--static-lib",
                    "--merge-only",
                )
        runAapt2("link", args)
    }

    /** Runs the AAPT2 [command] with [args], throwing an exception if it fails. */
    private fun runAapt2(command: String, args: List<String>) {
        val aapt2 = aapt2.get().executable.get().asFile.absolutePath
        val proc = ProcessBuilder(aapt2, command, *args.toTypedArray()).start()
        proc.waitFor()
        if (proc.exitValue() != 0) {
            throw GradleException(
                "$command step finished with exit code ${proc.exitValue()}\n" +
                    proc.errorReader().lines().toList().joinToString("\n")
            )
        }
    }

    companion object {
        /** Sets up a [CreateRJavaTask] for the [project], returning the output directory. */
        fun setupTask(
            project: Project,
            libraryVariant: LibraryVariant,
            components: AndroidComponents,
        ): FileCollection {
            val task =
                project.tasks.register("generateRJava", CreateRJavaTask::class.java) { task ->
                    task.aapt2.set(components.sdkComponents.aapt2)
                    task.resources.from(libraryVariant.sources.res?.static)
                    task.generatedR.set(project.layout.buildDirectory.dir("aapt2"))
                    task.manifest.set(libraryVariant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
                }
            return project.files(task)
        }
    }
}
