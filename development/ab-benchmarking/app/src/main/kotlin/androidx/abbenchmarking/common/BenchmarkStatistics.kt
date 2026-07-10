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

import java.nio.file.Path
import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.random.Random
import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.stat.StatUtils
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.descriptive.rank.Percentile
import org.jetbrains.letsPlot.export.ggsave
import org.jetbrains.letsPlot.geom.geomHistogram
import org.jetbrains.letsPlot.ggsize
import org.jetbrains.letsPlot.label.ggtitle
import org.jetbrains.letsPlot.letsPlot
import org.jetbrains.letsPlot.pos.positionIdentity

/**
 * Represents the lower and upper bounds of a confidence interval.
 *
 * @property lower The lower bound of the confidence interval.
 * @property upper The upper bound of the confidence interval.
 */
data class ConfidenceInterval(val lower: Double, val upper: Double)

/**
 * Calculates the bootstrap confidence interval for the difference in medians between two datasets.
 *
 * This method uses bootstrap resampling to estimate the 95% confidence interval for the difference
 * between the medians of two samples (`data1` and `data2`). It repeatedly draws random samples with
 * replacement from each dataset, calculates the difference in medians for each pair of resampled
 * datasets, and then determines the range that contains the central 95% of these differences.
 *
 * How to interpret the result: The returned `ConfidenceInterval` represents the lower and upper
 * bounds of the 95% confidence interval.
 * - If the interval (e.g., `[2.5, 5.0]`) does not contain zero, it suggests that there is a
 *   statistically significant difference between the medians of the underlying populations from
 *   which the samples were drawn.
 * - If the interval (e.g., `[-1.2, 3.4]`) contains zero, it means that there is no statistically
 *   significant evidence of a difference between the medians.
 * - The width of the interval provides a sense of the precision of the estimate. A narrower
 *   interval indicates a more precise estimate of the median difference.
 *
 * @param data1 The first dataset.
 * @param data2 The second dataset.
 * @param numSamples The number of bootstrap samples to generate.
 * @param confidenceLevel The desired confidence level.
 * @return A ConfidenceInterval containing the lower and upper bounds of the confidence interval.
 */
internal fun calculateBootstrapCIMedianDifference(
    data1: DoubleArray,
    data2: DoubleArray,
    numSamples: Int = 10000,
    confidenceLevel: Double = 0.95,
): ConfidenceInterval {
    if (data1.isEmpty() || data2.isEmpty()) {
        return ConfidenceInterval(Double.NaN, Double.NaN)
    }
    val random = Random(42) // Use a fixed seed for deterministic results
    val medianDiffs = DoubleArray(numSamples)
    val n1 = data1.size
    val n2 = data2.size
    val resample1 = DoubleArray(n1)
    val resample2 = DoubleArray(n2)
    for (i in 0 until numSamples) {
        for (j in 0 until n1) {
            resample1[j] = data1[random.nextInt(n1)]
        }
        val median1 = DescriptiveStatistics(resample1).getPercentile(50.0)
        for (j in 0 until n2) {
            resample2[j] = data2[random.nextInt(n2)]
        }
        val median2 = DescriptiveStatistics(resample2).getPercentile(50.0)
        medianDiffs[i] = median2 - median1
    }
    medianDiffs.sort()
    val lowerIndex = ((1.0 - confidenceLevel) / 2.0 * numSamples).toInt()
    val upperIndex = (numSamples - lowerIndex - 1)
    return ConfidenceInterval(medianDiffs[lowerIndex], medianDiffs[upperIndex])
}

