/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.build.license

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/** This task validates that all external dependencies have a license file. */
@DisableCachingByDefault(because = "I/O heavy operation")
abstract class ValidateLicensesExistTask : DefaultTask() {
    @get:[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    abstract val prebuiltsDirectory: DirectoryProperty

    @get:[InputFile PathSensitive(PathSensitivity.NONE)]
    abstract val baseline: RegularFileProperty

    @TaskAction
    fun validate() {
        val baselineFile = baseline.get().asFile
        val baseline =
            if (baselineFile.exists()) {
                baselineFile.readLines().toSet()
            } else setOf()

        val violations = mutableSetOf<String>()
        prebuiltsDirectory
            .get()
            .asFile
            .walkTopDown()
            .onEnter { !File(it, "LICENSE").exists() && !File(it, "NOTICE").exists() }
            .forEach {
                if (it.extension == "pom") {
                    violations.add(it.relativeTo(prebuiltsDirectory.get().asFile).toString())
                }
            }
        val nonBaselinedViolations = (violations - baseline).sorted()

        if (nonBaselinedViolations.isNotEmpty())
            throw GradleException(
                """
            Any external library referenced used by androidx
            build must have a LICENSE or NOTICE file next to it in the prebuilts.
            The following libraries are missing it:
            ${nonBaselinedViolations.joinToString("\n")}
        """
                    .trimIndent()
            )
    }
}
