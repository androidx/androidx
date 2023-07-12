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
object Outputs {

    private val formatter: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")

    /**
     * The intended output directory that respects the `additionalTestOutputDir`.
     */
    val outputDirectory: File

    /**
     * The usable output directory, given permission issues with `adb shell` on Android R.
     * Both the app and the shell have access to this output folder.
     *
     * This dir can be read/written by app
     * This dir can be read by shell (see [forceFilesForShellAccessible] for API 21/22!)
     */
    val dirUsableByAppAndShell: File

    /**
     * Any file created by this process for the shell to use must be explicitly made filesystem
     * globally readable, as prior to API 23 the shell didn't have access by default.
     */
    val forceFilesForShellAccessible: Boolean = Build.VERSION.SDK_INT in 21..22

    init {
        // Be explicit about the TimeZone for stable formatting
        formatter.timeZone = TimeZone.getTimeZone("UTC")

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        @SuppressLint("NewApi")
        dirUsableByAppAndShell = when {
            Build.VERSION.SDK_INT >= 29 -> {
                // On Android Q+ we are using the media directory because that is
                // the directory that the shell has access to. Context: b/181601156
                // Additionally, Benchmarks append user space traces to the ones produced
                // by the Macro Benchmark run; and that is a lot simpler to do if we use the
                // Media directory. (b/216588251)
                context.getFirstMountedMediaDir()
            }

            Build.VERSION.SDK_INT <= 22 -> {
                // prior to API 23, shell didn't have access to externalCacheDir
                context.cacheDir
            }

            else -> context.externalCacheDir
        } ?: throw IllegalStateException(
            "Unable to select a directory for writing files, " +
                "additionalTestOutputDir argument required to declare output dir."
        )

        if (forceFilesForShellAccessible) {
            // By default, shell doesn't have access to app dirs on 21/22 so we need to modify
            // this so that the shell can output here too
            dirUsableByAppAndShell.setReadable(true, false)
            dirUsableByAppAndShell.setWritable(true, false)
            dirUsableByAppAndShell.setExecutable(true, false)
        }

        Log.d(BenchmarkState.TAG, "Usable output directory: $dirUsableByAppAndShell")

        outputDirectory = Arguments.additionalTestOutputDir?.let { File(it) }
            ?: dirUsableByAppAndShell

        Log.d(BenchmarkState.TAG, "Output Directory: $outputDirectory")
        outputDirectory.mkdirs()

        // Clear all the existing files in the output directories
        deleteFiles { true }
    }

    /**
     * Create a benchmark output [File] to write to.
     *
     * This method handles reporting files to `InstrumentationStatus` to request copy,
     * writing them in the desired output directory, and handling shell access issues on Android R.
     *
     * @return The absolute path of the output [File].
     */
    fun writeFile(
        fileName: String,
        reportKey: String,
        reportOnRunEndOnly: Boolean = false,
        block: (file: File) -> Unit,
    ): String {
        val sanitizedName = sanitizeFilename(fileName)
        val destination = File(outputDirectory, sanitizedName)

        // We override the `additionalTestOutputDir` argument.
        // Context: b/181601156
        val file = File(dirUsableByAppAndShell, sanitizedName)
        block.invoke(file)
        check(file.exists()) { "File doesn't exist!" }

        if (dirUsableByAppAndShell != outputDirectory) {
            // We need to copy files over anytime `dirUsableByAppAndShell` is different from
            // `outputDirectory`.
            Log.d(BenchmarkState.TAG, "Copying $file to $destination")
            file.copyTo(destination, overwrite = true)
        }

        InstrumentationResults.reportAdditionalFileToCopy(
            key = reportKey,
            absoluteFilePath = destination.absolutePath,
            reportOnRunEndOnly = reportOnRunEndOnly
        )
        return destination.absolutePath
    }

    fun sanitizeFilename(filename: String): String {
        return filename
            .replace(" ", "")
            .replace("(", "[")
            .replace(")", "]")
    }

    fun testOutputFile(filename: String): File {
        return File(outputDirectory, filename)
    }

    fun dateToFileName(date: Date = Date()): String {
        return formatter.format(date)
    }

    fun relativePathFor(path: String): String {
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

    fun deleteFiles(filterBlock: (File) -> (Boolean)) {
        listOf(outputDirectory, dirUsableByAppAndShell)
            .flatMap { it.listFiles(filterBlock)?.asList() ?: emptyList() }
            .forEach { it.delete() }
    }
}