/**
 * Calculates the Bias-Corrected and Accelerated (BCa) bootstrap confidence interval for the
 * difference in medians between two datasets.
 *
 * This method provides a more robust and accurate confidence interval than the standard percentile
 * bootstrap. It improves upon the percentile method by correcting for two potential sources of
 * error:
 * 1. **Bias**: A systematic difference between the median of the bootstrap distribution and the
 *    original sample's median difference. Corrected by the bias-correction factor `z0`.
 * 2. **Skewness**: A non-symmetrical bootstrap distribution, which can lead to inaccurate
 *    intervals. Corrected by the acceleration factor `a`, which estimates the rate of change of the
 *    standard error of the statistic.
 *
 * The acceleration factor `a` is estimated using the computationally intensive but accurate
 * jackknife resampling method.
 *
 * @param data1 The first dataset (e.g., from Branch A).
 * @param data2 The second dataset (e.g., from Branch B).
 * @param observedMedianDiff The pre-calculated median difference (`median(data2) - median(data1)`).
 * @param numBootstrapSamples The number of bootstrap resamples to generate. SciPy defaults to 9999.
 * @param confidenceLevel The desired confidence level for the interval (e.g., 0.95 for 95% CI).
 * @return A ConfidenceInterval containing the lower and upper bounds of the BCa confidence
 *   interval. If bias-correction is not possible, it falls back to the standard percentile method.
 */
internal fun calculateBCaCIMedianDifference(
    data1: DoubleArray,
    data2: DoubleArray,
    observedMedianDiff: Double,
    numBootstrapSamples: Int = 10000,
    confidenceLevel: Double = 0.95,
): ConfidenceInterval {
    if (data1.isEmpty() || data2.isEmpty()) {
        return ConfidenceInterval(Double.NaN, Double.NaN)
    }

    val random = Random(42)
    val n1 = data1.size
    val n2 = data2.size
    val standardNormal = NormalDistribution(0.0, 1.0)
    val percentile = Percentile()

    // --- Step 1: Generate bootstrap replicates of the statistic ---
    // Create a distribution of median differences by repeatedly resampling the original data.
    val bootstrapReplicates =
        DoubleArray(numBootstrapSamples) {
            val resample1 = DoubleArray(n1) { data1[random.nextInt(n1)] }
            val resample2 = DoubleArray(n2) { data2[random.nextInt(n2)] }
            val median1 = percentile.evaluate(resample1)
            val median2 = percentile.evaluate(resample2)
            median2 - median1
        }
    val sortedBootstrapReplicates = bootstrapReplicates.sortedArray()

    // --- Step 2: Calculate the bias-correction factor (z₀) ---
    // This measures the median bias of the bootstrap distribution.
    val numBelow = bootstrapReplicates.count { it < observedMedianDiff }
    val proportionBelow = numBelow.toDouble() / numBootstrapSamples

    // Robustness: If the original statistic is an extremum of the bootstrap distribution,
    // z₀ would be infinite. In this case, BCa is not well-defined, so we fall back to the
    // simpler percentile method, which is more stable.
    if (proportionBelow == 0.0 || proportionBelow == 1.0) {
        println(
            "Warning: Bias-correction factor is undefined (z₀ is infinite). " +
                "Falling back to the standard percentile confidence interval."
        )
        return calculateBootstrapCIMedianDifference(
            data1,
            data2,
            numBootstrapSamples,
            confidenceLevel,
        )
    }
    val z0 = standardNormal.inverseCumulativeProbability(proportionBelow)

    // --- Step 3: Calculate the acceleration factor (a) using the Jackknife method ---
    // This estimates the skewness of the statistic's sampling distribution.
    val jackknifeEstimates = DoubleArray(n1 + n2)
    val originalMedian1 = percentile.evaluate(data1)
    val originalMedian2 = percentile.evaluate(data2)

    // Jackknife on the first dataset
    for (i in 0 until n1) {
        val jackknifeSample = data1.filterIndexed { index, _ -> index != i }.toDoubleArray()
        jackknifeEstimates[i] = originalMedian2 - percentile.evaluate(jackknifeSample)
    }
    // Jackknife on the second dataset
    for (i in 0 until n2) {
        val jackknifeSample = data2.filterIndexed { index, _ -> index != i }.toDoubleArray()
        jackknifeEstimates[n1 + i] = percentile.evaluate(jackknifeSample) - originalMedian1
    }

    val jackknifeMean = StatUtils.mean(jackknifeEstimates)
    val numerator = jackknifeEstimates.sumOf { (jackknifeMean - it).pow(3) }
    val denominator = 6 * jackknifeEstimates.sumOf { (jackknifeMean - it).pow(2) }.pow(1.5)

    // Robustness: If all jackknife estimates are identical, the denominator will be zero.
    // In this case, there is no skewness to correct for, so acceleration is zero.
    val a = if (denominator == 0.0) 0.0 else numerator / denominator

    // --- Step 4: Calculate the BCa interval endpoints (α₁, α₂) ---
    // Adjust the confidence level percentiles using the bias (z₀) and acceleration (a) factors.
    val alpha = (1.0 - confidenceLevel) / 2.0
    val zAlpha1 = standardNormal.inverseCumulativeProbability(alpha)
    val zAlpha2 = standardNormal.inverseCumulativeProbability(1.0 - alpha)

    val term1 = z0 + zAlpha1
    val adjustedAlpha1 = standardNormal.cumulativeProbability(z0 + term1 / (1.0 - a * term1))

    val term2 = z0 + zAlpha2
    val adjustedAlpha2 = standardNormal.cumulativeProbability(z0 + term2 / (1.0 - a * term2))

    // --- Step 5: Find the corresponding percentiles from the bootstrap distribution ---
    val lowerBound = percentile.evaluate(sortedBootstrapReplicates, adjustedAlpha1 * 100)
    val upperBound = percentile.evaluate(sortedBootstrapReplicates, adjustedAlpha2 * 100)

    return ConfidenceInterval(lowerBound, upperBound)
}

