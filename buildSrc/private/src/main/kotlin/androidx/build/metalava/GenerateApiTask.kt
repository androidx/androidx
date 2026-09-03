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
import androidx.build.checkapi.ApiLocation
import java.io.File
import javax.inject.Inject
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
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
internal abstract class GenerateApiTask @Inject constructor(workerExecutor: WorkerExecutor) :
    SourceMetalavaTask(workerExecutor) {

    @get:Input abstract val generateRestrictToLibraryGroupAPIs: Property<Boolean>

    /** Collection of text files to which API signatures will be written. */
    @get:Internal // already expressed by getTaskOutputs()
    abstract val apiLocation: Property<ApiLocation>

    @OutputFiles
    fun getTaskOutputs(): List<File> {
        val prop = apiLocation.get()
        return listOf(prop.publicApiFile, prop.restrictedApiFile, prop.apiLevelsFile)
    }

    @OutputDirectory
    fun getTaskOutputDirectory(): File {
        return apiLocation.get().multiplatformApiDirectory
    }

    @get:Internal abstract val currentVersion: Property<Version>

    /**
     * The directory where past API files are stored. Not all files in the directory are used, they
     * are filtered in [getPastApiFiles].
     *
     * This value is optional if the task does not generate API level metadata.
     */
    @get:Internal abstract val projectApiDirectory: DirectoryProperty

    /** An ordered list of the API files to use in generating the API level metadata JSON. */
    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    fun getPastApiFiles(): List<File> {
        return projectApiDirectory.orNull?.let { projectApiDirectory ->
            getFilesForApiLevels(projectApiDirectory.asFileTree.files, currentVersion.get())
        } ?: emptyList()
    }

    @TaskAction
    open fun exec() {
        // Only require an android jar, sources, and a classpath when there is a main jvm/android
        // target that will have an API surface generated.
        if (hasJvmOrAndroidTarget.get()) {
            check(bootClasspath.files.isNotEmpty()) { "Android boot classpath not set." }
            check(sourcePaths.files.isNotEmpty()) { "Source paths not set." }
            check(compiledSources.files.isNotEmpty()) {
                "Compiled sources " + compiledSources + " is empty!"
            }
            compiledSources.files.forEach { compiled ->
                check(compiled.exists()) { "File " + compiled + " does not exist" }
            }
        }

        val levelsArgs =
            getGenerateApiLevelsArgs(
                projectApiDirectory.asFile.get(),
                getPastApiFiles(),
                currentVersion.get(),
                apiLocation.get().apiLevelsFile,
            )

        generateApi(
            createProjectXmlFile(sourceSets.get()),
            ApiLintMode.CheckBaseline(baselines.get().apiLintFile, targetsJavaConsumers.get()),
            levelsArgs,
            multiplatform.get(),
        )
    }

    /**
     * Generates all of the specified api files, as well as a version history JSON for the public
     * API.
     */
    protected fun generateApi(
        projectXml: File,
        apiLintMode: ApiLintMode,
        apiLevelsArgs: List<String>,
        multiplatform: Boolean,
    ) {
        val generateApiConfigs: MutableList<Pair<GenerateApiMode, ApiLintMode>> =
            mutableListOf(GenerateApiMode.PublicApi to apiLintMode)

        // Generate `RestrictTo` APIs as a separate API surface. This does not make sense to do for
        // projects without a jvm/android target, because the purpose of tracking `RestrictTo` is
        // for
        // maintaining binary compatibility, but metalava can only enforce binary compatibility for
        // jvm
        // based projects.
        @Suppress("LiftReturnOrAssignment")
        if (hasJvmOrAndroidTarget.get()) {
            if (generateRestrictToLibraryGroupAPIs.get()) {
                generateApiConfigs += GenerateApiMode.AllRestrictedApis to ApiLintMode.Skip
            } else {
                generateApiConfigs +=
                    GenerateApiMode.RestrictToLibraryGroupPrefixApis to ApiLintMode.Skip
            }
        }

        generateApiConfigs.forEach { (generateApiMode, apiLintMode) ->
            val args =
                getGenerateApiArgs(
                    projectXml,
                    sourcePaths.files,
                    includeCompiledSources = true,
                    apiLocation.get(),
                    generateApiMode,
                    apiLintMode,
                    apiLevelsArgs,
                    multiplatform,
                )
            runWithArgs(args)
        }
    }
}
