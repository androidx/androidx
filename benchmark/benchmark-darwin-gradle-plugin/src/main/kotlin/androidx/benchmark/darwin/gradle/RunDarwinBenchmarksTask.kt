/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.benchmark.darwin.gradle

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

@CacheableTask
abstract class RunDarwinBenchmarksTask
@Inject
constructor(private val execOperations: ExecOperations) : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val xcodeProjectPath: DirectoryProperty

    @get:Input abstract val destination: Property<String>

    @get:Input abstract val scheme: Property<String>

    @get:OutputDirectory abstract val xcResultPath: DirectoryProperty

    @TaskAction
    fun runBenchmarks() {
        requireXcodeBuild()
        // Consider moving this into the shared instance of XCodeBuildService
        // given that is a much cleaner way of sharing a single instance of a running simulator.
        val simCtrl = XCodeSimCtrl(execOperations, destination.get())
        val xcodeProject = xcodeProjectPath.get().asFile
        val xcResultFile = xcResultPath.get().asFile
        if (xcResultFile.exists()) {
            xcResultFile.deleteRecursively()
        }
        simCtrl.start { destinationDesc ->
            val args =
                listOf(
                    "xcodebuild",
                    "test",
                    "-project",
                    xcodeProject.absolutePath.toString(),
                    "-scheme",
                    scheme.get(),
                    "-destination",
                    destinationDesc,
                    "-resultBundlePath",
                    xcResultFile.absolutePath,
                )
            logger.info("Command : ${args.joinToString(" ")}")
            execOperations.executeQuietly(args)
        }
    }

    private fun requireXcodeBuild() {
        val result =
            execOperations.exec { spec ->
                spec.commandLine = listOf("which", "xcodebuild")
                // Ignore exit value here to return a better exception message
                spec.isIgnoreExitValue = true
            }
        require(result.exitValue == 0) { "xcodebuild is missing on this machine." }
    }
}
