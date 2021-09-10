/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark.macro

import androidx.benchmark.MetricResult
import androidx.benchmark.Outputs
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertFailsWith

@RunWith(AndroidJUnit4::class)
@SmallTest
public class IdeSummaryStringTest {
    private fun createAbsoluteTracePaths(
        @Suppress("SameParameterValue") count: Int
    ) = List(count) {
        File(Outputs.dirUsableByAppAndShell, "iter$it.trace").absolutePath
    }

    @Test
    public fun minimalSample() {
        val metricResult = MetricResult("Metric", listOf(0.0, 1.1, 2.2))

        assertEquals(0, metricResult.minIndex)
        assertEquals(1, metricResult.medianIndex)
        assertEquals(2, metricResult.maxIndex)
        val absoluteTracePaths = createAbsoluteTracePaths(3)
        val (summaryV1, summaryV2) = ideSummaryStrings(
            warningLines = "",
            benchmarkName = "foo",
            metricResults = listOf(metricResult),
            absoluteTracePaths = absoluteTracePaths
        )
        assertEquals(
            """
                |foo
                |  Metric   min 0.0,   median 1.1,   max 2.2
                |
            """.trimMargin(),
            summaryV1
        )
        assertEquals(
            """
                |foo
                |  Metric   [min 0.0](file://iter0.trace),   [median 1.1](file://iter1.trace),   [max 2.2](file://iter2.trace)
                |    Traces: Iteration [0](file://iter0.trace) [1](file://iter1.trace) [2](file://iter2.trace)
                |
            """.trimMargin(),
            summaryV2
        )
    }

    @Test
    public fun complexSample() {
        val metric1 = MetricResult("Metric1", listOf(0.0, 1.0, 2.0))
        val metric2 = MetricResult("Metric2", listOf(222.0, 111.0, 0.0))
        val absoluteTracePaths = createAbsoluteTracePaths(3)
        val (summaryV1, summaryV2) = ideSummaryStrings(
            warningLines = "",
            benchmarkName = "foo",
            metricResults = listOf(metric1, metric2),
            absoluteTracePaths = absoluteTracePaths
        )
        assertEquals(
            """
                |foo
                |  Metric1   min   0.0,   median   1.0,   max   2.0
                |  Metric2   min   0.0,   median 111.0,   max 222.0
                |
            """.trimMargin(),
            summaryV1
        )
        assertEquals(
            """
                |foo
                |  Metric1   [min   0.0](file://iter0.trace),   [median   1.0](file://iter1.trace),   [max   2.0](file://iter2.trace)
                |  Metric2   [min   0.0](file://iter2.trace),   [median 111.0](file://iter1.trace),   [max 222.0](file://iter0.trace)
                |    Traces: Iteration [0](file://iter0.trace) [1](file://iter1.trace) [2](file://iter2.trace)
                |
            """.trimMargin(),
            summaryV2
        )
    }

    @Test
    public fun warningSample() {
        val metricResult = MetricResult("Metric", listOf(0.0, 1.0, 2.0))
        val absoluteTracePaths = createAbsoluteTracePaths(3)
        val (summaryV1, summaryV2) = ideSummaryStrings(
            warningLines = "warning\nstring\n",
            benchmarkName = "foo",
            metricResults = listOf(metricResult),
            absoluteTracePaths = absoluteTracePaths
        )
        assertEquals(
            """
                |warning
                |string
                |foo
                |  Metric   min 0.0,   median 1.0,   max 2.0
                |
            """.trimMargin(),
            summaryV1
        )
        assertEquals(
            """
                |warning
                |string
                |foo
                |  Metric   [min 0.0](file://iter0.trace),   [median 1.0](file://iter1.trace),   [max 2.0](file://iter2.trace)
                |    Traces: Iteration [0](file://iter0.trace) [1](file://iter1.trace) [2](file://iter2.trace)
                |
            """.trimMargin(),
            summaryV2
        )
    }

    @Test
    public fun requireNotEmpty() {
        assertFailsWith<IllegalArgumentException> {
            ideSummaryStrings(
                warningLines = "",
                benchmarkName = "foo",
                metricResults = emptyList(),
                absoluteTracePaths = emptyList()
            ).first
        }
    }
}
