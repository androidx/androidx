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

import androidx.abbenchmarking.common.ConfidenceInterval
import androidx.abbenchmarking.common.calculateBCaCIMedianDifference
import androidx.abbenchmarking.common.createHistogramPlot
import kotlin.text.isNotEmpty
import kotlin.text.padEnd
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.inference.MannWhitneyUTest

/**
 * Contains the data class for storing statistical results and all functions related to performing
 * and printing the statistical analysis.
 */
data class StatisticsResult(
    // Dataset 1 (Branch A)
    val count1: Int,
    val min1: Double,
    val max1: Double,
    val mean1: Double,
    val median1: Double,
    val p90_1: Double,
    val p95_1: Double,
    val p99_1: Double,
    val std1: Double,

    // Dataset 2 (Branch B)
    val count2: Int,
    val min2: Double,
    val max2: Double,
    val mean2: Double,
    val median2: Double,
    val p90_2: Double,
    val p95_2: Double,
    val p99_2: Double,
    val std2: Double,

    // Difference Metrics
    val minDiff: Double,
    val minDiffPercent: Double,
    val maxDiff: Double,
    val maxDiffPercent: Double,
    val meanDiff: Double,
    val meanDiffPercent: Double,
    val medianDiff: Double,
    val medianDiffPercent: Double,

    // Statistical Significance
    val pValue: Double,
    val medianDiffCI: ConfidenceInterval,
    val medianDiffCIPercent: ConfidenceInterval,
)

fun calculateAllMetricsStatistics(
    metricsA: Map<String, DoubleArray>,
    metricsB: Map<String, DoubleArray>,
): Map<String, StatisticsResult> {
    // Use mapNotNull to iterate, calculate, and collect results in a concise way.
    return metricsA
        .mapNotNull { (metricName, dataA) ->
            // Find the corresponding data for the same metric in revision B.
            val dataB = metricsB[metricName]

            // Proceed only if data exists for the metric in both revisions.
            if (dataB != null && dataA.isNotEmpty() && dataB.isNotEmpty()) {
                // Call the existing function to calculate stats for this single metric.
                val stats = calculateSingleMetricStatistics(dataA, dataB)
                // Return a Pair of the metric name and its stats to be converted into a map.
                metricName to stats
            } else {
                // If data is missing in revision B, skip this metric.
                null
            }
        }
        .toMap()
}

