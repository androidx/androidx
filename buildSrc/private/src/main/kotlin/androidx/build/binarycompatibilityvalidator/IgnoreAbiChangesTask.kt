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
import androidx.build.Version
import androidx.build.metalava.shouldFreezeApis
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader

@CacheableTask
abstract class IgnoreAbiChangesTask
@Inject
constructor(@Internal protected val workerExecutor: WorkerExecutor) : DefaultTask() {

    /** Text file from which API signatures will be read. */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract val previousApiDump: RegularFileProperty

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract val currentApiDump: RegularFileProperty

    @get:OutputFile abstract val ignoreFile: RegularFileProperty

    @get:Classpath abstract val runtimeClasspath: ConfigurableFileCollection

    @get:Input abstract var referenceVersion: Provider<String>

    @get:Input abstract var projectVersion: Provider<String>

    @TaskAction
    fun execute() {
        // Execute BCV code as a WorkAction to allow setting the classpath for the action.
        // This is to work around the kotlin compiler needing to be a compileOnly dependency for
        // buildSrc (https://kotl.in/gradle/internal-compiler-symbols, aosp/3368960).
        val workQueue = workerExecutor.classLoaderIsolation { it.classpath.from(runtimeClasspath) }
        workQueue.submit(IgnoreChangesWorker::class.java) { params ->
            params.previousApiDump.set(previousApiDump)
            params.currentApiDump.set(currentApiDump)
            params.ignoreFile.set(ignoreFile)
            params.referenceVersion.set(referenceVersion.get())
            params.projectVersion.set(projectVersion.get())
        }
    }
}

private interface IgnoreChangesParameters : WorkParameters {
    val previousApiDump: RegularFileProperty
    val currentApiDump: RegularFileProperty
    val ignoreFile: RegularFileProperty
    val referenceVersion: Property<String>
    val projectVersion: Property<String>
}

private abstract class IgnoreChangesWorker : WorkAction<IgnoreChangesParameters> {
    @OptIn(ExperimentalLibraryAbiReader::class)
    override fun execute() {
        val previousDump = KlibDumpParser(parameters.previousApiDump.get().asFile).parse()
        val currentDump = KlibDumpParser(parameters.currentApiDump.get().asFile).parse()
        val shouldFreeze =
            shouldFreezeApis(
                Version(parameters.referenceVersion.get()),
                Version(parameters.projectVersion.get()),
            )
        val ignoredErrors =
            BinaryCompatibilityChecker.checkAllBinariesAreCompatible(
                    currentDump,
                    previousDump,
                    null,
                    validate = false,
                    shouldFreeze = shouldFreeze,
                )
                .map { it.toString() }
                .toSet()
        parameters.ignoreFile.get().asFile.apply {
            if (!exists()) {
                createNewFile()
            }
            writeText(FORMAT_STRING + "\n" + ignoredErrors.joinToString("\n"))
        }
    }

    private companion object {
        const val BASELINE_FORMAT_VERSION = "1.0"
        const val FORMAT_STRING = "// Baseline format: $BASELINE_FORMAT_VERSION"
    }
}
