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

package androidx.benchmark.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.PerfettoRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.tracing.Trace
import androidx.tracing.trace
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class PerfettoOverheadBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    val perfettoRule = PerfettoRule()

    /**
     * Empty baseline, no tracing. Expect similar results to [TrivialJavaBenchmark.nothing].
     */
    @Test
    fun empty() = benchmarkRule.measureRepeated {}

    /**
     * The trace section within runWithTimingDisabled, even though not measured, can impact the
     * results of a small benchmark significantly.
     */
    @Test
    fun runWithTimingDisabled() = benchmarkRule.measureRepeated {
        runWithTimingDisabled { /* nothing*/ }
    }

    /**
     * Trace section adds ~5us (depending on many factors) in this ideal case, but will be
     * significantly worse in a real benchmark, as there's more computation to interfere with.
     */
    @Test
    fun traceBeginEnd() = benchmarkRule.measureRepeated {
        Trace.beginSection("foo")
        Trace.endSection()
    }

    /**
     * Dupe of [traceBeginEnd], just using [trace].
     */
    @Test
    fun traceBlock() = benchmarkRule.measureRepeated {
        trace("foo") { /* nothing */ }
    }
}