fun calculateSingleMetricStatistics(data1: DoubleArray, data2: DoubleArray): StatisticsResult {
    val stats1 = DescriptiveStatistics(data1)
    val stats2 = DescriptiveStatistics(data2)
    // Counts
    val n1 = stats1.n
    val n2 = stats2.n
    // Means
    val mean1 = stats1.mean
    val mean2 = stats2.mean
    val meanDiff = mean2 - mean1
    val meanDiffPercent = if (mean1 != 0.0) (meanDiff / mean1) * 100 else 0.0
    // Median
    val median1 = stats1.getPercentile(50.0)
    val median2 = stats2.getPercentile(50.0)
    val medianDiff = median2 - median1
    val medianDiffPercent = if (median1 != 0.0) (medianDiff / median1) * 100 else 0.0
    // Percentiles
    val p90_1 = stats1.getPercentile(90.0)
    val p95_1 = stats1.getPercentile(95.0)
    val p99_1 = stats1.getPercentile(99.0)
    val p90_2 = stats2.getPercentile(90.0)
    val p95_2 = stats2.getPercentile(95.0)
    val p99_2 = stats2.getPercentile(99.0)
    // Mins
    val min1 = stats1.min
    val min2 = stats2.min
    val minDiff = min2 - min1
    val minDiffPercent = if (min1 != 0.0) (minDiff / min1) * 100 else 0.0
    // Max
    val max1 = stats1.max
    val max2 = stats2.max
    val maxDiff = max2 - max1
    val maxDiffPercent = if (max1 != 0.0) (maxDiff / max1) * 100 else 0.0
    // Standard Deviation
    val std1 = stats1.standardDeviation
    val std2 = stats2.standardDeviation
    // Perform Mann-Whitney U test.
    // This is a non-parametric test that checks if there's a significant difference
    // between two independent samples. It's used here to compare the distributions of the
    // two benchmark datasets.
    //
    // How to interpret the p-value:
    // - A small p-value (typically < 0.05) suggests that the observed difference between
    //   the datasets is statistically significant, meaning it's unlikely to have occurred
    //   by chance.
    // - A large p-value (>= 0.05) suggests that there is no statistically significant
    //   difference between the datasets.
    val mannWhitneyUTest = MannWhitneyUTest()
    val pValue = mannWhitneyUTest.mannWhitneyUTest(data1, data2)

    // Calculate BCa Confidence Interval for Median Difference
    val medianDiffCI = calculateBCaCIMedianDifference(data1, data2, medianDiff)
    val ciLowerPercent = if (median1 != 0.0) (medianDiffCI.lower / median1) * 100 else 0.0
    val ciUpperPercent = if (median1 != 0.0) (medianDiffCI.upper / median1) * 100 else 0.0
    val medianDiffCIPercent = ConfidenceInterval(ciLowerPercent, ciUpperPercent)

    return StatisticsResult(
        count1 = n1.toInt(),
        min1 = min1,
        max1 = max1,
        mean1 = mean1,
        median1 = median1,
        p90_1 = p90_1,
        p95_1 = p95_1,
        p99_1 = p99_1,
        std1 = std1,
        count2 = n2.toInt(),
        min2 = min2,
        max2 = max2,
        mean2 = mean2,
        median2 = median2,
        p90_2 = p90_2,
        p95_2 = p95_2,
        p99_2 = p99_2,
        std2 = std2,
        minDiff = minDiff,
        minDiffPercent = minDiffPercent,
        maxDiff = maxDiff,
        maxDiffPercent = maxDiffPercent,
        meanDiff = meanDiff,
        meanDiffPercent = meanDiffPercent,
        medianDiff = medianDiff,
        medianDiffPercent = medianDiffPercent,
        pValue = pValue,
        medianDiffCI = medianDiffCI,
        medianDiffCIPercent = medianDiffCIPercent,
    )
}

/**
 * Prints a comprehensive statistical summary for all metrics within a benchmark.
 *
 * This function acts as a parent, printing a main header for the benchmark and then calling a
 * helper function to display the detailed comparison for each individual metric.
 *
 * @param benchmarkName The name of the benchmark test (e.g., "startup").
 * @param allStats A map where the key is the metric name (e.g., "timeToInitialDisplayMs") and the
 *   value is the calculated [StatisticsResult] for that metric.
 */
internal fun printSummary(benchmarkName: String, allStats: Map<String, StatisticsResult>) {
    println("\n================================================================================")
    println("          Statistical Summary for Benchmark: $benchmarkName")
    println("================================================================================")

    if (allStats.isEmpty()) {
        println("No metrics with comparable data found for this benchmark.")
        return
    }

    allStats.forEach { (metricName, stats) -> printSingleMetricSummary(metricName, stats) }
}

fun getUnitFromMetricName(name: String): String {
    return when {
        name.endsWith("Ms") -> "ms"
        name.endsWith("Uw") -> "uW"
        name.endsWith("Uws") -> "uWs"
        name.endsWith("Kb") -> "Kb"
        else -> ""
    }
}

/**
 * Prints a human-readable statistical summary to the console.
 *
 * @param metricName The name of the test being summarized.
 * @param stats The calculated statistical results.
 */
