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
package androidx.abbenchmarking.common

import androidx.abbenchmarking.util.getGitRoot
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Creates the output CSV file that stores processed benchmark data.
 *
 * This helper function creates a unique filename for a given Git branch, ensuring that the
 * extracted data from the baseline branch and the feature branch are saved to separate, predictable
 * files. This keeps the data organized for the final statistical comparison. The file is created in
 * the project's build directory
 * (`~/androidx-main/frameworks/support/development/ab-macrobenchmarking/app/build/benchmark-results`).
 *
 * @param rev The name of the Git branch / revision, used to construct the final filename (e.g.,
 *   "main" becomes "main.csv").
 * @return A [File] object representing the full path for the destination CSV file.
 */
internal fun createOutputFileForGitRevision(outputDirPath: Path, rev: String): File {
    Files.createDirectories(outputDirPath)
    return outputDirPath.resolve("$rev.csv").toFile()
}

/**
 * Recursively finds all files with a ".json" extension within a given directory.
 *
 * This is used to locate the raw benchmark result files that are generated in the build output
 * directory.
 *
 * @param directory The root directory to start the search from.
 * @return A list of [File] objects, each pointing to a found JSON file.
 */
internal fun findJsonFiles(directory: File): List<File> {
    return try {
        directory.walkTopDown().filter { it.isFile && it.extension == "json" }.toList()
    } catch (e: Exception) {
        System.err.println("Warning: Could not find or access directory '$directory'")
        emptyList()
    }
}

/**
 * Gets the file path for the output CSV file that stores processed benchmark data.
 *
 * The file is located in the project's build directory (`app/build/benchmark-results`).
 *
 * @param rev The name of the Git branch / revision, used to construct the final filename (e.g.,
 *   "main" becomes "main.csv").
 * @return A [Path] object representing the full path for the destination CSV file.
 */
internal fun getOutputFilePathForGitRevision(outputPath: Path, rev: String): Path {
    return outputPath.resolve("$rev.csv")
}

/**
 * Deletes any leftover processed data files from previous benchmark runs.
 *
 * This is a housekeeping function that should be called at the start of the script to ensure a
 * clean state. It scans the output directory and deletes any `.csv` data files, `_histogram.png`
 * plot files, and the `.metadata.json` file. This prevents data from past runs from contaminating
 * the current test results.
 *
 * @param outputPath The directory where output files are stored.
 */
internal fun cleanupPreviousOutputFilesIfAny(outputPath: Path) {
    val outputDir = outputPath.toFile()
    if (!outputDir.exists() || !outputDir.isDirectory) {
        println("No old output files found (directory does not exist).")
        return
    }

    var filesDeleted = false
    outputDir.listFiles()?.forEach { file ->
        if (
            file.extension == "csv" ||
                file.extension == "json" && file.name.endsWith(".metadata.json") ||
                file.extension == "png" && file.name.contains("histogram")
        ) {
            if (file.delete()) {
                filesDeleted = true
            }
        }
    }

    if (filesDeleted) {
        println("Cleaned up old output files.")
    } else {
        println("No old output files found.")
    }
}

/**
 * Discovers the device-specific output directory created by a benchmark run.
 *
 * @param module The Gradle module path (e.g., ":compose:integration-tests:macrobenchmark").
 * @param buildVariant The build variant to look for (e.g., "release", "releaseAndroidTest").
 * @return The File object for the device directory, or null if not found.
 */
internal fun discoverDeviceDirectory(module: String, buildVariant: String): File? {
    // The 'out' directory is a sibling of the 'frameworks' directory,
    // so we need to go up two level from the repoRoot (which is frameworks/support).
    val checkoutRoot = getGitRoot().parentFile.parentFile
    // 1. Construct the path to the parent 'connected' directory
    val connectedDirPath =
        Paths.get(
            checkoutRoot.absolutePath,
            "out/androidx",
            module.replace(":", "/"),
            "build/outputs/connected_android_test_additional_output/$buildVariant/connected",
        )
    val connectedDir = connectedDirPath.toFile()
    if (!connectedDir.exists() || !connectedDir.isDirectory) {
        System.err.println("Warning: 'connected' directory not found at: ${connectedDir.path}")
        return null
    }
    // 2. Find the first (and only) subdirectory inside the 'connected' folder
    val deviceDirs = connectedDir.listFiles { file -> file.isDirectory }
    if (deviceDirs.isNullOrEmpty()) {
        System.err.println(
            "Warning: No device output directories found inside: ${connectedDir.path}"
        )
        return null
    }
    // 3. In a CI environment, we expect only one. Return the first one found.
    val deviceDir = deviceDirs.first()
    println("DEBUG: Automatically discovered device path: ${deviceDir.name}")
    return deviceDir
}

internal fun getDefaultOutputDirPath(): Path {
    return Paths.get(
        getGitRoot().path,
        "development",
        "ab-benchmarking",
        "app",
        "build",
        "benchmark-results",
    )
}
