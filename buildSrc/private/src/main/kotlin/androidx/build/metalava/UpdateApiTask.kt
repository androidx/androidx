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

import androidx.build.checkapi.ApiLocation
import com.google.common.io.Files
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Updates API signature text files. In practice, the values they will be updated to will match the
 * APIs defined by the source code.
 */
@CacheableTask
abstract class UpdateApiTask : DefaultTask() {

    /** Text file from which API signatures will be read. */
    @get:Input abstract val inputApiLocation: Property<ApiLocation>

    /** Text files to which API signatures will be written. */
    @get:Internal // outputs are declared in getTaskOutputs()
    abstract val outputApiLocations: ListProperty<ApiLocation>

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    fun getTaskInputs(): List<File> {
        val inputApi = inputApiLocation.get()
        return listOf(inputApi.publicApiFile, inputApi.restrictedApiFile, inputApi.removedApiFile)
    }

    @Suppress("unused")
    @OutputFiles
    fun getTaskOutputs(): List<File> {
        return outputApiLocations.get().flatMap { outputApiLocation ->
            listOf(
                outputApiLocation.publicApiFile,
                outputApiLocation.restrictedApiFile,
                outputApiLocation.removedApiFile
            )
        }
    }

    @TaskAction
    fun exec() {
        for (outputApi in outputApiLocations.get()) {
            val inputApi = inputApiLocation.get()
            copy(source = inputApi.publicApiFile, dest = outputApi.publicApiFile, logger = logger)
            copy(source = inputApi.removedApiFile, dest = outputApi.removedApiFile, logger = logger)
            copy(
                source = inputApi.restrictedApiFile,
                dest = outputApi.restrictedApiFile,
                logger = logger
            )
        }
    }
}

fun copy(source: File, dest: File, permitOverwriting: Boolean = true, logger: Logger? = null) {
    if (!permitOverwriting) {
        val sourceText =
            if (source.exists()) {
                source.readText()
            } else {
                ""
            }
        val overwriting = (dest.exists() && sourceText != dest.readText())
        val changing = overwriting || (dest.exists() != source.exists())
        if (changing) {
            if (overwriting) {
                val diff = summarizeDiff(source, dest, maxDiffLines + 1)
                val diffMsg =
                    if (compareLineCount(diff, maxDiffLines) > 0) {
                        "Diff is greater than $maxDiffLines lines, use diff tool to compare.\n\n"
                    } else {
                        "Diff:\n$diff\n\n"
                    }
                val message =
                    "Modifying the API definition for a previously released artifact " +
                        "having a final API version (version not ending in '-alpha') is not " +
                        "allowed.\n\n" +
                        "Previously declared definition is $dest\n" +
                        "Current generated   definition is $source\n\n" +
                        diffMsg +
                        "Did you mean to increment the library version first?\n\n" +
                        "If you have a valid reason to override Semantic Versioning policy, see " +
                        "go/androidx/versioning#beta-api-change for information on obtaining " +
                        "approval."
                throw GradleException(message)
            }
        }
    }

    if (source.exists()) {
        Files.copy(source, dest)
        logger?.lifecycle("Wrote ${dest.name}")
    } else if (dest.exists()) {
        dest.delete()
        logger?.lifecycle("Deleted ${dest.name}")
    }
}

/**
 * Returns -1 if [text] has fewer than [count] newline characters, 0 if equal, and 1 if greater
 * than.
 */
fun compareLineCount(text: String, count: Int): Int {
    var found = 0
    var index = 0
    while (found < count) {
        index = text.indexOf('\n', index)
        if (index < 0) {
            break
        }
        found++
        index++
    }
    return if (found < count) {
        -1
    } else if (found == count) {
        0
    } else {
        1
    }
}

/** Maximum number of diff lines to include in output. */
internal const val maxDiffLines = 8
