/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.test.uiautomator

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.internal.instrumentationPackageMediaDir
import androidx.test.uiautomator.internal.joinLines
import java.io.File

/**
 * Allows to report test results to Android Studio. This is helpful especially when debugging a
 * test.
 *
 * For example:
 * ```kotlin
 * val file = resultsReporter.addNewFile("filename.jpg", "MyButton")
 * bitmap.saveToFile(file)
 * ```
 */
public class ResultsReporter(private val testName: String) {

    private data class Result(val file: File, val title: String)

    private val outputFolder: File = instrumentationPackageMediaDir
    private val results = mutableSetOf<Result>()

    /**
     * Returns a new file to add to the test results for Android Studio. The file will be created
     * with the given [filename] and reported with the given [title]. In order to be recognized from
     * android studio, the file needs to be created in [instrumentationPackageMediaDir].
     *
     * Usage example:
     * ```kotlin
     * val file = resultsReporter.addNewFile("filename.jpg", "MyButton")
     * bitmap.saveToFile(file)
     * ```
     *
     * @param filename the name of the file to add for reporting.
     * @param title a title to use when reporting to Android Studio.
     * @return a [File] for the file to report to Android Studio.
     */
    public fun addNewFile(filename: String, title: String): File {
        val file = File(outputFolder, filename)
        results.add(Result(file = file, title = title))
        return file
    }

    /**
     * At the end of the test execution reports to Android Studio what files need to be pulled that
     * were previously added with [addNewFile].
     */
    public fun reportToInstrumentation() {

        // Prepare the summary
        val summary =
            joinLines(

                // Summary header
                "Test results for `$testName`",

                // Link to files
                *results.map { "[${it.title}](file://${it.file.name})" }.toTypedArray(),
            )

        // Prepare the bundle to report to instrumentation
        val bundle =
            Bundle().apply {

                // Summary
                putString("android.studio.display.benchmark", summary)
                putString("android.studio.v2display.benchmark", summary)
                putString(
                    "android.studio.v2display.benchmark.outputDirPath",
                    outputFolder.absolutePath,
                )

                // Result files
                results.forEach { r ->
                    val suffix = r.file.nameWithoutExtension.replace(".", "_")
                    putString("additionalTestOutputFile_$suffix", r.file.absolutePath)
                }
            }

        // Report to instrumentation
        InstrumentationRegistry.getInstrumentation().sendStatus(2, bundle)
    }
}
