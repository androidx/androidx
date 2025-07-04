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
import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader

@CacheableTask
abstract class CheckAbiIsCompatibleTask
@Inject
constructor(@Internal protected val workerExecutor: WorkerExecutor) : DefaultTask() {

    // Input annotation is handled by getIgnoreFile
    @get:Internal abstract val ignoreFile: RegularFileProperty

    /** Text file from which API signatures will be read. */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract val previousApiDump: RegularFileProperty

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract val currentApiDump: RegularFileProperty

    @get:Input abstract var referenceVersion: Provider<String>

    @get:Input abstract var projectVersion: Provider<String>

    @PathSensitive(PathSensitivity.RELATIVE)
    @InputFile
    @Optional
    fun getBaseline(): File? = ignoreFile.get().asFile.takeIf { it.exists() }

    @get:Classpath abstract val runtimeClasspath: ConfigurableFileCollection

    @TaskAction
    fun execute() {
        val (previousApiPath, previousApiDumpText) =
            previousApiDump.get().asFile.let { it.path to it.readText() }
        val (currentApiPath, currentApiDumpText) =
            currentApiDump.get().asFile.let { it.path to it.readText() }
        val shouldFreeze =
            shouldFreezeApis(Version(referenceVersion.get()), Version(projectVersion.get()))

        // Execute BCV code as a WorkAction to allow setting the classpath for the action.
        // This is to work around the kotlin compiler needing to be a compileOnly dependency for
        // buildSrc (https://kotl.in/gradle/internal-compiler-symbols, aosp/3368960).
        val workQueue = workerExecutor.classLoaderIsolation { it.classpath.from(runtimeClasspath) }
        workQueue.submit(CheckCompatibilityWorker::class.java) { params ->
            params.previousApiDumpText.set(previousApiDumpText)
            params.previousApiPath.set(previousApiPath)
            params.currentApiDumpText.set(currentApiDumpText)
            params.currentApiPath.set(currentApiPath)
            params.baseline.set(ignoreFile)
            params.shouldFreeze.set(shouldFreeze)
            params.referenceVersion.set(referenceVersion)
        }
    }
}

private interface CheckCompatibilityParameters : WorkParameters {
    val previousApiDumpText: Property<String>
    val previousApiPath: Property<String>
    val currentApiDumpText: Property<String>
    val currentApiPath: Property<String>
    val baseline: RegularFileProperty
    val referenceVersion: Property<String>
    val shouldFreeze: Property<Boolean>
}

private abstract class CheckCompatibilityWorker : WorkAction<CheckCompatibilityParameters> {
    @OptIn(ExperimentalLibraryAbiReader::class)
    override fun execute() {
        val previousDump =
            KlibDumpParser(parameters.previousApiDumpText.get(), parameters.previousApiPath.get())
                .parse()
        val currentDump =
            KlibDumpParser(parameters.currentApiDumpText.get(), parameters.currentApiPath.get())
                .parse()

        try {
            BinaryCompatibilityChecker.checkAllBinariesAreCompatible(
                currentDump,
                previousDump,
                parameters.baseline.get().asFile.takeIf { it.exists() },
                validate = true,
                shouldFreeze = parameters.shouldFreeze.get(),
            )
        } catch (e: ValidationException) {
            if (parameters.shouldFreeze.get()) {
                throw GradleException(
                    frozenApiErrorMessage(
                        parameters.referenceVersion.get(),
                        previousApiDump = File(parameters.previousApiPath.get()),
                        currentApiDump = File(parameters.currentApiPath.get()),
                    )
                )
            }
            throw GradleException(compatErrorMessage(e), e)
        }
    }

    private fun compatErrorMessage(validationException: ValidationException) =
        "Your change has binary compatibility issues. Please resolve them before updating." +
            "\n${validationException.message}" +
            "\nIf you believe these changes are actually compatible and that this is a tooling " +
            "error, please file a bug. $NEW_ISSUE_URL"

    private fun frozenApiErrorMessage(
        referenceVersion: String,
        previousApiDump: File,
        currentApiDump: File,
    ) =
        "The API surface was finalized in $referenceVersion. Revert the changes unless you have " +
            "permission from Android API Council. " +
            summarizeDiff(previousApiDump, currentApiDump)
}
