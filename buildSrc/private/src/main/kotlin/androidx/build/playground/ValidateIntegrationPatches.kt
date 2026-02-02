/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.build.playground

import androidx.build.addToBuildOnServer
import androidx.build.getSupportRootFolder
import androidx.build.uptodatedness.cacheEvenIfNoOutputs
import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

/** Validates that it is possible to apply the patch files in `.github/integration-patches`. */
@DisableCachingByDefault(because = "Patch applies to all files, any change could break it")
abstract class ValidateIntegrationPatches : DefaultTask() {
    @get:Inject abstract val execOperations: ExecOperations

    @get:[InputDirectory PathSensitive(PathSensitivity.NONE)]
    abstract val patchesDirectory: DirectoryProperty

    @TaskAction
    fun checkPatches() {
        val patchFiles = patchesDirectory.asFileTree.files
        for (patchFile in patchFiles) {
            // Only check patch files, skip the README.
            if (patchFile.extension == "patch") {
                val result =
                    execOperations.exec {
                        it.commandLine(
                            "git",
                            "apply",
                            // This option will see if the patch can be applied but not apply it.
                            "--check",
                            patchFile.absolutePath,
                        )
                        // Don't immediately error if the patch fails, to throw a custom error
                        // message.
                        it.isIgnoreExitValue = true
                    }
                if (result.exitValue != 0) {
                    throw GradleException(
                        "Failed to apply patch file ${patchFile.absolutePath}\n" +
                            "See the instructions in $PATCH_DIRECTORY/README.md to fix it."
                    )
                }
            }
        }
    }

    companion object {
        private const val PATCH_DIRECTORY = ".github/integration-patches"

        fun createTask(project: Project) {
            val task =
                project.tasks.register(
                    "validateIntegrationPatches",
                    ValidateIntegrationPatches::class.java,
                ) { task ->
                    task.patchesDirectory.set(File(project.getSupportRootFolder(), PATCH_DIRECTORY))
                    task.cacheEvenIfNoOutputs()
                }
            project.addToBuildOnServer(task)
        }
    }
}
