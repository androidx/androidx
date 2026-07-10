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
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import org.junit.Test

class MicroBenchmarkStatisticsTest {

    // A small tolerance for comparing floating-point numbers.
    private val tolerance = 0.01

    @Test
    fun calculateStatistics_withSignificantDifference() {
        // GIVEN two datasets where set B is consistently ~10% slower than set A.
        val dataA = doubleArrayOf(100.0, 101.0, 99.0, 102.0, 98.0) // mean ~100
        val dataB = doubleArrayOf(110.0, 111.0, 109.0, 112.0, 108.0) // mean ~110

        // WHEN statistics are calculated
        val stats = calculateStatistics(dataA, dataB)

        // THEN the results should reflect the difference
        assertThat(stats.count1).isEqualTo(5)
        assertThat(stats.mean1).isWithin(tolerance).of(100.0)
        assertThat(stats.mean2).isWithin(tolerance).of(110.0)
        assertThat(stats.meanDiffPercent).isWithin(tolerance).of(10.0) // B is 10% slower

        assertThat(stats.median1).isWithin(tolerance).of(100.0)
        assertThat(stats.median2).isWithin(tolerance).of(110.0)
        assertThat(stats.medianDiff).isWithin(tolerance).of(10.0)
        assertThat(stats.medianDiffPercent).isWithin(tolerance).of(10.0)

        // The p-value should be very low, indicating a significant difference.
        assertThat(stats.pValue).isLessThan(0.05)
        // AND confidence intervals should not contain zero
        assertThat(stats.medianDiffCI.lower).isGreaterThan(0.0)
    }

    @Test
    fun calculateStatistics_withNoSignificantDifference() {
        // GIVEN two nearly identical datasets.
        val dataA = doubleArrayOf(100.0, 101.0, 99.0, 102.0, 98.0)
        val dataB = doubleArrayOf(100.1, 101.1, 99.1, 102.1, 98.1)

        // WHEN statistics are calculated
        val stats = calculateStatistics(dataA, dataB)

        // THEN the differences should be minimal
        assertThat(stats.meanDiffPercent).isWithin(tolerance).of(0.1)
        assertThat(stats.medianDiff).isWithin(tolerance).of(0.1)

        // The p-value should be high, indicating no significant difference.
        assertThat(stats.pValue).isGreaterThan(0.05)
        // AND confidence intervals should contain zero
        assertThat(stats.medianDiffCI.lower).isLessThan(0.0)
        assertThat(stats.medianDiffCI.upper).isGreaterThan(0.0)
    }

    @Test
    fun calculateStatistics_handlesImprovement() {
        // GIVEN two datasets where set B is consistently faster than set A.
        val dataA = doubleArrayOf(110.0, 111.0, 109.0, 112.0, 108.0) // mean ~110
        val dataB = doubleArrayOf(100.0, 101.0, 99.0, 102.0, 98.0) // mean ~100

        // WHEN statistics are calculated
        val stats = calculateStatistics(dataA, dataB)

        // THEN the differences should be negative
        assertThat(stats.meanDiffPercent).isWithin(tolerance).of(-9.09) // (100-110)/110
        assertThat(stats.medianDiff).isWithin(tolerance).of(-10.0)
        assertThat(stats.medianDiffPercent).isWithin(tolerance).of(-9.09)
    }

    @Test
    fun confidenceInterval_shouldContainTheMedianDifference() {
        // GIVEN two datasets
        val dataA = doubleArrayOf(100.0, 101.0, 99.0, 102.0, 98.0, 105.0, 95.0)
        val dataB = doubleArrayOf(110.0, 111.0, 109.0, 112.0, 108.0, 115.0, 105.0)

        // WHEN statistics are calculated
        val stats = calculateStatistics(dataA, dataB)

        // THEN the calculated median difference should fall within the confidence interval
        assertThat(stats.medianDiff).isAtLeast(stats.medianDiffCI.lower)
        assertThat(stats.medianDiff).isAtMost(stats.medianDiffCI.upper)
    }

