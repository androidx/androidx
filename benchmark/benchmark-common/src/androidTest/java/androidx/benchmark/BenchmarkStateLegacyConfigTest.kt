/*
 * Copyright 2018 The Android Open Source Project
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
import androidx.test.filters.LargeTest
import androidx.test.filters.SmallTest
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class BenchmarkStateLegacyConfigTest {
    private fun validateConfig(
        config: MicrobenchmarkPhase.Config,
        expectedWarmups: Int?,
        expectedMeasurements: Int,
        expectedIterations: Int?,
        expectedUsesProfiler: Boolean = false,
        expectedProfilerIterations: Int = 0,
    ) {
        val state = BenchmarkStateLegacy(config)
        var count = 0
        while (state.keepRunning()) {
            // This spin loop works around an issue where nanoTime is only precise to 30us on some
            // devices. This was reproduced on api 17 and emulators api 33. (b/331226761)
            val start = System.nanoTime()
            @Suppress("ControlFlowWithEmptyBody") while (System.nanoTime() == start) {}
            count++
        }

        val calculatedIterations =
            state.warmupRepeats + expectedMeasurements * state.iterationsPerRepeat

        val usesProfiler = config.generatePhases().any { it.profiler != null }

        assertEquals(expectedUsesProfiler, usesProfiler)
        if (!usesProfiler) {
            assertEquals(calculatedIterations, count)
        } else if (config.profiler!!.requiresSingleMeasurementIteration) {
            assertEquals(calculatedIterations + expectedProfilerIterations, count)
        } else {
            throw IllegalStateException("Test doesn't support validating profiler $config.profiler")
        }
        if (expectedIterations != null) {
            assertEquals(expectedIterations, count)
        }
        if (expectedWarmups != null) {
            assertEquals(expectedWarmups, state.warmupRepeats)
        }
        assertNotEquals(0.0, state.getMinTimeNanos()) // just verify some value is set
    }

    @Test
    fun dryRunMode() =
        validateConfig(
            MicrobenchmarkPhase.Config(
                dryRunMode = true, // everything after is ignored
                startupMode = true,
                simplifiedTimingOnlyMode = true,
                profiler = null,
                profilerPerfCompareMode = false,
                warmupCount = 100,
                measurementCount = 1000,
                metrics = arrayOf(TimeCapture()),
            ),
            expectedWarmups = 0,
            expectedMeasurements = 1,
            expectedIterations = 1,
        )

    @Test
    fun startupMode() =
        validateConfig(
            MicrobenchmarkPhase.Config(
                dryRunMode = false,
                startupMode = true, // everything after is ignored
                simplifiedTimingOnlyMode = true,
                profiler = null,
                profilerPerfCompareMode = false,
                warmupCount = 100,
                measurementCount = 1000,
                metrics = arrayOf(TimeCapture()),
            ),
            expectedWarmups = 0,
            expectedMeasurements = 10,
            expectedIterations = 10,
        )

    @LargeTest // due to dynamic warmup
    @Test
    fun basic() =
        validateConfig(
            MicrobenchmarkPhase.Config(
                dryRunMode = false,
                startupMode = false,
                simplifiedTimingOnlyMode = false,
                profiler = null,
                profilerPerfCompareMode = false,
                warmupCount = null,
                measurementCount = null,
                metrics = arrayOf(TimeCapture()),
            ),
            expectedWarmups = null,
            expectedMeasurements = 55, // includes allocations
            expectedIterations = null,
        )

    @Test
    fun basicOverride() =
        validateConfig(
            MicrobenchmarkPhase.Config(
                dryRunMode = false,
                startupMode = false,
                simplifiedTimingOnlyMode = false,
                profiler = null,
                profilerPerfCompareMode = false,
                warmupCount = 10,
                measurementCount = 100,
                metrics = arrayOf(TimeCapture()),
            ),
            expectedWarmups = 10,
            expectedMeasurements = 105, // includes allocations
            expectedIterations = null, // iterations are dynamic
        )

    @Test
    fun profilerMethodTracing() =
        validateConfig(
            MicrobenchmarkPhase.Config(
                dryRunMode = false,
                startupMode = false,
                simplifiedTimingOnlyMode = false,
                profiler = MethodTracing,
                profilerPerfCompareMode = false,
                warmupCount = 5,
                measurementCount = 10,
                metrics = arrayOf(TimeCapture()),
            ),
            expectedWarmups = 5,
            expectedMeasurements = 15, // 10 timing + 5 allocations
            expectedIterations = null, // iterations are dynamic
            expectedUsesProfiler = true,
            expectedProfilerIterations = 1,
        )

    @Test
    fun profilerMethodTracing_perfCompareMode() =
        validateConfig(
            MicrobenchmarkPhase.Config(
                dryRunMode = false,
                startupMode = false,
                simplifiedTimingOnlyMode = false,
                profiler = MethodTracing,
                profilerPerfCompareMode = true,
                warmupCount = 5,
                measurementCount = 10,
                metrics = arrayOf(TimeCapture()),
            ),
            expectedWarmups = 5,
            expectedMeasurements = 15,
            expectedIterations =
                30, // fixed iterations to be consistent between measurement/profiling
            expectedUsesProfiler = true,
            expectedProfilerIterations = 10,
        )

    @Test
    fun simplifiedTimingOnlyMode_ignoresProfiler() =
        validateConfig(
            MicrobenchmarkPhase.Config(
                dryRunMode = false,
                startupMode = false,
                simplifiedTimingOnlyMode = true,
                profiler = MethodTracing,
                profilerPerfCompareMode = true,
                warmupCount = 100,
                measurementCount = 10,
                metrics = arrayOf(TimeCapture()),
            ),
            expectedWarmups = 100,
            expectedMeasurements = 10,
            expectedIterations = null,
            expectedUsesProfiler = false, // ignored due to simplifiedTimingOnlyMode
        )
}
