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

import androidx.build.checkapi.ApiBaselinesLocation
import androidx.build.checkapi.SourceSetInputs
import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
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

    /** Dependencies (compiled classes) of the project. */
    @get:Classpath lateinit var dependencyClasspath: FileCollection

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

/** A metalava task that takes source code as input (other tasks take signature files). */
@CacheableTask
internal abstract class SourceMetalavaTask(workerExecutor: WorkerExecutor) :
    MetalavaTask(workerExecutor) {
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
    /** Source files against which API signatures will be validated. */
    @get:Internal // UP-TO-DATE checking is done based on the compiled classes
    var sourcePaths: FileCollection = project.files()

    /** Class files compiled from sourcePaths */
    @get:Classpath var compiledSources: FileCollection = project.files()

    @get:[Optional InputFile PathSensitive(PathSensitivity.NONE)]
    abstract val manifestPath: RegularFileProperty

    @get:Internal // already expressed by getApiLintBaseline()
    abstract val baselines: Property<ApiBaselinesLocation>

    @Optional
    @PathSensitive(PathSensitivity.NONE)
    @InputFile
    fun getInputApiLintBaseline(): File? {
        val baseline = baselines.get().apiLintFile
        return if (baseline.exists()) baseline else null
    }

    @get:Input abstract val targetsJavaConsumers: Property<Boolean>

    /**
     * Information about all source sets for multiplatform projects. Non-multiplatform projects
     * should be represented as a list with one source set.
     *
     * This is marked as [Internal] because [compiledSources] is what should determine whether to
     * rerun metalava.
     */
    @get:Internal abstract val sourceSets: ListProperty<SourceSetInputs>

    /**
     * Creates an XML file representing the project structure.
     *
     * This should only be called during task execution.
     */
    protected fun createProjectXmlFile(): File {
        val sourceSets = sourceSets.get()
        check(sourceSets.isNotEmpty()) { "Project must have at least one source set." }
        val outputFile = File(temporaryDir, "project.xml")
        ProjectXml.create(
            sourceSets,
            bootClasspath.files,
            compiledSources.singleFile,
            outputFile,
        )
        return outputFile
    }
}
