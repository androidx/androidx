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

import androidx.abbenchmarking.common.ConfidenceInterval
import androidx.abbenchmarking.common.calculateBCaCIMedianDifference
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.inference.MannWhitneyUTest

/**
 * Contains the data class for storing statistical results and all functions related to performing
 * and printing the statistical analysis.
 */
data class StatisticsResult(
    val count1: Int,
    val min1: Double,
    val mean1: Double,
    val median1: Double,
    val std1: Double,
    val count2: Int,
    val min2: Double,
    val mean2: Double,
    val median2: Double,
    val std2: Double,
    val minDiff: Double,
    val minDiffPercent: Double,
    val meanDiff: Double,
    val meanDiffPercent: Double,
    val medianDiff: Double,
    val medianDiffPercent: Double,
    val pValue: Double,
    val medianDiffCI: ConfidenceInterval,
    val medianDiffCIPercent: ConfidenceInterval,
)

fun calculateStatistics(data1: DoubleArray, data2: DoubleArray): StatisticsResult {
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
    // Mins
    val min1 = stats1.min
    val min2 = stats2.min
    val minDiff = min2 - min1
    val minDiffPercent = if (min1 != 0.0) (minDiff / min1) * 100 else 0.0
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
        mean1 = mean1,
        median1 = median1,
        std1 = std1,
        count2 = n2.toInt(),
        min2 = min2,
        mean2 = mean2,
        median2 = median2,
        std2 = std2,
        minDiff = minDiff,
        minDiffPercent = minDiffPercent,
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
 * Prints a human-readable statistical summary to the console.
 *
 * @param benchmarkName The name of the test being summarized.
 * @param stats The calculated statistical results.
 */
internal fun printSummary(benchmarkName: String, stats: StatisticsResult) {
    println("\n--- Comparison for: $benchmarkName ---")
    println("                             Dataset 1 (Branch A)   | Dataset 2 (Branch B)")
    println("--------------------------------------------------------------------------")
    println(
        "Count                        | ${stats.count1.toString().padEnd(20)} | ${stats.count2}"
    )
    println(
        "Min (ns)                     | ${"%.2f".format(stats.min1).padEnd(20)} | ${"%.2f".format(stats.min2)}"
    )
    println(
        "Mean (ns)                    | ${"%.2f".format(stats.mean1).padEnd(20)} | ${"%.2f".format(stats.mean2)}"
    )
    println(
        "Median (ns)                  | ${"%.2f".format(stats.median1).padEnd(20)} | ${"%.2f".format(stats.median2)}"
    )
    println(
        "Std. Dev. (ns)               | ${"%.2f".format(stats.std1).padEnd(20)} | ${"%.2f".format(stats.std2)}"
    )
    println(
        "Min Difference:              | ${"%.2f".format(stats.minDiff)} ns (${"%.2f".format(stats.minDiffPercent)}%)"
    )
    println(
        "Mean Difference:             | ${"%.2f".format(stats.meanDiff)} ns (${"%.2f".format(stats.meanDiffPercent)}%)"
    )
    println(
        "Median Difference:           | ${"%.2f".format(stats.medianDiff)} ns (${"%.2f".format(stats.medianDiffPercent)}%)"
    )
    println(
        "95% CI of Diff:              | [${"%.2f".format(stats.medianDiffCI.lower)}, ${"%.2f".format(stats.medianDiffCI.upper)}] ns ([${"%.2f".format(stats.medianDiffCIPercent.lower)}%, ${"%.2f".format(stats.medianDiffCIPercent.upper)}%])"
    )
    println("P-value (Mann-Whitney U):    | ${"%.4f".format(stats.pValue)}")

    // Check if the interval contains zero.
    if (stats.medianDiffCI.lower < 0 && stats.medianDiffCI.upper > 0) {
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

internal fun printCsvOutput(benchmarkName: String, stats: StatisticsResult) {
    // Define columns and their formatted values in one place
    val columns =
        with(stats) {
            listOf(
                "count" to "$count1",
                "min1" to "%.2f".format(min1),
                "min2" to "%.2f".format(min2),
                "min_diff" to "%.2f".format(minDiff),
                "min_diff_%" to "%.2f%%".format(minDiffPercent),
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
            )
        }
    // Generate the header and data row from the single source of truth
    val header = "benchmarkName," + columns.joinToString(",") { it.first }
    val dataRow = "$benchmarkName," + columns.joinToString(",") { it.second }
    println("--- Machine-Readable CSV ---")
    println(header)
    println(dataRow)
}
