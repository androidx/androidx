/*
 * Copyright 2022 The Android Open Source Project
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

import android.os.Build
import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.MicrobenchmarkConfig
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.benchmark.perfetto.PerfettoCapture
import androidx.benchmark.perfetto.PerfettoCapture.PerfettoSdkConfig
import androidx.benchmark.perfetto.PerfettoCapture.PerfettoSdkConfig.InitialProcessState
import androidx.benchmark.perfetto.PerfettoHelper.Companion.isAbiSupported
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import junit.framework.TestCase.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalBenchmarkConfigApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.R) // TODO(234351579): Support API < 30
/**
 * Measures overhead of Perfetto SDK tracing [androidx.tracing.perfetto].
 *
 * @see [PerfettoOverheadBenchmark]
 */
class PerfettoSdkOverheadBenchmark {
    private val targetPackage =
        InstrumentationRegistry.getInstrumentation().targetContext.packageName

    @get:Rule
    val benchmarkRule = BenchmarkRule(MicrobenchmarkConfig(shouldEnableTraceAppTag = true))

    private val testData = Array(50_000) { UUID.randomUUID().toString() }

    @Before
    fun setUp() = assumeTrue(isAbiSupported()) // We need all tests to work to compare their results

    /**
     * Empty baseline, no tracing. Expect similar results to [TrivialJavaBenchmark.nothing].
     */
    @Test
    fun empty() = benchmarkRule.measureRepeated { /* nothing */ }

    /**
     * The trace section within runWithTimingDisabled, even though not measured, can impact the
     * results of a small benchmark significantly.
     */
    @Test
    fun runWithTimingDisabled() = benchmarkRule.measureRepeated {
        runWithTimingDisabled { /* nothing */ }
    }

    /** Measuring overhead of [androidx.tracing.perfetto.PerfettoSdkTrace]. */
    @Test
    fun traceBeginEnd_perfettoSdkTrace() {
        PerfettoCapture().enableAndroidxTracingPerfetto(
            PerfettoSdkConfig(targetPackage, InitialProcessState.Alive)
        ).let { (resultCode, _) ->
            assertTrue(
                "Ensuring Perfetto SDK is enabled",
                resultCode in arrayOf(1, 2) // 1 = success, 2 = already enabled
            )
        }
        var ix = 0
        benchmarkRule.measureRepeated {
            androidx.tracing.perfetto.PerfettoSdkTrace.beginSection(testData[ix++ % testData.size])
            androidx.tracing.perfetto.PerfettoSdkTrace.endSection()
        }
    }

    /** Measuring overhead of [android.os.Trace] as a reference point. */
    @Test
    fun traceBeginEnd_androidOsTrace() {
        var ix = 0
        benchmarkRule.measureRepeated {
            android.os.Trace.beginSection(testData[ix++ % testData.size])
            android.os.Trace.endSection()
        }
    }
}
