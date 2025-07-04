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

package androidx.build.binarycompatibilityvalidator

import javax.inject.Inject
import kotlinx.validation.ExperimentalBCVApi
import kotlinx.validation.KlibValidationSettings
import kotlinx.validation.WorkerAwareTaskBase
import kotlinx.validation.api.klib.KlibDump
import kotlinx.validation.api.klib.KlibDumpFilters
import kotlinx.validation.api.klib.KlibSignatureVersion
import kotlinx.validation.api.klib.KlibTarget
import kotlinx.validation.api.klib.saveTo
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader

/** Generates a text file with a KLib ABI dump for a single klib. */
@CacheableTask
abstract class KotlinKlibAbiBuildTask : WorkerAwareTaskBase() {
    @get:Inject internal abstract val executor: WorkerExecutor

    /**
     * Collection consisting of a single path to a compiled klib (either file, or directory).
     *
     * By the end of the compilation process, there might be no klib file emitted, for example, when
     * there are no sources in a project in general, or for a target in particular. The lack of a
     * compiled klib file is not considered as an error, and instead causes the ask being skipped.
     */
    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val klibFile: ConfigurableFileCollection

    /** Refer to [KlibValidationSettings.signatureVersion] for details. */
    @get:Input abstract val signatureVersion: Property<KlibSignatureVersion>

    /** A target [klibFile] was compiled for. */
    @get:Input abstract val target: Property<KlibTarget>

    @get:Input abstract val nonPublicMarkers: SetProperty<String>

    /** A path to the resulting dump file. */
    @get:OutputFile abstract val outputAbiFile: RegularFileProperty

    @TaskAction
    internal fun generate() {
        val workQueue = executor.classLoaderIsolation { it.classpath.from(runtimeClasspath) }
        workQueue.submit(KlibAbiBuildWorker::class.java) { params ->
            params.klibFile.from(klibFile)
            params.nonPublicMarkers.set(nonPublicMarkers)
            params.target.set(target)
            params.signatureVersion.set(signatureVersion)
            params.outputAbiFile.set(outputAbiFile)
        }
    }
}

internal interface KlibAbiBuildParameters : WorkParameters {
    val klibFile: ConfigurableFileCollection
    val nonPublicMarkers: SetProperty<String>
    val signatureVersion: Property<KlibSignatureVersion>
    val target: Property<KlibTarget>
    val outputAbiFile: RegularFileProperty
}

internal abstract class KlibAbiBuildWorker : WorkAction<KlibAbiBuildParameters> {
    @OptIn(ExperimentalBCVApi::class, ExperimentalLibraryAbiReader::class)
    override fun execute() {
        val outputFile = parameters.outputAbiFile.asFile.get()
        outputFile.delete()
        outputFile.parentFile.mkdirs()

        val dump =
            KlibDump.fromKlib(
                parameters.klibFile.singleFile,
                parameters.target.get().configurableName,
                KlibDumpFilters {
                    nonPublicMarkers.addAll(parameters.nonPublicMarkers.get())
                    signatureVersion = parameters.signatureVersion.get()
                },
            )

        dump.saveTo(outputFile)
    }
}
