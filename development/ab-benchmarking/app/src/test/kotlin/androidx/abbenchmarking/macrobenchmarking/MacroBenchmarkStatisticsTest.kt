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
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.text.trimIndent
import org.junit.After
import org.junit.Before
import org.junit.Test

class MacroBenchmarkStatisticsTest {

    private val originalOut = System.out
    private lateinit var outContent: ByteArrayOutputStream

    @Before
    fun setUp() {
        outContent = ByteArrayOutputStream()
        System.setOut(PrintStream(outContent))
    }

    @After
    fun tearDown() {
        System.setOut(originalOut)
    }

    private val tolerance = 0.01

    @org.junit.Test
    fun calculateAllMetricsStatistics_withSignificantDifference() {
        // GIVEN two maps of metrics, where set B is consistently ~10% slower than set A
        // for "metric1", and a metric that only exists in set A.
        val dataAForMetric1 = doubleArrayOf(100.0, 101.0, 99.0, 102.0, 98.0) // mean ~100
        val dataBForMetric1 = doubleArrayOf(110.0, 111.0, 109.0, 112.0, 108.0) // mean ~110

        val dataAForMetric2 = doubleArrayOf(50.0, 50.0, 50.0) // mean 50
        val dataBForMetric2 = doubleArrayOf(55.0, 55.0, 55.0) // mean 55

        val metricsA = mapOf("metric1" to dataAForMetric1, "metric2" to dataAForMetric2)
        val metricsB = mapOf("metric1" to dataBForMetric1, "metric2" to dataBForMetric2)

        // WHEN statistics are calculated for all metrics
        val allStats = calculateAllMetricsStatistics(metricsA, metricsB)

        // THEN the results map should contain stats for matching metrics only
        assertThat(allStats).hasSize(2)
        assertThat(allStats).containsKey("metric1")
        assertThat(allStats).containsKey("metric2")

        // AND the statistics for "metric1" should reflect the difference
        val statsForMetric1 = allStats.getValue("metric1")
        assertThat(statsForMetric1.count1).isEqualTo(5)
        assertThat(statsForMetric1.mean1).isWithin(tolerance).of(100.0)
        assertThat(statsForMetric1.mean2).isWithin(tolerance).of(110.0)
        assertThat(statsForMetric1.meanDiffPercent).isWithin(tolerance).of(10.0)

        assertThat(statsForMetric1.median1).isWithin(tolerance).of(100.0)
        assertThat(statsForMetric1.median2).isWithin(tolerance).of(110.0)
        assertThat(statsForMetric1.medianDiff).isWithin(tolerance).of(10.0)
        assertThat(statsForMetric1.medianDiffPercent).isWithin(tolerance).of(10.0)

        // The p-value should be very low, indicating a significant difference.
        assertThat(statsForMetric1.pValue).isLessThan(0.05)
        // AND confidence intervals should not contain zero
        assertThat(statsForMetric1.medianDiffCI.lower).isGreaterThan(0.0)

        // AND the statistics for "metric2" should also reflect the difference
        val statsForMetric2 = allStats.getValue("metric2")
        assertThat(statsForMetric2.count1).isEqualTo(3)
        assertThat(statsForMetric2.mean1).isWithin(tolerance).of(50.0)
        assertThat(statsForMetric2.mean2).isWithin(tolerance).of(55.0)
        assertThat(statsForMetric2.meanDiffPercent).isWithin(tolerance).of(10.0)

        assertThat(statsForMetric2.median1).isWithin(tolerance).of(50.0)
        assertThat(statsForMetric2.median2).isWithin(tolerance).of(55.0)
        assertThat(statsForMetric2.medianDiff).isWithin(tolerance).of(5.0)
        assertThat(statsForMetric2.medianDiffPercent).isWithin(tolerance).of(10.0)

        // The p-value should be very low, indicating a significant difference.
        assertThat(statsForMetric2.pValue).isLessThan(0.05)
        // AND confidence intervals should not contain zero
        assertThat(statsForMetric2.medianDiffCI.lower).isGreaterThan(0.0)
    }

