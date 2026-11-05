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

package androidx.build.docs

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Task which verifies that Dackka has not logged any new warnings. */
@CacheableTask
internal abstract class ValidateDocsWarningsTask : DefaultTask() {
    /** Path of `build_log_simplifier.py`, which is used to verify the warning output. */
    @get:[InputFile PathSensitive(PathSensitivity.NONE)]
    abstract val buildLogSimplifierScript: RegularFileProperty

    /** File with logging output from the `docs` task of this project. */
    @get:[InputFile PathSensitive(PathSensitivity.NONE)]
    abstract val loggingFile: RegularFileProperty

    /** Baseline file containing known warnings for the project. */
    @get:[InputFile PathSensitive(PathSensitivity.NONE)]
    abstract val baselineFile: RegularFileProperty

    /**
     * Optional baseline to use in addition to [baselineFile].
     *
     * For `docs-public`, the `docs-tip-of-tree` baseline is used as an additional baseline so that
     * warnings do not need to be suppressed a second time when a library is released.
     */
    @get:[Optional InputFile PathSensitive(PathSensitivity.NONE)]
    abstract val additionalBaselineFile: RegularFileProperty

    /**
     * Build ID for a CI build, or "0" for a local build. Only used in the error message if this
     * task fails.
     */
    @get:Internal abstract val buildId: Property<String>

    @TaskAction
    fun validate() {
        val args = buildList {
            add(buildLogSimplifierScript.get().asFile.absolutePath)
            add(loggingFile.get().asFile.absolutePath)
            add("--override_exemptions_path")
            add(baselineFile.get().asFile.absolutePath)
            // Use the additional baseline when provided
            if (additionalBaselineFile.isPresent) {
                add("--override_exemptions_flake_path")
                add(additionalBaselineFile.get().asFile.absolutePath)
            }
            add("--validate")
            // Do not print the usual instructions so that custom instructions can be used instead
            add("--exclude_failure_instructions")
        }

        // Redirect output in case there are so many new warnings that they would overload the
        // default stdout buffer.
        val tmpOutput = File(temporaryDir, "out.tmp")
        val process = ProcessBuilder(args).redirectOutput(tmpOutput).start()

        val code = process.waitFor()
        if (code != 0) {
            val buildId = buildId.get()
            val buildIdArg = if (buildId == "0") "" else " --build-id $buildId"
            val errorMessage =
                """
                There are new docs warnings. These warnings should be fixed in source if possible.

                If it is impossible to fix the warnings, run this command to suppress them:
                `development/referenceDocs/updateWarningsFiles.sh$buildIdArg`
                """
                    .trimIndent()
            throw GradleException(errorMessage + "\n" + tmpOutput.readText())
        }
    }
}
