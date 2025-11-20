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

import androidx.abbenchmarking.*
import java.io.File
import java.nio.file.Path
import java.time.Instant
import kotlin.system.exitProcess
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default

/**
 * Main entry point for the A/B benchmarking tool. This file is responsible for parsing command-line
 * arguments and orchestrating the overall workflow.
 */
fun main(args: Array<String>) {
    // --- 1. Argument Parsing ---
    val parser = ArgParser("BenchmarkRunner")
    val revA by
        parser.argument(
            ArgType.String,
            fullName = "rev_a",
            description = "First branch / commit (e.g., 'main')",
        )
    val revB by
        parser.argument(
            ArgType.String,
            fullName = "rev_b",
            description = "Second branch / commit (e.g., a feature branch)",
        )
    val module by
        parser.argument(
            ArgType.String,
            fullName = "module",
            description =
                "Module containing the benchmark test class (e.g., 'compose:ui:ui-benchmark')",
        )
    val benchmarkTest by
        parser.argument(
            ArgType.String,
            fullName = "benchmark_test",
            description =
                "Fully qualified name of the test class and optionally the method.\n" +
                    "Can also include parameters for parameterized tests.\n" +
                    "Example: 'androidx.compose.ui.benchmark.ModifiersBenchmark#full[clickable_1x]",
        )
    val runCount by
        parser
            .option(
                ArgType.Int,
                fullName = "run_count",
                description = "Number of times to run the test on each git revision.",
            )
            .default(1)
    val iterationCount by
        parser
            .option(
                ArgType.Int,
                fullName = "iteration_count",
                description =
                    "Number of benchmark runs to perform  on each test run. Total number of measurements = iteration_count * run_count ",
            )
            .default(50)
    val serial by
        parser.option(
            ArgType.String,
            fullName = "serial",
            description =
                "The SERIAL of the device to run the tests on. This is an optional parameter if only one device is connected.",
        )
    val outputDirectoryPath by
        parser.option(
            ArgType.String,
            fullName = "output_path",
            description =
                "The path where output files will be stored, such as benchmark measurements, run metadata, and result plots.",
        )
    parser.parse(args)

    // --- 2. Pre-flight Checks ---
    val gitCommitHashA = resolveGitCommit(revA)
    val gitCommitHashB = resolveGitCommit(revB)
    val repoRoot = getGitRoot()
    println("DEBUG: Found repository root at: ${repoRoot.absolutePath}")
    if (!isGitStatusClean()) {
        System.err.println(
            "Git status is not clean. Please commit or stash your changes before running."
        )
        exitProcess(1)
    }
    val originalGitRevision = getCurrentGitRevision()
    println("Currently on rev '$originalGitRevision'. Starting benchmark process...")

    val connectedDevices = getConnectedDevices()
    val targetDeviceId =
        when {
            serial != null -> {
                if (connectedDevices.contains(serial)) {
                    serial
                } else {
                    java.lang.System.err.println("Error: Device with ID '$serial' not found.")
                    exitProcess(1)
                }
            }
            connectedDevices.size == 1 -> connectedDevices[0]
            connectedDevices.isEmpty() -> {
                java.lang.System.err.println("Error: No devices connected.")
                exitProcess(1)
            }
            else -> {
                java.lang.System.err.println(
                    "Error: Multiple devices connected. Please specify one with --serial."
                )
                exitProcess(1)
            }
        }

    // --- 3. Test Execution ---
    val executionTimestamp = Instant.now().toString()
    val outputPath = outputDirectoryPath?.let { Path.of(it) } ?: getDefaultOutputDirPath()
    cleanupPreviousOutputFilesIfAny(outputPath)
    try {
        repeat(runCount) { i ->
            // Run tests on git rev A
            println(
                "\nRunning test ${i + 1}/$runCount on git rev '$revA' (commit $gitCommitHashA)..."
            )
            checkoutAndRunTest(
                outputPath = outputPath,
                gitRevision = revA,
                gitRevisionHash = gitCommitHashA,
                module = module,
                benchmarkTest = benchmarkTest,
                iterationCount = iterationCount,
                targetDeviceId = targetDeviceId,
            )
            // Run tests on git rev B
            println(
                "\nRunning test ${i + 1}/$runCount on git rev '$revB' (commit $gitCommitHashB)..."
            )
            checkoutAndRunTest(
                outputPath = outputPath,
                gitRevision = revB,
                gitRevisionHash = gitCommitHashB,
                module = module,
                benchmarkTest = benchmarkTest,
                iterationCount = iterationCount,
                targetDeviceId = targetDeviceId,
            )
        }
    } catch (e: Exception) {
        System.err.println("Error: Test execution failed. Aborting.")
        e.printStackTrace()
    } finally {
        // Return to the original git revision
        println("\n--- Tests complete. Returning to original git rev: $originalGitRevision ---")
        checkoutGitRevision(originalGitRevision)
    }
    // --- 4. Data Analysis & Reporting ---
    val benchmarkRevA =
        getBenchmarkDataFromOutputFile(getOutputFilePathForGitRevision(outputPath, revA).toFile())
    val benchmarkRevB =
        getBenchmarkDataFromOutputFile(getOutputFilePathForGitRevision(outputPath, revB).toFile())
    if (benchmarkRevA.isEmpty() || benchmarkRevB.isEmpty()) {
        System.err.println(
            "Error: One or both of the result sets are empty. Cannot perform analysis."
        )
        exitProcess(1)
    }

    // Create metadata file
    val inputParameters =
        mapOf(
            "module" to module,
            "benchmark_test" to benchmarkTest,
            "run_count" to runCount.toString(),
            "iteration_count" to iterationCount.toString(),
            "serial" to (serial ?: "Not Provided"),
            "output_path" to (outputDirectoryPath ?: getDefaultOutputDirPath().toString()),
        )
    val deviceDir = discoverDeviceDirectory(module)
    val deviceInfo =
        if (deviceDir != null) {
            val jsonFiles = findJsonFiles(File(deviceDir.absolutePath))
            getDeviceInfoFromOutputFile(jsonFiles.first())
        } else null
    val metadata =
        Metadata(
            executionTimestamp = executionTimestamp,
            revA = RevInfo(name = revA, commit = gitCommitHashA),
            revB = RevInfo(name = revB, commit = gitCommitHashB),
            deviceInfo = deviceInfo,
            inputParameters = inputParameters,
        )
    createMetadataFile(metadata, outputPath)

    // Calculate and print statistics
    benchmarkRevA.forEach { (benchmarkName, benchmarkData) ->
        val dataA = benchmarkData
        val dataB = benchmarkRevB[benchmarkName] ?: doubleArrayOf()
        if (dataA.isNotEmpty() && dataB.isNotEmpty()) {
            val stats = calculateStatistics(dataA, dataB)
            printSummary(benchmarkName, stats)
            printCsvOutput(benchmarkName, stats)
            createHistogramPlot(benchmarkName, dataA, dataB, outputPath)
        } else {
            println(
                "\nSkipping comparison for '$benchmarkName': data is missing or empty in one of the revisions."
            )
        }
    }
}
