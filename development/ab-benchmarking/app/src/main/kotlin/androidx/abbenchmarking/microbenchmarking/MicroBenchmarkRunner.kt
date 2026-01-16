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
package androidx.abbenchmarking.microbenchmarking

import androidx.abbenchmarking.common.*
import androidx.abbenchmarking.util.checkoutGitRevision
import androidx.abbenchmarking.util.getCurrentGitRevision
import androidx.abbenchmarking.util.getTargetDeviceId
import androidx.abbenchmarking.util.resolveGitCommit
import java.nio.file.Path
import java.time.Instant
import kotlin.collections.isNotEmpty
import kotlin.system.exitProcess
import kotlinx.cli.ArgParser

/**
 * Main entry point for the A/B benchmarking tool. This file is responsible for parsing command-line
 * arguments and orchestrating the overall workflow.
 */
fun main(args: Array<String>) {
    // --- 1. Argument Parsing ---
    val parser = ArgParser("BenchmarkRunner")

    val benchmarkArgs = parser.parseBenchmarkArguments(args)

    // --- 2. Pre-flight Checks ---
    val gitCommitHashA = resolveGitCommit(benchmarkArgs.revA)
    val gitCommitHashB = resolveGitCommit(benchmarkArgs.revB)

    performGitPreflightChecks()

    val originalGitRevision = getCurrentGitRevision()
    println("Currently on rev '$originalGitRevision'. Starting benchmark process...")

    val targetDeviceId =
        try {
            getTargetDeviceId(benchmarkArgs.serial)
        } catch (e: RuntimeException) {
            System.err.println(e.message)
            exitProcess(1)
        }

    // --- 3. Test Execution ---
    val executionTimestamp = Instant.now().toString()
    val outputPath =
        benchmarkArgs.outputDirectoryPath?.let { Path.of(it) } ?: getDefaultOutputDirPath()
    cleanupPreviousOutputFilesIfAny(outputPath)
    try {
        repeat(benchmarkArgs.runCount) { i ->
            // Run tests on git rev A
            println(
                "\nRunning test ${i + 1}/${benchmarkArgs.runCount} on git rev '${benchmarkArgs.revA}' (commit $gitCommitHashA)..."
            )
            checkoutAndRunTest(
                outputPath = outputPath,
                gitRevision = benchmarkArgs.revA,
                gitRevisionHash = gitCommitHashA,
                module = benchmarkArgs.module,
                benchmarkTest = benchmarkArgs.benchmarkTest,
                iterationCount = benchmarkArgs.iterationCount ?: 50,
                targetDeviceId = targetDeviceId,
                buildVariant = BuildVariant.RELEASE_ANDROID_TEST,
                discoverDeviceDirectory = ::discoverDeviceDirectory,
                extractBenchmarkTestResult = ::extractBenchmarkTestResult,
            )
            // Run tests on git rev B
            println(
                "\nRunning test ${i + 1}/${benchmarkArgs.runCount} on git rev '${benchmarkArgs.revB}' (commit $gitCommitHashB)..."
            )
            checkoutAndRunTest(
                outputPath = outputPath,
                gitRevision = benchmarkArgs.revB,
                gitRevisionHash = gitCommitHashB,
                module = benchmarkArgs.module,
                benchmarkTest = benchmarkArgs.benchmarkTest,
                iterationCount = benchmarkArgs.iterationCount ?: 50,
                targetDeviceId = targetDeviceId,
                buildVariant = BuildVariant.RELEASE_ANDROID_TEST,
                discoverDeviceDirectory = ::discoverDeviceDirectory,
                extractBenchmarkTestResult = ::extractBenchmarkTestResult,
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
        getBenchmarkDataFromOutputFile(
            getOutputFilePathForGitRevision(outputPath, benchmarkArgs.revA).toFile()
        )
    val benchmarkRevB =
        getBenchmarkDataFromOutputFile(
            getOutputFilePathForGitRevision(outputPath, benchmarkArgs.revB).toFile()
        )
    if (benchmarkRevA.isEmpty() || benchmarkRevB.isEmpty()) {
        System.err.println(
            "Error: One or both of the result sets are empty. Cannot perform analysis."
        )
        exitProcess(1)
    }

    // Create metadata file
    createMetadataFile(
        benchmarkArgs,
        outputPath,
        gitCommitHashA,
        gitCommitHashB,
        executionTimestamp,
        BuildVariant.RELEASE_ANDROID_TEST,
    )

    // Calculate and print statistics
    benchmarkRevA.forEach { (benchmarkName, benchmarkData) ->
        val dataA = benchmarkData
        val dataB = benchmarkRevB[benchmarkName] ?: doubleArrayOf()
        if (dataA.isNotEmpty() && dataB.isNotEmpty()) {
            val stats = calculateStatistics(dataA, dataB)
            printSummary(benchmarkName, stats)
            printCsvOutput(benchmarkName, stats)
            println("\n--- Graphical Plot ---")
            createHistogramPlot(benchmarkName, dataA, dataB, outputPath, "timing")
        } else {
            println(
                "\nSkipping comparison for '$benchmarkName': data is missing or empty in one of the revisions."
            )
        }
    }
}