internal fun printSingleMetricSummary(metricName: String, stats: StatisticsResult) {
    val unit = getUnitFromMetricName(metricName)

    val unitLabel = if (unit.isNotEmpty()) " ($unit)" else ""
    val diffUnitLabel = if (unit.isNotEmpty()) " $unit" else ""

    val labelColumnWidth = 25
    val dataColumnWidth = 20

    println("\n--- Comparison for: $metricName ---")
    println(
        "".padEnd(labelColumnWidth) +
            " | ${"Dataset 1 (Branch A)".padEnd(dataColumnWidth)} | Dataset 2 (Branch B)"
    )
    println("----------------------------------------------------------------")
    println(
        "${"Count".padEnd(labelColumnWidth)} | ${stats.count1.toString().padEnd(dataColumnWidth)} | ${stats.count2}"
    )
    println(
        "${("Min" + unitLabel).padEnd(labelColumnWidth)} | ${"%.2f".format(stats.min1).padEnd(dataColumnWidth)} | ${"%.2f".format(stats.min2)}"
    )
    println(
        "${("Mean" + unitLabel).padEnd(labelColumnWidth)} | ${"%.2f".format(stats.mean1).padEnd(dataColumnWidth)} | ${"%.2f".format(stats.mean2)}"
    )
    println(
        "${("Max" + unitLabel).padEnd(labelColumnWidth)} | ${"%.2f".format(stats.max1).padEnd(dataColumnWidth)} | ${"%.2f".format(stats.max2)}"
    )
    println(
        "${("Median" + unitLabel).padEnd(labelColumnWidth)} | ${"%.2f".format(stats.median1).padEnd(dataColumnWidth)} | ${"%.2f".format(stats.median2)}"
    )
    if (metricName == "frameDurationCpuMs" || metricName == "frameOverrunMs") {
        println(
            "${("P90" + unitLabel).padEnd(labelColumnWidth)} | ${"%.2f".format(stats.p90_1).padEnd(dataColumnWidth)} | ${"%.2f".format(stats.p90_2)}"
        )
        println(
            "${("P95" + unitLabel).padEnd(labelColumnWidth)} | ${"%.2f".format(stats.p95_1).padEnd(dataColumnWidth)} | ${"%.2f".format(stats.p95_2)}"
        )
        println(
            "${("P99" + unitLabel).padEnd(labelColumnWidth)} | ${"%.2f".format(stats.p99_1).padEnd(dataColumnWidth)} | ${"%.2f".format(stats.p99_2)}"
        )
    }
    println(
        "${("Std. Dev." + unitLabel).padEnd(labelColumnWidth)} | ${"%.2f".format(stats.std1).padEnd(dataColumnWidth)} | ${"%.2f".format(stats.std2)}"
    )
    println(
        "${"Min Difference:".padEnd(labelColumnWidth)} | ${"%.2f".format(stats.minDiff)}$diffUnitLabel (${"%.2f".format(stats.minDiffPercent)}%)"
    )
    println(
        "${"Mean Difference:".padEnd(labelColumnWidth)} | ${"%.2f".format(stats.meanDiff)}$diffUnitLabel (${"%.2f".format(stats.meanDiffPercent)}%)"
    )
    println(
        "${"Max Difference:".padEnd(labelColumnWidth)} | ${"%.2f".format(stats.maxDiff)}$diffUnitLabel (${"%.2f".format(stats.maxDiffPercent)}%)"
    )
    println(
        "${"Median Difference:".padEnd(labelColumnWidth)} | ${"%.2f".format(stats.medianDiff)}$diffUnitLabel (${"%.2f".format(stats.medianDiffPercent)}%)"
    )
    println(
        "${"95% CI of Diff:".padEnd(labelColumnWidth)} | [${"%.2f".format(stats.medianDiffCI.lower)}, ${"%.2f".format(stats.medianDiffCI.upper)}]$diffUnitLabel ([${"%.2f".format(stats.medianDiffCIPercent.lower)}%, ${"%.2f".format(stats.medianDiffCIPercent.upper)}%])"
    )
    println(
        "${"P-value (Mann-Whitney U):".padEnd(labelColumnWidth)} | ${"%.4f".format(stats.pValue)}"
    )

    // Check if the interval contains zero.
    if (stats.medianDiffCI.lower <= 0 && stats.medianDiffCI.upper >= 0) {
        println(
            "\nThe confidence interval contains zero, suggesting no statistically significant difference between the medians."
        )
    } else {
        println(
            "\nThe confidence interval does not contain zero, suggesting a statistically significant difference exists between the medians."
        )
    }
    println("\n-------------------------------------------------------\n")
}

