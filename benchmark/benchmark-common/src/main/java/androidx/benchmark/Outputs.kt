/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object Outputs {

    private val formatter: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")

    /**
     * The intended output directory that respects the `additionalTestOutputDir`.
     */
    public val outputDirectory: File

    /**
     * The usable output directory, given permission issues with `adb shell` on Android R.
     * Both the app and the shell have access to this output folder.
     */
    public val dirUsableByAppAndShell: File

    init {
        // Be explicit about the TimeZone for stable formatting
        formatter.timeZone = TimeZone.getTimeZone("UTC")

        @Suppress("DEPRECATION")
        @SuppressLint("NewApi")
        dirUsableByAppAndShell = when {
            Build.VERSION.SDK_INT >= 29 -> {
                // On Android Q+ we are using the media directory because that is
                // the directory that the shell has access to. Context: b/181601156
                // Additionally, Benchmarks append user space traces to the ones produced
                // by the Macro Benchmark run; and that is a lot simpler to do if we use the
                // Media directory. (b/216588251)
                InstrumentationRegistry.getInstrumentation().context.getFirstMountedMediaDir()
            }
            Build.VERSION.SDK_INT <= 22 -> {
                // prior to API 23, shell didn't have access to externalCacheDir
                InstrumentationRegistry.getInstrumentation().context.cacheDir
            }
            else -> InstrumentationRegistry.getInstrumentation().context.externalCacheDir
        } ?: throw IllegalStateException(
            "Unable to select a directory for writing files, " +
                "additionalTestOutputDir argument required to declare output dir."
        )

        Log.d(BenchmarkState.TAG, "Usable output directory: $dirUsableByAppAndShell")

        outputDirectory = Arguments.additionalTestOutputDir?.let { File(it) }
            ?: dirUsableByAppAndShell

        Log.d(BenchmarkState.TAG, "Output Directory: $outputDirectory")
    }

    /**
     * Create a benchmark output [File] to write to.
     *
     * This method handles reporting files to `InstrumentationStatus` to request copy,
     * writing them in the desired output directory, and handling shell access issues on Android R.
     *
     * @return The absolute path of the output [File].
     */
    public fun writeFile(
        fileName: String,
        reportKey: String,
        reportOnRunEndOnly: Boolean = false,
        block: (file: File) -> Unit,
    ): String {
        val sanitizedName = sanitizeFilename(fileName)

        // We need to copy files over anytime `dirUsableByAppAndShell` is different from
        // `outputDirectory`.
        val override = dirUsableByAppAndShell != outputDirectory
        // We override the `additionalTestOutputDir` argument.
        // Context: b/181601156
        val file = File(dirUsableByAppAndShell, sanitizedName)
        try {
            block.invoke(file)
        } finally {
            var destination = file
            if (override) {
                // This respects the `additionalTestOutputDir` argument.
                val actualOutputDirectory = outputDirectory
                destination = File(actualOutputDirectory, sanitizedName)
                Log.d(BenchmarkState.TAG, "Copying $file to $destination")
                try {
                    destination.mkdirs()
                    file.copyTo(destination, overwrite = true)
                } catch (exception: Throwable) {
                    // This can happen when `additionalTestOutputDir` being passed in cannot
                    // be written to. The shell does not have permissions to do the necessary
                    // setup, and this can cause `adb pull` to fail.
                    val message = """
                        Unable to copy files to ${destination.absolutePath}.
                        Please pull the Macrobenchmark results manually by using:
                        adb pull ${file.absolutePath}
                    """.trimIndent()
                    Log.e(BenchmarkState.TAG, message, exception)

                    // TODO(b/227510293): return failure/null to signal file isn't readable
                    destination = file
                }
            }
            InstrumentationResults.reportAdditionalFileToCopy(
                key = reportKey,
                absoluteFilePath = destination.absolutePath,
                reportOnRunEndOnly = reportOnRunEndOnly
            )
            return destination.absolutePath
        }
    }

    public fun sanitizeFilename(filename: String): String {
        return filename
            .replace(" ", "")
            .replace("(", "[")
            .replace(")", "]")
    }

    public fun testOutputFile(filename: String): File {
        return File(outputDirectory, filename)
    }

    public fun dateToFileName(date: Date = Date()): String {
        return formatter.format(date)
    }

    public fun relativePathFor(path: String): String {
        val hasOutputDirectoryPrefix = path.startsWith(outputDirectory.absolutePath)
        val relativePath = when {
            hasOutputDirectoryPrefix -> path.removePrefix("${outputDirectory.absolutePath}/")
            else -> path.removePrefix("${dirUsableByAppAndShell.absolutePath}/")
        }
        check(relativePath != path) {
            "$relativePath == $path"
        }
        return relativePath
    }
}