/**
 * Creates and saves a graphical histogram comparing two datasets using Lets-Plot.
 *
 * The resulting plot is saved as a PNG file in the output directory.
 *
 * @param benchmarkName The name of the benchmark test, used for the plot title and filename.
 * @param data1 Benchmark data from rev A.
 * @param data2 Benchmark data from rev B.
 * @param outputPath The path where the plot file will be saved.
 * @param metricName An optional name for the specific metric being plotted. If provided, it's
 *   included in the plot title and filename.
 */
fun createHistogramPlot(
    benchmarkName: String,
    data1: DoubleArray,
    data2: DoubleArray,
    outputPath: Path,
    metricName: String,
) {
    val combinedMetricData = data1.toList() + data2.toList()
    val metricLabel = metricName ?: "timing"
    val combinedData =
        mapOf<String, Any>(
            metricName to combinedMetricData,
            "branch" to List(data1.size) { "Branch A" } + List(data2.size) { "Branch B" },
        )

    val stats = DescriptiveStatistics(combinedMetricData.toDoubleArray())
    val n = stats.n.toDouble()
    val q1 = stats.getPercentile(25.0)
    val q3 = stats.getPercentile(75.0)
    val iqr = q3 - q1

    val minBins = 5
    val calculatedBins =
        if (iqr > 0) {
            val binWidth = 2 * iqr * (1 / cbrt(n))
            if (binWidth > 0) {
                ((stats.max - stats.min) / binWidth).toInt()
            } else {
                minBins
            }
        } else {
            minBins
        }

    val optimalBins = maxOf(1, minOf(calculatedBins, n.toInt())).coerceAtLeast(minBins)

    val plotTitle = "Benchmark Comparison: $benchmarkName - $metricName"

    val plot =
        letsPlot(combinedData) {
            x = metricName
            fill = "branch"
        } +
            geomHistogram(alpha = 0.3, position = positionIdentity, bins = optimalBins) +
            ggsize(1000, 600) +
            ggtitle(plotTitle)

    val plotFileName = "${benchmarkName}_${metricName}_histogram.png"

    val plotFile = ggsave(plot, plotFileName, path = outputPath.toString())

    val savedMessage = "Saved histogram for $benchmarkName - $metricName to: $plotFile"
    println(savedMessage)
}
