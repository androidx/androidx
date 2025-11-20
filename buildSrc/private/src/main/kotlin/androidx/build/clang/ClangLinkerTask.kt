/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.build.clang

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.konan.target.LinkerOutputKind

@CacheableTask
abstract class ClangLinkerTask @Inject constructor(private val workerExecutor: WorkerExecutor) :
    DefaultTask() {
    init {
        description =
            "Combines multiple object files (.o) into either a shared library file" +
                "(.so / .dylib) or an executable."
        group = "Build"
    }

    @get:ServiceReference(KonanBuildService.KEY)
    abstract val konanBuildService: Property<KonanBuildService>

    @get:Nested abstract val clangParameters: ClangLinkerParameters

    @TaskAction
    fun archive() {
        workerExecutor.noIsolation().submit(ClangLinkerWorker::class.java) {
            it.clangParameters.set(clangParameters)
            it.buildService.set(konanBuildService)
        }
    }
}

abstract class ClangLinkerParameters {

    /** The kind of output the linker should produce. */
    @get:Input abstract val linkerOutputKind: Property<LinkerOutputKind>

    /** The target platform for the shared file. */
    @get:Input abstract val konanTarget: Property<SerializableKonanTarget>

    /** List of object files that will be added to the shared file output. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val objectFiles: ConfigurableFileCollection

    /**
     * The final output file that will include the shared library containing the given
     * [objectFiles].
     */
    @get:OutputFile abstract val outputFile: RegularFileProperty

    /**
     * List of additional objects that will be dynamically linked with the output file. At runtime,
     * these need to be available for the shared object output to work.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val linkedObjects: ConfigurableFileCollection

    /** List of arguments that will be passed into linker when creating a shared library. */
    @get:Input abstract val linkerArgs: ListProperty<String>
}

private abstract class ClangLinkerWorker : WorkAction<ClangLinkerWorker.Params> {
    interface Params : WorkParameters {
        val clangParameters: Property<ClangLinkerParameters>
        val buildService: Property<KonanBuildService>
    }

    override fun execute() {
        val buildService = parameters.buildService.get()
        buildService.runLinker(parameters.clangParameters.get())
    }
}
