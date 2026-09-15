/*
 * Copyright 2026 The Android Open Source Project
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
import java.io.File
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.workers.WorkerExecutor

/** A metalava task that uses signature files to run compatibility checks. */
@CacheableTask
internal abstract class CompatibilityMetalavaTask(workerExecutor: WorkerExecutor) :
    MetalavaTask(workerExecutor) {
    /** Location of the previous API surface for compatibility checks. */
    @get:Internal // already expressed by getTaskInputs()
    abstract val referenceApi: Property<ApiLocation>

    /** Location of the current API surface to check. */
    @get:Internal // already expressed by getTaskInputs()
    abstract val api: Property<ApiLocation>

    /** Location of the text files listing violations that should be ignored. */
    @get:Internal // already expressed by getTaskInputs()
    abstract val baselines: Property<ApiBaselinesLocation>

    /** Version for the current API surface. */
    @get:Input abstract val version: Property<Version>

    @PathSensitive(PathSensitivity.RELATIVE)
    @InputFiles
    fun getTaskInputs(): List<File> {
        val apiLocation = api.get()
        val referenceApiLocation = referenceApi.get()
        val baselineApiLocation = baselines.get()
        return listOf(
            apiLocation.publicApiFile,
            apiLocation.restrictedApiFile,
            apiLocation.multiplatformApiDirectory,
            referenceApiLocation.publicApiFile,
            referenceApiLocation.restrictedApiFile,
            referenceApiLocation.multiplatformApiDirectory,
            baselineApiLocation.publicApiFile,
            baselineApiLocation.restrictedApiFile,
        )
    }

    /** Whether there are restricted APIs to check. */
    protected fun restrictedApisExist(): Boolean = referenceApi.get().restrictedApiFile.exists()

    /** Returns the baseline file to use, depending on whether it is for [restricted] APIs. */
    protected fun getBaselineFile(restricted: Boolean): File {
        return if (restricted) {
            baselines.get().restrictedApiFile
        } else {
            baselines.get().publicApiFile
        }
    }

    /**
     * Returns the list of common arguments for compatibility tasks.
     *
     * @param restricted whether this compatibility check is for restricted APIs
     * @param freezeApis whether APIs are frozen and no changes should be allowed
     */
    protected fun getCompatibilityArguments(
        restricted: Boolean,
        freezeApis: Boolean,
    ): List<String> {
        val (currentSignature, previousSignature) =
            if (restricted) {
                api.get().restrictedApiFile to referenceApi.get().restrictedApiFile
            } else {
                api.get().publicApiFile to referenceApi.get().publicApiFile
            }
        // Restricted API files aren't generated for multiplatform.
        val (currentMultiplatform, previousMultiplatform) =
            if (restricted) {
                null to null
            } else {
                api.get().multiplatformApiDirectory to referenceApi.get().multiplatformApiDirectory
            }

        return buildList {
            val classpath = bootClasspath + dependencyClasspath.files
            if (classpath.isNotEmpty()) {
                add("--classpath")
                add(classpath.joinToString(File.pathSeparator))
            }
            // Compatibility check for regular signature files, if they exist.
            if (currentSignature.exists()) {
                add("--source-files")
                add(currentSignature.toString())
                add("--check-compatibility:api:released")
                add(previousSignature.toString())
            }
            // Compatibility check for multiplatform signature files, if they exist. Gradle may
            // create an empty directory because this is marked as a task input/output, but only use
            // it if there are actually any signature files in it.
            if (previousMultiplatform?.let { ApiLocation.containsApiFiles(it) } == true) {
                add("--multiplatform-enabled")
                add("--multiplatform-api-sources")
                add(currentMultiplatform.toString())
                add("--multiplatform-compatibility-api")
                add(previousMultiplatform.toString())
            }

            add("--warnings-as-errors")

            if (freezeApis) {
                add("--error-category")
                add("Compatibility")
            }

            if (!targetsJavaConsumers.get()) {
                add("--hide")
                add("RemovedFromJava")
            }
        }
    }
}