internal fun printCsvOutput(benchmarkName: String, allStats: Map<String, StatisticsResult>) {
    println("\n--- Machine-Readable CSV for Benchmark: $benchmarkName ---")

    if (allStats.isEmpty()) {
        println("No metrics with comparable data found for this benchmark.")
        return
    }

    // Print header once for the first metric, then only data rows for subsequent metrics.
    var isFirstMetric = true
    allStats.forEach { (metricName, stats) ->
        printSingleMetricCsvOutput(benchmarkName, metricName, stats, isFirstMetric)
        isFirstMetric = false
    }
    println("-------------------------------------------------------\n")
}

internal fun printSingleMetricCsvOutput(
    benchmarkName: String,
    metricName: String,
    stats: StatisticsResult,
    printHeader: Boolean,
) {
    val columns =
        with(stats) {
            listOf(
                "count" to "$count1",
                "min1" to "%.2f".format(min1),
                "min2" to "%.2f".format(min2),
                "min_diff" to "%.2f".format(minDiff),
                "min_diff_%" to "%.2f%%".format(minDiffPercent),
                "max1" to "%.2f".format(max1),
                "max2" to "%.2f".format(max2),
                "max_diff" to "%.2f".format(maxDiff),
                "max_diff_%" to "%.2f%%".format(maxDiffPercent),
                "mean1" to "%.2f".format(mean1),
                "mean2" to "%.2f".format(mean2),
                "mean_diff_%" to "%.2f%%".format(meanDiffPercent),
                "median1" to "%.2f".format(median1),
                "median2" to "%.2f".format(median2),
                "p-value" to "%.4f".format(pValue),
                "median_diff_%" to "%.2f%%".format(medianDiffPercent),
                "median_diff" to "%.2f".format(medianDiff),
                "median_diff_ci_lower" to "%.2f".format(medianDiffCI.lower),
                "median_diff_ci_upper" to "%.2f".format(medianDiffCI.upper),
                "median_diff_ci_lower_%" to "%.2f%%".format(medianDiffCIPercent.lower),
                "median_diff_ci_upper_%" to "%.2f%%".format(medianDiffCIPercent.upper),
                "p90_1" to "%.2f".format(p90_1),
                "p95_1" to "%.2f".format(p95_1),
                "p99_1" to "%.2f".format(p99_1),
                "p90_2" to "%.2f".format(p90_2),
                "p95_2" to "%.2f".format(p95_2),
                "p99_2" to "%.2f".format(p99_2),
            )
        }

    if (printHeader) {
        val header = "benchmark_name,metric_name," + columns.joinToString(",") { it.first }
        println(header)
    }
    val dataRow = "$benchmarkName,$metricName," + columns.joinToString(",") { it.second }
    println(dataRow)
}

/**
 * Creates and saves graphical histograms for all metrics within a benchmark.
 *
 * This function iterates through all metrics for a given benchmark and generates a separate
 * histogram plot for each, comparing the data from two revisions.
 *
 * @param benchmarkName The name of the benchmark test (e.g., "startup").
 * @param metricsA A map from metric name to its data array for revision A.
 * @param metricsB A map from metric name to its data array for revision B.
 * @param outputPath The path where the plot files will be saved.
 */
internal fun createAllMetricsHistogramPlots(
    benchmarkName: String,
    metricsA: Map<String, DoubleArray>,
    metricsB: Map<String, DoubleArray>,
    outputPath: java.nio.file.Path,
) {
    println("\n--- Generating Histogram Plots for Benchmark: $benchmarkName ---")

    if (metricsA.isEmpty() || metricsB.isEmpty()) {
        println("No data available for plotting histograms for this benchmark.")
        return
    }

    metricsA.forEach { (metricName, dataA) ->
        val dataB = metricsB[metricName]
        if (dataA.isNotEmpty() && dataB?.isNotEmpty() == true) {
            createHistogramPlot(benchmarkName, dataA, dataB, outputPath, metricName)
        } else {
            println(
                "Skipping histogram plot for '$benchmarkName' - '$metricName': data is missing or empty in one of the revisions."
            )
        }
    }
    println("-------------------------------------------------------\n")
}
