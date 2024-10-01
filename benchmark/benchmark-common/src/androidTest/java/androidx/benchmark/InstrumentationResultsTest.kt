/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.benchmark

import androidx.benchmark.json.BenchmarkData
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class InstrumentationResultsTest {
    @Before
    fun setup() {
        // flush any scheduled ide warnings so that tests below aren't affected
        InstrumentationResults.ideSummary(
            testName = "foo",
            measurements =
                Measurements(
                    singleMetrics = listOf(MetricResult("Metric", listOf(0.0))),
                    sampledMetrics = emptyList()
                )
        )
    }

    @Test
    fun ideSummaryBasicMicro_alignment() {
        val summary1 =
            InstrumentationResults.ideSummaryBasicMicro(
                benchmarkName = "foo",
                nanos = 1000.0,
                allocations = 100.0,
                profilerResults = emptyList()
            )
        val summary2 =
            InstrumentationResults.ideSummaryBasicMicro(
                benchmarkName = "fooBarLongerKey",
                nanos = 10000.0,
                allocations = 0.0,
                profilerResults = emptyList()
            )
        assertEquals(summary1.indexOf("foo"), summary2.indexOf("foo"))
    }

    @Test
    fun ideSummaryBasicMicro_allocs() {
        assertEquals(
            "        1,000   ns    foo",
            InstrumentationResults.ideSummaryBasicMicro("foo", 1000.0, null, emptyList())
        )
        assertEquals(
            "        1,000   ns          10 allocs    foo",
            InstrumentationResults.ideSummaryBasicMicro("foo", 1000.0, 10.0, emptyList())
        )
    }

    @Test
    fun ideSummaryBasicMicro_decimal() {
        assertEquals(
            "        1,000   ns    foo",
            InstrumentationResults.ideSummaryBasicMicro("foo", 1000.0, null, emptyList())
        )
        assertEquals(
            "          100   ns    foo", // 10ths not shown ...
            InstrumentationResults.ideSummaryBasicMicro("foo", 100.4, null, emptyList())
        )
        assertEquals(
            "           99.9 ns    foo", // ... until value is < 100
            InstrumentationResults.ideSummaryBasicMicro("foo", 99.9, null, emptyList())
        )
        assertEquals(
            "            1.0 ns    foo",
            InstrumentationResults.ideSummaryBasicMicro("foo", 1.0, null, emptyList())
        )
    }

    @Test
    fun ideSummaryBasicMicro_profilerResult() {
        assertEquals(
            "        1,000   ns    [Trace Label](file://tracePath.trace)    foo",
            InstrumentationResults.ideSummaryBasicMicro(
                benchmarkName = "foo",
                nanos = 1000.0,
                allocations = null,
                listOf(
                    Profiler.ResultFile.of(
                        label = "Trace Label",
                        outputRelativePath = "tracePath.trace",
                        type = BenchmarkData.TestResult.ProfilerOutput.Type.MethodTrace,
                        source = MethodTracing
                    )
                )
            )
        )
    }

    private fun createAbsoluteTracePaths(@Suppress("SameParameterValue") count: Int) =
        List(count) { File(Outputs.dirUsableByAppAndShell, "iter$it.trace").absolutePath }

    @Test
    fun ideSummary_singleMinimal() {
        val metricResult = MetricResult("Metric", listOf(0.0, 1.1, 2.2))

        assertEquals(0, metricResult.minIndex)
        assertEquals(1, metricResult.medianIndex)
        assertEquals(2, metricResult.maxIndex)
        val absoluteTracePaths = createAbsoluteTracePaths(3)
        val summary =
            InstrumentationResults.ideSummary(
                testName = "foo",
                measurements =
                    Measurements(
                        singleMetrics = listOf(metricResult),
                        sampledMetrics = emptyList()
                    ),
                iterationTracePaths = absoluteTracePaths
            )
        assertEquals(
            """
                |foo
                |  Metric   [min 0.0](file://iter0.trace),   [median 1.1](file://iter1.trace),   [max 2.2](file://iter2.trace)
                |    Traces: Iteration [0](file://iter0.trace) [1](file://iter1.trace) [2](file://iter2.trace)
                |
            """
                .trimMargin(),
            summary.summaryV2
        )
        // v1 is deprecated and should be the same as v2
        assertEquals(summary.summaryV1, summary.summaryV2)
    }

    @Test
    fun ideSummary_singleComplex() {
        val metric1 = MetricResult("Metric1", listOf(0.0, 1.0, 2.0))
        val metric2 = MetricResult("Metric2", listOf(222.0, 111.0, 0.0))
        val summary =
            InstrumentationResults.ideSummary(
                testName = "foo",
                measurements =
                    Measurements(
                        singleMetrics = listOf(metric1, metric2),
                        sampledMetrics = emptyList()
                    ),
                iterationTracePaths = createAbsoluteTracePaths(3)
            )
        assertEquals(
            """
                |foo
                |  Metric1   [min   0.0](file://iter0.trace),   [median   1.0](file://iter1.trace),   [max   2.0](file://iter2.trace)
                |  Metric2   [min   0.0](file://iter2.trace),   [median 111.0](file://iter1.trace),   [max 222.0](file://iter0.trace)
                |    Traces: Iteration [0](file://iter0.trace) [1](file://iter1.trace) [2](file://iter2.trace)
                |
            """
                .trimMargin(),
            summary.summaryV2
        )
        // v1 is deprecated and should be the same as v2
        assertEquals(summary.summaryV1, summary.summaryV2)
    }

    @Test
    fun ideSummary_sampledMinimal() {
        val metricResult = MetricResult("Metric1", List(101) { it.toDouble() })
        val summary =
            InstrumentationResults.ideSummary(
                testName = "foo",
                measurements =
                    Measurements(
                        singleMetrics = emptyList(),
                        sampledMetrics = listOf(metricResult)
                    ),
                iterationTracePaths = createAbsoluteTracePaths(3)
            )
        assertEquals(
            """
                |foo
                |  Metric1   P50   50.0,   P90   90.0,   P95   95.0,   P99   99.0
                |    Traces: Iteration [0](file://iter0.trace) [1](file://iter1.trace) [2](file://iter2.trace)
                |
            """
                .trimMargin(),
            summary.summaryV2
        )
        // v1 is deprecated and should be the same as v2
        assertEquals(summary.summaryV1, summary.summaryV2)
    }

    @Test
    public fun ideSummary_complex() {
        val single = MetricResult("Metric1", listOf(0.0, 1.0, 2.0))
        val sampled = MetricResult("Metric2", List(101) { it.toDouble() })
        val absoluteTracePaths = createAbsoluteTracePaths(3)
        val summary =
            InstrumentationResults.ideSummary(
                testName = "foo",
                measurements =
                    Measurements(singleMetrics = listOf(single), sampledMetrics = listOf(sampled)),
                iterationTracePaths = absoluteTracePaths
            )
        assertEquals(
            """
                |foo
                |  Metric1   [min   0.0](file://iter0.trace),   [median   1.0](file://iter1.trace),   [max   2.0](file://iter2.trace)
                |  Metric2   P50   50.0,   P90   90.0,   P95   95.0,   P99   99.0
                |    Traces: Iteration [0](file://iter0.trace) [1](file://iter1.trace) [2](file://iter2.trace)
                |
            """
                .trimMargin(),
            summary.summaryV2
        )
        // v1 is deprecated and should be the same as v2
        assertEquals(summary.summaryV1, summary.summaryV2)
    }

    @Test
    fun ideSummary_warning_singleLine() {
        val metricResult = MetricResult("timeNs", listOf(0.0, 1.0, 2.0))

        InstrumentationResults.scheduleIdeWarningOnNextReport("warning\nstring")
        val summary =
            InstrumentationResults.ideSummary(
                testName = "foo",
                measurements =
                    Measurements(
                        singleMetrics = listOf(metricResult),
                        sampledMetrics = emptyList()
                    ),
            )
        assertEquals(
            """
                |warning
                |string
                |            0.0 ns    foo
            """
                .trimMargin(),
            summary.summaryV2
        )
        // v1 is deprecated and should be the same as v2
        assertEquals(summary.summaryV1, summary.summaryV2)
    }

    @Test
    fun ideSummary_warning() {
        val metricResult = MetricResult("Metric", listOf(0.0, 1.0, 2.0))
        val absoluteTracePaths = createAbsoluteTracePaths(3)

        InstrumentationResults.scheduleIdeWarningOnNextReport("warning\nstring")
        val summary =
            InstrumentationResults.ideSummary(
                testName = "foo",
                measurements =
                    Measurements(
                        singleMetrics = listOf(metricResult),
                        sampledMetrics = emptyList()
                    ),
                iterationTracePaths = absoluteTracePaths
            )
        assertEquals(
            """
                |warning
                |string
                |foo
                |  Metric   [min 0.0](file://iter0.trace),   [median 1.0](file://iter1.trace),   [max 2.0](file://iter2.trace)
                |    Traces: Iteration [0](file://iter0.trace) [1](file://iter1.trace) [2](file://iter2.trace)
                |
            """
                .trimMargin(),
            summary.summaryV2
        )
        // v1 is deprecated and should be the same as v2
        assertEquals(summary.summaryV1, summary.summaryV2)
    }

    @Test
    fun ideSummary_requireMeasurementsNotEmpty() {
        assertFailsWith<IllegalArgumentException> {
            InstrumentationResults.ideSummary(
                measurements =
                    Measurements(singleMetrics = emptyList(), sampledMetrics = emptyList()),
            )
        }
    }
}

private fun <T> assertEquals(expected: T, actual: T) = assertThat(actual).isEqualTo(expected)
