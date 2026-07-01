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
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.abi.tools.AbiFilters
import org.jetbrains.kotlin.abi.tools.AbiTools
import org.jetbrains.kotlin.abi.tools.KlibTarget

@CacheableTask
abstract class GenerateAbiTask
@Inject
constructor(@Internal protected val workerExecutor: WorkerExecutor) : DefaultTask() {
    @get:OutputFile abstract val abiFile: RegularFileProperty

    @get:Nested internal abstract val klibs: ListProperty<KlibTargetInfo>

    @get:[Input Optional]
    abstract val excludedAnnotatedWith: SetProperty<String>

    @get:Classpath abstract val runtimeClasspath: ConfigurableFileCollection

    @TaskAction
    fun execute() {
        // Execute BCV code as a WorkAction to allow setting the classpath for the action.
        // This is to work around the kotlin compiler needing to be a compileOnly dependency for
        // buildSrc (https://kotl.in/gradle/internal-compiler-symbols, aosp/3368960).
        val workQueue = workerExecutor.classLoaderIsolation { it.classpath.from(runtimeClasspath) }
        workQueue.submit(KlibDumpWorker::class.java) { params ->
            params.mergedApiFile.set(abiFile)
            params.klibs.set(klibs)
            params.excludedAnnotatedWith.set(excludedAnnotatedWith)
        }
    }
}

abstract class KlibDumpWorker : WorkAction<KlibDumpWorker.Parameters> {
    internal interface Parameters : WorkParameters {
        @get:OutputFile abstract val mergedApiFile: RegularFileProperty

        @get:Nested abstract val klibs: ListProperty<KlibTargetInfo>

        @get:[Input Optional]
        abstract val excludedAnnotatedWith: SetProperty<String>
    }

    private val abiTools = AbiTools.getInstance()

    override fun execute() {
        val klibTargets = parameters.klibs.get()

        val filters =
            AbiFilters(
                includedClasses = emptySet(),
                excludedClasses = emptySet(),
                includedAnnotatedWith = emptySet(),
                parameters.excludedAnnotatedWith.getOrElse(mutableSetOf()),
            )
        val mergedDump = abiTools.createKlibDump()
        klibTargets.forEach { suite ->
            val klibDir = suite.klibFiles.files.first()
            if (klibDir.exists()) {
                val dump =
                    abiTools.extractKlibAbi(
                        klibDir,
                        KlibTarget(suite.canonicalTargetName, suite.targetName),
                        filters,
                    )
                mergedDump.merge(dump)
            }
        }
        mergedDump.print(parameters.mergedApiFile.get().asFile)
    }
}

internal abstract class KlibTargetInfo {
    @get:Input abstract var targetName: String

    @get:Input abstract var canonicalTargetName: String

    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract var klibFiles: FileCollection
}
