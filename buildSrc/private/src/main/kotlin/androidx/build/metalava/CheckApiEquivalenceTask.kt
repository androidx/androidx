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
import java.io.File
import java.util.concurrent.TimeUnit
import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/** Compares two API txt files against each other. */
@DisableCachingByDefault(because = "Doesn't benefit from caching")
abstract class CheckApiEquivalenceTask : DefaultTask() {
    /** Api file (in the build dir) to check */
    @get:Input abstract val builtApi: Property<ApiLocation>

    /** Api file (in source control) to compare against */
    @get:Input abstract val checkedInApis: ListProperty<ApiLocation>

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    fun getTaskInputs(): List<File> {
        val checkedInApiLocations = checkedInApis.get()
        val checkedInApiFiles =
            checkedInApiLocations.flatMap { checkedInApiLocation ->
                listOf(
                    checkedInApiLocation.publicApiFile,
                    checkedInApiLocation.restrictedApiFile
                )
            }

        val builtApiLocation = builtApi.get()
        val builtApiFiles =
            listOf(
                builtApiLocation.publicApiFile,
                builtApiLocation.restrictedApiFile
            )

        return checkedInApiFiles + builtApiFiles
    }

    @TaskAction
    fun exec() {
        val builtApiLocation = builtApi.get()
        for (checkedInApi in checkedInApis.get()) {
            checkEqual(checkedInApi.publicApiFile, builtApiLocation.publicApiFile)
            checkEqual(checkedInApi.restrictedApiFile, builtApiLocation.restrictedApiFile)
        }
    }
}

/**
 * Returns the output of running the `diff` command-line tool on files [a] and [b], truncated to
 * [maxSummaryLines] lines.
 */
fun summarizeDiff(a: File, b: File, maxSummaryLines: Int = 50): String {
    if (!a.exists()) {
        return "$a does not exist"
    }
    if (!b.exists()) {
        return "$b does not exist"
    }
    val process =
        ProcessBuilder(listOf("diff", a.toString(), b.toString()))
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()
    process.waitFor(5, TimeUnit.SECONDS)
    var diffLines = process.inputStream.bufferedReader().readLines().toMutableList()
    if (diffLines.size > maxSummaryLines) {
        diffLines = diffLines.subList(0, maxSummaryLines)
        diffLines.plusAssign("[long diff was truncated]")
    }
    return diffLines.joinToString("\n")
}

fun checkEqual(expected: File, actual: File) {
    if (!FileUtils.contentEquals(expected, actual)) {
        val diff = summarizeDiff(expected, actual)
        val message =
            """API definition has changed

                    Declared definition is $expected
                    True     definition is $actual

                    Please run `./gradlew updateApi` to confirm these changes are
                    intentional by updating the API definition.

                    Difference between these files:
                    $diff"""
        throw GradleException(message)
    }
}
