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

import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.text.substringAfter
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BenchmarkStatisticsTest {

    @get:Rule val tempFolder = TemporaryFolder()

    // A small tolerance for comparing floating-point numbers.
    private val tolerance = 0.01

    @Test
    fun calculateBootstrapCIMedianDifference_withClearDifference() {
        // GIVEN two datasets with clearly different medians
        // Median of data1 is 100.0, Median of data2 is 120.0
        val data1 = doubleArrayOf(98.0, 99.0, 100.0, 101.0, 102.0)
        val data2 = doubleArrayOf(118.0, 119.0, 120.0, 121.0, 122.0)

        // WHEN the confidence interval is calculated
        val ci = calculateBootstrapCIMedianDifference(data1, data2)

        // THEN the resulting interval should not contain zero, indicating a significant difference
        // The expected values are deterministic due to the fixed random seed.
        assertThat(ci.lower).isWithin(0.01).of(17.0)
        assertThat(ci.upper).isWithin(0.01).of(23.0)
        assertThat(ci.lower).isGreaterThan(0.0)
        assertThat(ci.upper).isGreaterThan(0.0)
    }

    @Test
    fun calculateBootstrapCIMedianDifference_withOverlappingData() {
        // GIVEN two datasets with similar medians and overlapping values
        // Median of data1 is 100.0, Median of data2 is 101.0
        val data1 = doubleArrayOf(98.0, 99.0, 100.0, 101.0, 102.0)
        val data2 = doubleArrayOf(99.0, 100.0, 101.0, 102.0, 103.0)

        // WHEN the confidence interval is calculated
        val ci = calculateBootstrapCIMedianDifference(data1, data2)

        // THEN the resulting interval should contain zero, suggesting no significant difference
        // The expected values are deterministic due to the fixed random seed.
        assertThat(ci.lower).isWithin(0.01).of(-2.0)
        assertThat(ci.upper).isWithin(0.01).of(4.0)
        assertThat(ci.lower).isLessThan(0.0)
        assertThat(ci.upper).isGreaterThan(0.0)
    }

    @Test
    fun calculateBCaCIMedianDifference_withClearDifference() {
        // GIVEN two datasets with clearly different medians
        val data1 = doubleArrayOf(98.0, 99.0, 100.0, 101.0, 102.0) // Median: 100.0
        val data2 = doubleArrayOf(118.0, 119.0, 120.0, 121.0, 122.0) // Median: 120.0
        val observedMedianDiff = 20.0

        // WHEN the BCa confidence interval is calculated
        val ci = calculateBCaCIMedianDifference(data1, data2, observedMedianDiff)

        // THEN the interval should not contain zero
        // With simple symmetric data, BCa can be close to percentile, which is fine.
        assertThat(ci.lower).isWithin(0.01).of(16.76)
        assertThat(ci.upper).isWithin(0.01).of(22.0)
        assertThat(ci.lower).isGreaterThan(0.0)
    }

    @Test
    fun calculateBCaCIMedianDifference_withOverlappingData() {
        // GIVEN two datasets with similar medians
        val data1 = doubleArrayOf(98.0, 99.0, 100.0, 101.0, 102.0) // Median: 100.0
        val data2 = doubleArrayOf(99.0, 100.0, 101.0, 102.0, 103.0) // Median: 101.0
        val observedMedianDiff = 1.0

        // WHEN the BCa confidence interval is calculated
        val ci = calculateBCaCIMedianDifference(data1, data2, observedMedianDiff)

        // THEN the interval should contain zero
        assertThat(ci.lower).isWithin(0.01).of(-2.24)
        assertThat(ci.upper).isWithin(0.01).of(3.0)
        assertThat(ci.lower).isLessThan(0.0)
        assertThat(ci.upper).isGreaterThan(0.0)
    }

    @Test
    fun calculateBCaCIMedianDifference_fallsBackToBootstrapCI_whenBiasIsUndefined() {
        // GIVEN two datasets where all elements are identical within each set.
        // This creates a scenario where all bootstrap replicates of the median difference
        // are identical to the observed median difference.
        val data1 = doubleArrayOf(10.0, 10.0, 10.0, 10.0, 10.0) // Median: 10.0
        val data2 = doubleArrayOf(20.0, 20.0, 20.0, 20.0, 20.0) // Median: 20.0
        val observedMedianDiff = 10.0

        // In this case, every bootstrap median difference will be exactly 10.0.
        // This makes the proportion of bootstrap replicates less than the observed median diff
        // (10.0)
        // equal to 0. The bias-correction factor (z₀) becomes infinite, and the BCa method
        // must fall back to the standard percentile bootstrap method.

        // and GIVEN the standard output is redirected to capture the warning
        val originalOut = System.out
        val outContent = ByteArrayOutputStream()
        System.setOut(PrintStream(outContent))

        // WHEN the BCa confidence interval is calculated
        val bcaResult = calculateBCaCIMedianDifference(data1, data2, observedMedianDiff)

        // THEN the system should print a fallback warning
        val consoleOutput = outContent.toString()
        assertThat(consoleOutput)
            .contains(
                "Warning: Bias-correction factor is undefined (z₀ is infinite). " +
                    "Falling back to the standard percentile confidence interval."
            )

        // AND the result from BCa should be identical to a direct call to the percentile method
        val bootstrapResult = calculateBootstrapCIMedianDifference(data1, data2)

        assertThat(bcaResult.lower).isEqualTo(bootstrapResult.lower)
        assertThat(bcaResult.upper).isEqualTo(bootstrapResult.upper)

        // AND the result itself should be a single point, since there's no variation
        assertThat(bcaResult.lower).isWithin(tolerance).of(10.0)
        assertThat(bcaResult.upper).isWithin(tolerance).of(10.0)

        // FINALLY, restore the original standard output
        System.setOut(originalOut)
    }

    @Test
    fun calculateBCaCIMedianDifference_withEmptyInput() {
        // GIVEN one empty dataset
        val data1 = doubleArrayOf(1.0, 2.0, 3.0)
        val data2 = doubleArrayOf()

        // WHEN the confidence interval is calculated
        val ci = calculateBCaCIMedianDifference(data1, data2, Double.NaN)

        // THEN the result should be NaN for both bounds
        assertThat(ci.lower).isNaN()
        assertThat(ci.upper).isNaN()
    }

    @Test
    fun createHistogramPlot_savesFileToTempDir() {
        // GIVEN two datasets, a benchmark name, and a temporary output path
        val data1 = doubleArrayOf(100.0, 102.0, 105.0)
        val data2 = doubleArrayOf(110.0, 112.0, 115.0)
        val benchmarkName = "MyPlotTest"
        val tempPath: Path = tempFolder.root.toPath()

        // and GIVEN the standard output is redirected to capture console messages
        val originalOut = System.out
        val outContent = ByteArrayOutputStream()
        System.setOut(PrintStream(outContent))

        // WHEN the createHistogramPlot function is called
        createHistogramPlot(benchmarkName, data1, data2, tempPath, "timing")

        // THEN a file with the correct name should exist in the temporary directory
        val expectedFileName = "${benchmarkName}_timing_histogram.png"
        val expectedFile = tempPath.resolve(expectedFileName)

        assertThat(expectedFile.exists()).isTrue()
        assertThat(expectedFile.name).isEqualTo(expectedFileName)

        // AND the console output should confirm the file was saved
        val consoleOutput = outContent.toString()
        val actualPathInOutput =
            consoleOutput.substringAfter("Saved histogram for MyPlotTest - timing to: ").trim()
        assertThat(actualPathInOutput).isEqualTo(expectedFile.toRealPath().toString())

        // FINALLY, restore the original standard output
        System.setOut(originalOut)
    }
}
