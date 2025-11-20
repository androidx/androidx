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

package androidx.build.binarycompatibilityvalidator

import androidx.build.metalava.summarizeDiff
import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.konan.target.HostManager

/** Compares two ABI txt files against each other to confirm they are equal */
@CacheableTask
abstract class CheckAbiEquivalenceTask : DefaultTask() {

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract var checkedInDump: Provider<RegularFileProperty>

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract var builtDump: Provider<RegularFileProperty>

    @get:Input abstract val shouldWriteVersionedAbiFile: Property<Boolean>
    @get:Input abstract val version: Property<String>

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    abstract val projectAbiDir: DirectoryProperty

    @get:OutputFile abstract val debugOutFile: RegularFileProperty

    @TaskAction
    fun execute() {
        if (shouldWriteVersionedAbiFile.get()) {
            val versionedFile = projectAbiDir.get().asFile.resolve("${version.get()}.txt")
            if (!versionedFile.exists()) {
                throw GradleException("Missing versioned abi file: ${versionedFile.path}")
            }
        }
        checkEqual()
    }

    private fun checkEqual() {
        val expected = checkedInDump.get().asFile.get()
        val actual = builtDump.get().asFile.get()
        val debugOutFile = debugOutFile.get().asFile
        if (!FileUtils.contentEquals(expected, actual)) {
            if (HostManager.hostIsMac) {
                actual.copyTo(debugOutFile, overwrite = true)
            }
            val diff = summarizeDiff(expected, actual)
            val messageBuilder = StringBuilder()
            messageBuilder.append(
                """
        ABI definition has changed

        Declared definition is $expected
        True     definition is $actual

        Please run `./gradlew updateAbi` to confirm these changes are
        intentional by updating the ABI definition.
        """
            )
            if (HostManager.hostIsMac) {
                messageBuilder.append(
                    """

            Actual output file has been written to ${debugOutFile.path}.
            If you are unable to generate the dump file for all targets locally you can copy the definition from the expected output file created during presubmit.
            """
                        .trimIndent()
                )
            }
            messageBuilder.append(
                """

        Difference between these files:
        $diff""${'"'}
        """
                    .trimIndent()
            )
            throw GradleException(messageBuilder.toString())
        }
    }
}
