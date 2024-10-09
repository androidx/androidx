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
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor

@CacheableTask
abstract class ClangCompileTask @Inject constructor(private val workerExecutor: WorkerExecutor) :
    DefaultTask() {
    init {
        description = "Compiles C sources into an object file (.o)."
        group = "Build"
    }

    // ServiceReference is @Incubating since 8.0 https://github.com/gradle/gradle/issues/30858
    @Suppress("UnstableApiUsage")
    @get:ServiceReference(KonanBuildService.KEY)
    abstract val konanBuildService: Property<KonanBuildService>

    @get:Nested abstract val clangParameters: ClangCompileParameters

    @TaskAction
    fun compile() {
        workerExecutor.noIsolation().submit(ClangCompileWorker::class.java) {
            it.buildService.set(konanBuildService)
            it.clangParameters.set(clangParameters)
        }
    }
}

abstract class ClangCompileParameters {
    /** The compilation target platform for which the given inputs will be compiled. */
    @get:Input abstract val konanTarget: Property<SerializableKonanTarget>

    /** List of C source files. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val sources: ConfigurableFileCollection

    /** The output directory where the object files for each source file will be written. */
    @get:OutputDirectory abstract val output: DirectoryProperty

    /** List of directories that include the headers used in the compilation. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val includes: ConfigurableFileCollection

    /** List of arguments that will be passed into clang during compilation. */
    @get:Input abstract val freeArgs: ListProperty<String>
}

private abstract class ClangCompileWorker : WorkAction<ClangCompileWorker.Params> {
    interface Params : WorkParameters {
        val clangParameters: Property<ClangCompileParameters>
        val buildService: Property<KonanBuildService>
    }

    override fun execute() {
        val buildService = parameters.buildService.get()
        buildService.compile(parameters.clangParameters.get())
    }
}
