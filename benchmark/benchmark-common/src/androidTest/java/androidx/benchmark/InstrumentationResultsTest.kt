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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
public class InstrumentationResultsTest {
    @Test
    public fun ideSummary_alignment() {
        val summary1 = InstrumentationResults.ideSummaryLine(
            key = "foo",
            nanos = 1000.0,
            allocations = 100.0,
            traceRelPath = "path",
            profilerResult = null
        )
        val summary2 = InstrumentationResults.ideSummaryLine(
            key = "fooBarLongerKey",
            nanos = 10000.0,
            allocations = 0.0,
            traceRelPath = "path",
            profilerResult = null
        )

        assertEquals(
            summary1.indexOf("foo"),
            summary2.indexOf("foo")
        )
    }

    @Test
    public fun ideSummary_allocs() {
        assertEquals(
            "        1,000   ns    foo",
            InstrumentationResults.ideSummaryLine("foo", 1000.0, null, null, null)
        )
        assertEquals(
            "        1,000   ns          10 allocs    foo",
            InstrumentationResults.ideSummaryLine("foo", 1000.0, 10.0, null, null)
        )
    }

    @Test
    public fun ideSummary_decimal() {
        assertEquals(
            "        1,000   ns    foo",
            InstrumentationResults.ideSummaryLine("foo", 1000.0, null, null, null)
        )
        assertEquals(
            "          100   ns    foo", // 10ths not shown ...
            InstrumentationResults.ideSummaryLine("foo", 100.4, null, null, null)
        )
        assertEquals(
            "           99.9 ns    foo", // ... until value is < 100
            InstrumentationResults.ideSummaryLine("foo", 99.9, null, null, null)
        )
        assertEquals(
            "            1.0 ns    foo",
            InstrumentationResults.ideSummaryLine("foo", 1.0, null, null, null)
        )
    }

    @Test
    public fun ideSummary_traceRelPath() {
        assertEquals(
            "        1,000   ns    [trace](file://bar)    foo",
            InstrumentationResults.ideSummaryLine("foo", 1000.0, null, "bar", null)
        )
    }

    @Test
    public fun ideSummary_profilerResult() {
        assertEquals(
            "        1,000   ns    [Trace Label](file://tracePath.trace)    foo",
            InstrumentationResults.ideSummaryLine(
                key = "foo",
                nanos = 1000.0,
                allocations = null,
                traceRelPath = null,
                profilerResult = Profiler.ResultFile(
                    label = "Trace Label",
                    outputRelativePath = "tracePath.trace"
                )
            )
        )
    }
}
