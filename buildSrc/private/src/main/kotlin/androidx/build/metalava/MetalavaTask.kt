/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.build.metalava

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

/** Base class for invoking Metalava. */
@CacheableTask
abstract class MetalavaTask
@Inject
constructor(@Internal protected val workerExecutor: WorkerExecutor) : DefaultTask() {
    /** Classpath containing Metalava and its dependencies. */
    @get:Classpath abstract val metalavaClasspath: ConfigurableFileCollection

    /** Android's boot classpath */
    @get:Classpath lateinit var bootClasspath: FileCollection

    /** Dependencies (compiled classes) of [sourcePaths]. */
    @get:Classpath lateinit var dependencyClasspath: FileCollection

    /**
     * Specifies both the source files and their corresponding compiled class files
     *
     * We specify the source files to pass to Metalava because that's the format that Metalava
     * needs.
     *
     * However, Metalava is only supposed to read the public API, so we don't need to rerun Metalava
     * if no API changes occurred.
     *
     * Gradle doesn't offer all of the same abilities as Metalava for writing a signature file and
     * validating its compatibility, but Gradle does offer the ability to check whether two sets of
     * classes have the same API.
     *
     * So, we ask Gradle to rerun this task only if the public API changes, which we implement by
     * declaring the compiled classes as inputs rather than the sources
     */
    fun putSourcePaths(sourcePaths: FileCollection, compiledSources: FileCollection) {
        this.sourcePaths = sourcePaths
        this.compiledSources = compiledSources
    }

    /** Source files against which API signatures will be validated. */
    @get:Internal // UP-TO-DATE checking is done based on the compiled classes
    var sourcePaths: FileCollection = project.files()
    /** Class files compiled from sourcePaths */
    @get:Classpath var compiledSources: FileCollection = project.files()

    /** Multiplatform source files from the module's common sourceset */
    @get:[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    var commonModuleSourcePaths: FileCollection = project.files()

    @get:[Optional InputFile PathSensitive(PathSensitivity.NONE)]
    abstract val manifestPath: RegularFileProperty

    @get:Input abstract val k2UastEnabled: Property<Boolean>

    @get:Input abstract val kotlinSourceLevel: Property<KotlinVersion>

    fun runWithArgs(args: List<String>) {
        runMetalavaWithArgs(
            metalavaClasspath,
            args,
            k2UastEnabled.get(),
            kotlinSourceLevel.get(),
            workerExecutor
        )
    }
}