    @org.junit.Test
    fun calculateAllMetricsStatistics_withNoSignificantDifference() {
        // GIVEN two maps of metrics with nearly identical datasets.
        val dataAForMetric1 = doubleArrayOf(100.0, 101.0, 99.0, 102.0, 98.0)
        val dataBForMetric1 = doubleArrayOf(100.1, 101.1, 99.1, 102.1, 98.1)

        val dataAForMetric2 = doubleArrayOf(200.0, 202.0, 198.0)
        val dataBForMetric2 = doubleArrayOf(200.2, 202.2, 198.2)

        val metricsA =
            kotlin.collections.mapOf("metric1" to dataAForMetric1, "metric2" to dataAForMetric2)
        val metricsB =
            kotlin.collections.mapOf("metric1" to dataBForMetric1, "metric2" to dataBForMetric2)

        // WHEN statistics are calculated for all metrics
        val allStats =
            androidx.abbenchmarking.macrobenchmarking.calculateAllMetricsStatistics(
                metricsA,
                metricsB,
            )

        // THEN the results map should contain stats for both metrics
        assertThat(allStats).hasSize(2)
        assertThat(allStats).containsKey("metric1")
        assertThat(allStats).containsKey("metric2")

        // AND the statistics for "metric1" should show minimal differences
        val statsForMetric1 = allStats.getValue("metric1")
        assertThat(statsForMetric1.meanDiffPercent).isWithin(tolerance).of(0.1)
        assertThat(statsForMetric1.medianDiff).isWithin(tolerance).of(0.1)

        // The p-value should be high, indicating no significant difference.
        assertThat(statsForMetric1.pValue).isGreaterThan(0.05)
        // AND the confidence interval for the median difference should contain zero.
        assertThat(statsForMetric1.medianDiffCI.lower).isLessThan(0.0)
        assertThat(statsForMetric1.medianDiffCI.upper).isGreaterThan(0.0)

        // AND the statistics for "metric2" should also show minimal differences
        val statsForMetric2 = allStats.getValue("metric2")
        assertThat(statsForMetric2.meanDiffPercent).isWithin(tolerance).of(0.1)
        assertThat(statsForMetric2.medianDiff).isWithin(tolerance).of(0.2)

        // The p-value should be high, indicating no significant difference.
        assertThat(statsForMetric2.pValue).isGreaterThan(0.05)
        // AND the confidence interval for the median difference should contain zero.
        assertThat(statsForMetric2.medianDiffCI.lower).isLessThan(0.0)
        assertThat(statsForMetric2.medianDiffCI.upper).isGreaterThan(0.0)
    }

    @Test
    fun printSummary_formatsOutputCorrectly_withSignificantDifference() {
        // GIVEN a StatisticsResult object with known values indicating a significant difference
        val timeToInitialDisplayStats = createSampleStats()
        val frameDurationCpuStats = createSampleStats1()
        val benchmarkName = "MyBenchmarkTest"
        val allMetricStats: Map<String, StatisticsResult> =
            mapOf(
                "timeToInitialDisplayMs" to timeToInitialDisplayStats,
                "frameDurationCpuMs" to frameDurationCpuStats,
            )

        // and GIVEN the standard output is redirected to capture the result
        val originalOut = System.out
        val outContent = ByteArrayOutputStream()
        System.setOut(PrintStream(outContent))

        // WHEN the printSummary function is called
        printSummary(benchmarkName, allMetricStats)

        // THEN the output should be a correctly formatted multi-line string
        val expectedOutput =
            """
================================================================================
          Statistical Summary for Benchmark: MyBenchmarkTest
================================================================================

--- Comparison for: timeToInitialDisplayMs ---
                          | Dataset 1 (Branch A) | Dataset 2 (Branch B)
----------------------------------------------------------------
Count                     | 50                   | 50
Min (ms)                  | 153.74               | 156.08
Mean (ms)                 | 167.41               | 167.09
Max (ms)                  | 190.97               | 187.51
Median (ms)               | 166.29               | 165.79
Std. Dev. (ms)            | 7.38                 | 5.83
Min Difference:           | 2.34 ms (1.52%)
Mean Difference:          | -0.32 ms (-0.19%)
Max Difference:           | -3.46 ms (-1.81%)
Median Difference:        | -0.50 ms (-0.30%)
95% CI of Diff:           | [-3.05, 2.09] ms ([-1.83%, 1.26%])
P-value (Mann-Whitney U): | 0.9012

The confidence interval contains zero, suggesting no statistically significant difference between the medians.

-------------------------------------------------------


--- Comparison for: frameDurationCpuMs ---
                          | Dataset 1 (Branch A) | Dataset 2 (Branch B)
----------------------------------------------------------------
Count                     | 122                  | 120
Min (ms)                  | 1.47                 | 1.44
Mean (ms)                 | 16.99                | 17.06
Max (ms)                  | 48.97                | 47.79
Median (ms)               | 4.10                 | 3.98
P90 (ms)                  | 38.81                | 39.43
P95 (ms)                  | 40.51                | 40.44
P99 (ms)                  | 48.40                | 46.50
Std. Dev. (ms)            | 17.62                | 17.55
Min Difference:           | -0.03 ms (-2.29%)
Mean Difference:          | 0.08 ms (0.44%)
Max Difference:           | -1.19 ms (-2.42%)
Median Difference:        | -0.12 ms (-2.95%)
95% CI of Diff:           | [-15.33, 29.66] ms ([-374.04%, 723.90%])
P-value (Mann-Whitney U): | 0.8198

The confidence interval contains zero, suggesting no statistically significant difference between the medians.

-------------------------------------------------------

            """
                .trimIndent()

        val actualOutput = outContent.toString().replace("\r\n", "\n")
        assertThat(actualOutput.trim()).isEqualTo(expectedOutput.trim())

        // FINALLY, restore the original standard output
        System.setOut(originalOut)
    }

