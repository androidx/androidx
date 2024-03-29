/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.build.resources

import androidx.build.checkapi.ApiLocation
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Generates a resource API file for consumption by other API tasks. */
@CacheableTask
abstract class GenerateResourceApiTask : DefaultTask() {
    /**
     * Public resources text file generated by AAPT.
     *
     * This file must be defined, but the file may not exist on the filesystem if the library has no
     * resources. In that case, we will generate an empty API signature file.
     */
    @get:InputFiles // InputFiles allows non-existent files, whereas InputFile does not.
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val builtApi: RegularFileProperty

    /** Source files against which API signatures will be validated. */
    @get:[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    var sourcePaths: Collection<File> = emptyList()

    /** Text file to which API signatures will be written. */
    @get:Internal abstract val apiLocation: Property<ApiLocation>

    @OutputFile
    fun getTaskOutput(): File {
        return apiLocation.get().resourceFile
    }

    @TaskAction
    fun generateResourceApi() {
        val builtApiFile = builtApi.get().asFile
        val sortedApiLines =
            if (builtApiFile.exists()) {
                builtApiFile.readLines().toSortedSet()
            } else {
                val errorMessage =
                    """No public resources defined

At least one public resource must be defined to prevent all resources from
appearing public by  default.

This exception should never occur for AndroidX projects, as a <public />
resource is added by default to all library project. Please contact the
AndroidX Core team for assistance."""
                throw GradleException(errorMessage)
            }

        val outputApiFile = apiLocation.get().resourceFile
        outputApiFile.bufferedWriter().use { out ->
            sortedApiLines.forEach {
                out.write(it)
                out.newLine()
            }
        }
    }
}