    @Test
    fun printSummary_formatsOutputCorrectly_withSignificantDifference() {
        // GIVEN a StatisticsResult object with known values indicating a significant difference
        val stats =
            StatisticsResult(
                count1 = 10,
                min1 = 95.5,
                mean1 = 100.0,
                median1 = 99.0,
                std1 = 5.0,
                count2 = 10,
                min2 = 105.5,
                mean2 = 110.0,
                median2 = 109.0,
                std2 = 6.0,
                minDiff = 10.0,
                minDiffPercent = 10.47,
                meanDiff = 10.0,
                meanDiffPercent = 10.0,
                medianDiff = 10.0,
                medianDiffPercent = 10.101,
                pValue = 0.04,
                medianDiffCI = ConfidenceInterval(8.1, 12.2),
                medianDiffCIPercent = ConfidenceInterval(8.18, 12.32),
            )
        val benchmarkName = "MyBenchmarkTest"

        // and GIVEN the standard output is redirected to capture the result
        val originalOut = System.out
        val outContent = ByteArrayOutputStream()
        System.setOut(PrintStream(outContent))

        // WHEN the printSummary function is called
        printSummary(benchmarkName, stats)

        // THEN the output should be a correctly formatted multi-line string
        val expectedOutput =
            """
--- Comparison for: MyBenchmarkTest ---
                             Dataset 1 (Branch A)   | Dataset 2 (Branch B)
--------------------------------------------------------------------------
Count                        | 10                   | 10
Min (ns)                     | 95.50                | 105.50
Mean (ns)                    | 100.00               | 110.00
Median (ns)                  | 99.00                | 109.00
Std. Dev. (ns)               | 5.00                 | 6.00
Min Difference:              | 10.00 ns (10.47%)
Mean Difference:             | 10.00 ns (10.00%)
Median Difference:           | 10.00 ns (10.10%)
95% CI of Diff:              | [8.10, 12.20] ns ([8.18%, 12.32%])
P-value (Mann-Whitney U):    | 0.0400

The confidence interval does not contain zero, suggesting a statistically significant difference exists between the medians.

-------------------------------------------------------

"""

        val actualOutput = outContent.toString()
        assertThat(actualOutput).isEqualTo(expectedOutput)

        // FINALLY, restore the original standard output
        System.setOut(originalOut)
    }

    @Test
    fun printCsvOutput_formatsOutputCorrectly() {
        // GIVEN a StatisticsResult object with known values
        val stats =
            StatisticsResult(
                count1 = 10,
                min1 = 95.5,
                mean1 = 100.0,
                median1 = 99.0,
                std1 = 5.0,
                count2 = 10,
                min2 = 105.5,
                mean2 = 110.0,
                median2 = 109.0,
                std2 = 6.0,
                minDiff = 10.0,
                minDiffPercent = 10.47,
                meanDiff = 10.0,
                meanDiffPercent = 10.0,
                medianDiff = 10.0,
                medianDiffPercent = 10.101,
                pValue = 0.04567,
                medianDiffCI = ConfidenceInterval(8.1, 12.2),
                medianDiffCIPercent = ConfidenceInterval(8.18, 12.32),
            )
        val benchmarkName = "MyBenchmarkTest"

        // and GIVEN the standard output is redirected to capture the result
        val originalOut = System.out
        val outContent = ByteArrayOutputStream()
        System.setOut(PrintStream(outContent))

        // WHEN the printCsvOutput function is called
        printCsvOutput(benchmarkName, stats)

        // THEN the output should be a correctly formatted multi-line CSV string
        val expectedOutput =
            """
        --- Machine-Readable CSV ---
        benchmarkName,count,min1,min2,min_diff,min_diff_%,mean1,mean2,mean_diff_%,median1,median2,p-value,median_diff_%,median_diff,median_diff_ci_lower,median_diff_ci_upper,median_diff_ci_lower_%,median_diff_ci_upper_%
        MyBenchmarkTest,10,95.50,105.50,10.00,10.47%,100.00,110.00,10.00%,99.00,109.00,0.0457,10.10%,10.00,8.10,12.20,8.18%,12.32%
    """
                .trimIndent() + System.lineSeparator() // printCsvOutput adds a final newline

        val actualOutput = outContent.toString()
        assertThat(actualOutput).isEqualTo(expectedOutput)

        // FINALLY, restore the original standard output
        System.setOut(originalOut)
    }
}
