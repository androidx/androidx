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
package androidx.abbenchmarking

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.collections.getOrPut
import kotlin.collections.mapValues
import kotlin.text.toDoubleOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Contains data classes for JSON parsing and functions for handling files, such as reading
 * benchmark outputs and cleaning up temporary files.
 */
@Serializable private data class BenchmarkReport(val benchmarks: List<Benchmark>)

@Serializable
private data class Benchmark(val name: String, val className: String, val metrics: Metrics)

@Serializable private data class Metrics(val timeNs: TimeNs)

@Serializable private data class TimeNs(val runs: List<Double>)

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
 * Parses benchmark JSON files to extract the raw performance timing data for all tests.
 *
 * The Android Benchmark library produces JSON files with a specific structure. This function
 * navigates that structure to find the `runs` array for each benchmark test, aggregates all runs
 * from all files, and organizes them by benchmark name.
 *
 * @param jsonFiles List of benchmark result files to parse.
 * @return A map where each key is a benchmark test name (e.g., "myBenchmark_test") and the value is
 *   a list of all timing runs (in nanoseconds) collected for that test across all input files.
 */
private fun parseBenchmarkRuns(jsonFiles: List<File>): Map<String, List<Double>> {
    val allRuns = mutableMapOf<String, MutableList<Double>>()
    val jsonParser = Json { ignoreUnknownKeys = true }
    jsonFiles.forEach { jsonFile ->
        try {
            val jsonString = jsonFile.readText()
            val report = jsonParser.decodeFromString<BenchmarkReport>(jsonString)
            report.benchmarks.forEach { benchmark ->
                // Get the existing list for this benchmark, or create a new empty
                // list if it's the first time we've seen it. Then, add all the
                // new runs to that list.
                allRuns
                    .getOrPut(benchmark.name) { kotlin.collections.mutableListOf() }
                    .addAll(benchmark.metrics.timeNs.runs)
            }
        } catch (e: Exception) {
            System.err.println(
                "Warning: Could not decode or process JSON from file: $jsonFile. Error: ${e.message}"
            )
        }
    }
    return allRuns
}

/**
 * Saves or appends aggregated benchmark data to a structured CSV file.
 *
 * This function writes benchmark results to a CSV file with two columns: `benchmark_name` and
 * `timing`. If the file does not exist or is empty, it first writes a header row. Otherwise, it
 * appends the new data to the existing file. This is useful for aggregating results from multiple
 * benchmark runs into a single source.
 *
 * @param benchmarks A map where the key is the benchmark name and the value is a list of timing
 *   values.
 * @param outputFile The [File] to write the data to. The file will be created if it doesn't exist.
 */
private fun saveDataToFile(benchmarks: Map<String, List<Double>>, outputFile: File) {
    if (benchmarks.isEmpty()) return
    // Check if the file is new. If so, we need to write the header.
    val isNewFile = !outputFile.exists() || outputFile.length() == 0L
    // Use a buffered writer in append mode for efficient I/O.
    java.io.FileOutputStream(outputFile, true).writer(Charsets.UTF_8).buffered().use { writer ->
        if (isNewFile) {
            writer.write("benchmark_name,timing")
            writer.newLine()
        }

        benchmarks.forEach { (name, timings) ->
            timings.forEach { timing ->
                writer.write("$name,$timing")
                writer.newLine()
            }
        }
    }
}

/**
 * Orchestrates the full process of extracting and saving benchmark results.
 *
 * This function acts as a primary entry point for data processing. It takes a path to a directory
 * containing raw benchmark results, finds all JSON files within it, parses each file to extract the
 * timing data, aggregates all data points from all files into a single list, and finally saves that
 * consolidated list to a
 * - * specified output file.
 *
 * @param resultsPath The root directory containing the raw JSON benchmark results.
 * @param outputFile The final destination file for the consolidated, clean data.
 */
internal fun extractBenchmarkTestResult(resultsPath: String, outputFile: File) {
    // 1.Extract JSON files where benchmark test run data is stored
    val jsonFiles = findJsonFiles(File(resultsPath))
    if (jsonFiles.isEmpty()) {
        System.err.println("Warning: No .json files found in '$resultsPath'")
        return
    }
    // 2. Parse the JSON files and extract the benchmark run time
    val benchmarks = parseBenchmarkRuns(jsonFiles)
    // 3. Save the results to a file
    saveDataToFile(benchmarks, outputFile)
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

/**
 * Creates the output CSV file that stores processed benchmark data.
 *
 * This helper function creates a unique filename for a given Git branch, ensuring that the
 * extracted data from the baseline branch and the feature branch are saved to separate, predictable
 * files. This keeps the data organized for the final statistical comparison. The file is created in
 * the project's build directory
 * (`~/androidx-main/frameworks/support/development/ab-benchmarking/app/build/benchmark-results`).
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
 * Loads and parses processed benchmark data from a specified CSV file.
 *
 * It opens a CSV file containing `benchmark_name` and `timing` columns, groups the timings by
 * benchmark name, and returns them as a map for statistical analysis.
 *
 * @param outputFile The CSV file containing the benchmark data.
 * @return A map where the key is the benchmark name and the value is a [DoubleArray] of its
 *   timings.
 */
internal fun getBenchmarkDataFromOutputFile(outputFile: File): Map<String, DoubleArray> {
    if (!outputFile.exists()) {
        java.lang.System.err.println("Error: File not found at '${outputFile.absolutePath}'")
        return java.util.Collections.emptyMap()
    }
    return outputFile
        .readLines()
        .drop(1) // Skip the header row.
        .mapNotNull { line ->
            // Split each line into two parts at the first comma.
            val parts = line.split(',', limit = 2)
            if (parts.size == 2) {
                // Create a Pair of the benchmark name and its timing.
                val name = parts[0]
                val timing = parts[1].toDoubleOrNull()
                if (timing != null) {
                    name to timing
                } else {
                    null
                }
            } else {
                null
            }
        }
        // Group the pairs by the benchmark name.
        .groupBy({ it.first }, { it.second })
        // Convert the list of timings for each benchmark into a DoubleArray.
        .mapValues { it.value.toDoubleArray() }
}

/**
 * Discovers the device-specific output directory created by a benchmark run.
 *
 * @param module The Gradle module path (e.g., "compose:ui:ui-benchmark").
 * @return The File object for the device directory, or null if not found.
 */
internal fun discoverDeviceDirectory(module: String): File? {
    // The 'out' directory is a sibling of the 'frameworks' directory,
    // so we need to go up two level from the repoRoot (which is frameworks/support).
    val checkoutRoot = getGitRoot().parentFile.parentFile
    // 1. Construct the path to the parent 'connected' directory
    val connectedDirPath =
        Paths.get(
            checkoutRoot.absolutePath,
            "out/androidx",
            module.replace(":", "/"),
            "build/outputs/connected_android_test_additional_output/releaseAndroidTest/connected",
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
