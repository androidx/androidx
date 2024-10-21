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

import androidx.binarycompatibilityvalidator.BinaryCompatibilityChecker
import androidx.binarycompatibilityvalidator.KlibDumpParser
import androidx.binarycompatibilityvalidator.ValidationException
import androidx.build.Version
import androidx.build.metalava.shouldFreezeApis
import androidx.build.metalava.summarizeDiff
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader

@CacheableTask
@OptIn(ExperimentalLibraryAbiReader::class)
abstract class CheckAbiIsCompatibleTask : DefaultTask() {

    /** Text file from which API signatures will be read. */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract val previousApiDump: RegularFileProperty

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract val currentApiDump: RegularFileProperty

    @get:Input abstract var referenceVersion: Provider<String>

    @get:Input abstract var projectVersion: Provider<String>

    @TaskAction
    fun execute() {
        val (previousApiPath, previousApiDumpText) =
            previousApiDump.get().asFile.let { it.path to it.readText() }
        val (currentApiPath, currentApiDumpText) =
            currentApiDump.get().asFile.let { it.path to it.readText() }
        val shouldFreeze =
            shouldFreezeApis(Version(referenceVersion.get()), Version(projectVersion.get()))
        if (shouldFreeze && previousApiDumpText != currentApiDumpText) {
            throw GradleException(frozenApiErrorMessage(referenceVersion.get()))
        }

        val previousDump = KlibDumpParser(previousApiDumpText, previousApiPath).parse()
        val currentDump = KlibDumpParser(currentApiDumpText, currentApiPath).parse()

        try {
            BinaryCompatibilityChecker.checkAllBinariesAreCompatible(currentDump, previousDump)
        } catch (e: ValidationException) {
            throw GradleException(compatErrorMessage(e), e)
        }
    }

    private fun compatErrorMessage(validationException: ValidationException) =
        "Your change has binary compatibility issues. Please resolve them before updating." +
            "\n${validationException.message}" +
            "\nIf you believe these changes are actually compatible and that this is a tooling" +
            "error, please file a bug. $NEW_ISSUE_URL"

    private fun frozenApiErrorMessage(referenceVersion: String) =
        "The API surface was finalized in $referenceVersion. Revert the changes unless you have " +
            "permission from Android API Council. " +
            summarizeDiff(previousApiDump.get().asFile, currentApiDump.get().asFile)
}
