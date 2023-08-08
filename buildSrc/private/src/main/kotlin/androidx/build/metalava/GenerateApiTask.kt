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

import androidx.build.Version
import androidx.build.checkapi.ApiBaselinesLocation
import androidx.build.checkapi.ApiLocation
import androidx.build.java.JavaCompileInputs
import java.io.File
import javax.inject.Inject
import org.gradle.api.file.Directory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor

/**
 * Generate API signature text files from a set of source files, and an API version history JSON
 * file from the previous API signature files.
 */
@CacheableTask
abstract class GenerateApiTask @Inject constructor(workerExecutor: WorkerExecutor) :
    MetalavaTask(workerExecutor) {
    @get:Internal // already expressed by getApiLintBaseline()
    abstract val baselines: Property<ApiBaselinesLocation>

    @Optional
    @PathSensitive(PathSensitivity.NONE)
    @InputFile
    fun getApiLintBaseline(): File? {
        val baseline = baselines.get().apiLintFile
        return if (baseline.exists()) baseline else null
    }

    @get:Input var targetsJavaConsumers: Boolean = true

    @get:Input var generateRestrictToLibraryGroupAPIs = true

    /** Collection of text files to which API signatures will be written. */
    @get:Internal // already expressed by getTaskOutputs()
    abstract val apiLocation: Property<ApiLocation>

    @OutputFiles
    fun getTaskOutputs(): List<File> {
        val prop = apiLocation.get()
        return listOf(
            prop.publicApiFile,
            prop.removedApiFile,
            prop.restrictedApiFile,
            prop.apiLevelsFile
        )
    }

    @get:Internal abstract val currentVersion: Property<Version>

    /**
     * The directory where past API files are stored. Not all files in the directory are used, they
     * are filtered in [getPastApiFiles].
     */
    @get:Internal abstract var projectApiDirectory: Directory

    /** An ordered list of the API files to use in generating the API level metadata JSON. */
    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    fun getPastApiFiles(): List<File> {
        return getFilesForApiLevels(projectApiDirectory.asFileTree.files, currentVersion.get())
    }

    @TaskAction
    fun exec() {
        check(bootClasspath.files.isNotEmpty()) { "Android boot classpath not set." }
        check(sourcePaths.files.isNotEmpty()) { "Source paths not set." }

        val inputs = JavaCompileInputs(sourcePaths, dependencyClasspath, bootClasspath)

        val levelsArgs =
            getGenerateApiLevelsArgs(
                getPastApiFiles(),
                currentVersion.get(),
                apiLocation.get().apiLevelsFile
            )

        generateApi(
            metalavaClasspath,
            inputs,
            apiLocation.get(),
            ApiLintMode.CheckBaseline(baselines.get().apiLintFile, targetsJavaConsumers),
            generateRestrictToLibraryGroupAPIs,
            levelsArgs,
            k2UastEnabled.get(),
            workerExecutor,
            manifestPath.orNull?.asFile?.absolutePath
        )
    }
}
