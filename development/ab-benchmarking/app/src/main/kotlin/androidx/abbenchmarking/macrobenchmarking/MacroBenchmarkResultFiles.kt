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

package androidx.abbenchmarking.macrobenchmarking

import androidx.abbenchmarking.common.findJsonFiles
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import kotlin.collections.emptyMap
import kotlin.collections.flatten
import kotlin.collections.getOrPut
import kotlin.collections.mapValues
import kotlin.collections.mutableListOf
import kotlin.text.toDoubleOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVPrinter

/**
 * Contains data classes for JSON parsing and functions for handling files, such as reading
 * benchmark outputs and cleaning up temporary files.
 */
@Serializable private data class BenchmarkReport(val benchmarks: List<Benchmark>)

@Serializable
private data class Benchmark(
    val name: String,
    val className: String,
    val metrics: Map<String, Metrics>,
    val sampledMetrics: Map<String, SampledMetrics> = emptyMap(),
)

@Serializable private data class Metrics(val runs: List<Double>)

@Serializable private data class SampledMetrics(val runs: List<List<Double>>)

private data class BenchmarkRecord(
    val benchmarkName: String,
    val metricName: String,
    val metricValue: Double,
)

/**
 * Parses benchmark JSON files to extract the raw performance metricValue data for all tests.
 *
 * The Android Benchmark library produces JSON files with a specific structure. This function
 * navigates that structure to find the `runs` array for each benchmark test, aggregates all runs
 * from all files, and organizes them by benchmark name.
 *
 * @param jsonFiles List of benchmark result files to parse.
 * @return A map where each key is a benchmark test name (e.g., "myBenchmark_test") and the value is
 *   a list of all metricValue runs collected for that test across all input files.
 */
internal fun parseBenchmarkRuns(jsonFiles: List<File>): Map<String, Map<String, List<Double>>> {
    val allRuns = mutableMapOf<String, MutableMap<String, MutableList<Double>>>()
    val jsonParser = Json { ignoreUnknownKeys = true }
    jsonFiles.forEach { jsonFile ->
        try {
            val jsonString = jsonFile.readText()
            val report = jsonParser.decodeFromString<BenchmarkReport>(jsonString)
            report.benchmarks.forEach { benchmark ->
                val benchmarkMetrics = allRuns.getOrPut(benchmark.name) { mutableMapOf() }
                benchmark.metrics.forEach { (metricName, metricRun) ->
                    // Get the existing list for this benchmark, or create a new empty
                    // list if it's the first time we've seen it. Then, add all the
                    // new runs to that list.
                    benchmarkMetrics.getOrPut(metricName) { mutableListOf() }.addAll(metricRun.runs)
                }

                benchmark.sampledMetrics.forEach { (metricName, sampledMetricRun) ->
                    // The 'runs' in sampledMetrics is a List<List<Double>>. Flatten it.
                    val flattenedRuns = sampledMetricRun.runs.flatten()
                    benchmarkMetrics.getOrPut(metricName) { mutableListOf() }.addAll(flattenedRuns)
                }
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
 * This function writes benchmark results to a CSV file with three columns: `benchmark_name`,
 * `metric_name`, and `metric_value`. If the file does not exist or is empty, it first writes a
 * header row. Otherwise, it appends the new data to the existing file. This is useful for
 * aggregating results from multiple benchmark runs into a single source.
 *
 * @param benchmarks A nested map where the outer key is the benchmark name (e.g., "startup"), the
 *   inner key is the metric name (e.g., "timeToInitialDisplayMs"), and the value is a list of all
 *   measurement values for that metric.
 * @param outputFile The [File] to write the data to. The file will be created if it doesn't exist.
 */
private fun saveDataToFile(benchmarks: Map<String, Map<String, List<Double>>>, outputFile: File) {
    if (benchmarks.isEmpty()) return
    // Check if the file is new. If so, we need to write the header.
    val isNewFile = !outputFile.exists() || outputFile.length() == 0L

    val csvFormat = CSVFormat.DEFAULT.builder().build()

    FileWriter(outputFile, true).use { fileWriter ->
        CSVPrinter(fileWriter, csvFormat).use { csvPrinter ->
            if (isNewFile) {
                csvPrinter.printRecord("benchmark_name", "metric_name", "metric_value")
            }
            benchmarks.forEach { (benchmarkName, metrics) ->
                metrics.forEach { (metricName, metricValues) ->
                    metricValues.forEach { metricValue ->
                        csvPrinter.printRecord(benchmarkName, metricName, metricValue)
                    }
                }
            }
        }
    }
}

/**
 * Orchestrates the full process of extracting and saving benchmark results.
 *
 * This function acts as a primary entry point for data processing. It takes a path to a directory
 * containing raw benchmark results, finds all JSON files within it, parses each file to extract the
 * metricValue data, aggregates all data points from all files into a single list, and finally saves
 * that consolidated list to a
 * - * specified output file.
 *
 * @param resultsPath The root directory containing the raw JSON benchmark results.
 * @param outputFile The final destination file for the consolidated, clean data.
 */
internal fun extractBenchmarkTestResult(resultsPath: String, outputFile: File) {
    val jsonFiles = findJsonFiles(File(resultsPath))
    if (jsonFiles.isEmpty()) {
        System.err.println("Warning: No .json files found in '$resultsPath'")
        return
    }
    val benchmarks = parseBenchmarkRuns(jsonFiles)
    saveDataToFile(benchmarks, outputFile)
}

/**
 * Loads and parses processed benchmark data from a specified CSV file.
 *
 * It opens a CSV file containing `benchmark_name`, `metric_name`, and `metric_value` columns. It
 * then groups the metricValues first by benchmark name and then by metric name, returning them as a
 * nested map suitable for statistical analysis.
 *
 * @param outputFile The CSV file containing the benchmark data.
 * @return A nested map where the outer key is the benchmark name (e.g., "startup"), the inner key
 *   is the metric name (e.g., "timeToInitialDisplayMs"), and the value is a [DoubleArray] of all
 *   its metricValue measurements.
 */
internal fun getBenchmarkDataFromOutputFile(
    outputFile: File
): Map<String, Map<String, DoubleArray>> {
    if (!outputFile.exists()) {
        System.err.println("Error: File not found at '${outputFile.absolutePath}'")
        return emptyMap()
    }

    val allRecords = mutableListOf<BenchmarkRecord>()

    try {
        FileReader(outputFile).use { reader ->
            val csvParser =
                CSVParser(
                    reader,
                    CSVFormat.DEFAULT.builder().setHeader().setQuote('"').setTrim(true).build(),
                )

            for (record in csvParser) {
                val benchmarkName = record.get("benchmark_name")
                val metricName = record.get("metric_name")
                val metricValue = record.get("metric_value").toDoubleOrNull()

                if (metricValue != null) {
                    allRecords.add(BenchmarkRecord(benchmarkName, metricName, metricValue))
                } else {
                    System.err.println("Warning: Could not parse metric value from record: $record")
                }
            }
        }
    } catch (e: Exception) {
        System.err.println("Error parsing CSV file '${outputFile.path}': ${e.message}")
        return emptyMap()
    }

    return allRecords
        .groupBy { it.benchmarkName } // Group by benchmark_name
        .mapValues { (_, triplesForBenchmark) ->
            triplesForBenchmark
                .groupBy { it.metricName } // Group by metric_name
                .mapValues { (_, triplesForMetric) ->
                    triplesForMetric.map { it.metricValue }.toDoubleArray()
                }
        }
}