    @Test
    fun printCsvOutput_formatsOutputCorrectly() {
        // GIVEN a StatisticsResult object and a benchmark name
        val timeToInitialDisplayStats = createSampleStats()
        val frameDurationCpuStats = createSampleStats1()
        val benchmarkName = "MyBenchmarkTest"
        val allMetricStats: Map<String, StatisticsResult> =
            mapOf(
                "timeToInitialDisplayMs" to timeToInitialDisplayStats,
                "frameDurationCpuMs" to frameDurationCpuStats,
            )
        // WHEN the printCsvOutput function is called
        printCsvOutput(benchmarkName, allMetricStats)

        // THEN the output should be a correctly formatted multi-line CSV string
        val expectedOutput =
            """
            --- Machine-Readable CSV for Benchmark: MyBenchmarkTest ---
            benchmark_name,metric_name,count,min1,min2,min_diff,min_diff_%,max1,max2,max_diff,max_diff_%,mean1,mean2,mean_diff_%,median1,median2,p-value,median_diff_%,median_diff,median_diff_ci_lower,median_diff_ci_upper,median_diff_ci_lower_%,median_diff_ci_upper_%,p90_1,p95_1,p99_1,p90_2,p95_2,p99_2
            MyBenchmarkTest,timeToInitialDisplayMs,50,153.74,156.08,2.34,1.52%,190.97,187.51,-3.46,-1.81%,167.41,167.09,-0.19%,166.29,165.79,0.9012,-0.30%,-0.50,-3.05,2.09,-1.83%,1.26%,176.81,182.14,190.97,175.45,177.62,187.51
            MyBenchmarkTest,frameDurationCpuMs,122,1.47,1.44,-0.03,-2.29%,48.97,47.79,-1.19,-2.42%,16.99,17.06,0.44%,4.10,3.98,0.8198,-2.95%,-0.12,-15.33,29.66,-374.04%,723.90%,38.81,40.51,48.40,39.43,40.44,46.50
            -------------------------------------------------------

            """
                .trimIndent()

        // Restore stream so we can see debug output
        System.setOut(originalOut)
        val actualOutput = outContent.toString().replace("\r\n", "\n")

        // ASSERT: Compare trimmed content to avoid whitespace issues
        assertThat(actualOutput.trim()).isEqualTo(expectedOutput.trim())
    }

    // Helper function to create sample stats and avoid code duplication
    private fun createSampleStats(): StatisticsResult {
        return StatisticsResult(
            count1 = 50,
            min1 = 153.74,
            max1 = 190.97,
            mean1 = 167.41,
            median1 = 166.29,
            p90_1 = 176.81,
            p95_1 = 182.14,
            p99_1 = 190.97,
            std1 = 7.38,
            count2 = 50,
            min2 = 156.08,
            max2 = 187.51,
            mean2 = 167.09,
            median2 = 165.79,
            p90_2 = 175.45,
            p95_2 = 177.62,
            p99_2 = 187.51,
            std2 = 5.83,
            minDiff = 2.34,
            minDiffPercent = 1.52,
            maxDiff = -3.46,
            maxDiffPercent = -1.81,
            meanDiff = -0.32,
            meanDiffPercent = -0.19,
            medianDiff = -0.50,
            medianDiffPercent = -0.30,
            pValue = 0.9012,
            medianDiffCI = ConfidenceInterval(-3.05, 2.09),
            medianDiffCIPercent = ConfidenceInterval(-1.83, 1.26),
        )
    }

    private fun createSampleStats1(): StatisticsResult {
        return StatisticsResult(
            count1 = 122,
            min1 = 1.47,
            max1 = 48.97,
            mean1 = 16.99,
            median1 = 4.10,
            p90_1 = 38.81,
            p95_1 = 40.51,
            p99_1 = 48.4,
            std1 = 17.62,
            count2 = 120,
            min2 = 1.44,
            max2 = 47.79,
            mean2 = 17.06,
            median2 = 3.98,
            p90_2 = 39.43,
            p95_2 = 40.44,
            p99_2 = 46.5,
            std2 = 17.55,
            minDiff = -0.03,
            minDiffPercent = -2.29,
            maxDiff = -1.19,
            maxDiffPercent = -2.42,
            meanDiff = 0.08,
            meanDiffPercent = 0.44,
            medianDiff = -0.12,
            medianDiffPercent = -2.95,
            pValue = 0.8198,
            medianDiffCI = ConfidenceInterval(-15.33, 29.66),
            medianDiffCIPercent = ConfidenceInterval(-374.04, 723.90),
        )
    }
}
