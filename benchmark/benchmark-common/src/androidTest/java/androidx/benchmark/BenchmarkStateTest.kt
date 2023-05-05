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

import android.Manifest
import androidx.benchmark.BenchmarkState.Companion.ExperimentalExternalReport
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@LargeTest
@RunWith(AndroidJUnit4::class)
class BenchmarkStateTest {
    private fun us2ns(ms: Long): Long = TimeUnit.MICROSECONDS.toNanos(ms)

    @get:Rule
    val writePermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)!!

    /**
     * Run the block, and then spin-loop until durationUs has elapsed.
     *
     * Note: block must take less time than durationUs
     */
    private inline fun runAndSpin(durationUs: Long, crossinline block: () -> Unit = {}) {
        val start = System.nanoTime()
        block()
        val end = start + us2ns(durationUs)

        @Suppress("ControlFlowWithEmptyBody") // intentionally spinning
        while (System.nanoTime() < end) {}
    }

    @Test
    @FlakyTest(bugId = 187711141)
    fun validateMetrics() {
        val state = BenchmarkState()
        while (state.keepRunning()) {
            runAndSpin(durationUs = 300) {
                // note, important here to not do too much work - this test may run on an
                // extremely slow device, or wacky emulator.
                allocate(40)
            }

            state.pauseTiming()
            runAndSpin(durationUs = 700) {
                allocate(80)
            }
            state.resumeTiming()
        }
        // The point of these asserts are to verify that pause/resume work, and that metrics that
        // come out are reasonable, not perfect - this isn't always run in stable perf environments
        val medianTime = state.getReport().getMetricResult("timeNs").median.toLong()
        assertTrue(
            "median time (ns) $medianTime should be roughly 300us",
            medianTime in us2ns(280)..us2ns(900)
        )
        val medianAlloc = state.getReport().getMetricResult("allocationCount").median.toInt()
        assertTrue(
            "median allocs $medianAlloc should be approximately 40",
            medianAlloc in 40..50
        )
    }

    @Test
    fun keepRunningMissingResume() {
        val state = BenchmarkState()

        assertEquals(true, state.keepRunning())
        state.pauseTiming()
        assertFailsWith<IllegalStateException> { state.keepRunning() }
    }

    @Test
    fun pauseCalledTwice() {
        val state = BenchmarkState()

        assertEquals(true, state.keepRunning())
        state.pauseTiming()
        assertFailsWith<IllegalStateException> { state.pauseTiming() }
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun priorityJitThread() {
        assertEquals(
            "JIT priority should not yet be modified",
            ThreadPriority.JIT_INITIAL_PRIORITY,
            ThreadPriority.getJit()
        )

        // verify priority is only bumped during loop (NOTE: lower number means higher priority)
        val state = BenchmarkState()
        while (state.keepRunning()) {
            val currentJitPriority = ThreadPriority.getJit()
            assertTrue(
                "JIT priority should be bumped," +
                    " is $currentJitPriority vs ${ThreadPriority.JIT_INITIAL_PRIORITY}",
                currentJitPriority < ThreadPriority.JIT_INITIAL_PRIORITY
            )
        }
        assertEquals(ThreadPriority.JIT_INITIAL_PRIORITY, ThreadPriority.getJit())
    }

    @Test
    fun priorityBenchThread() {
        val initialPriority = ThreadPriority.get()
        assertNotEquals(
            "Priority should not be max",
            ThreadPriority.HIGH_PRIORITY,
            ThreadPriority.get()
        )

        // verify priority is only bumped during loop (NOTE: lower number means higher priority)
        val state = BenchmarkState()
        while (state.keepRunning()) {
            val currentPriority = ThreadPriority.get()
            assertTrue(
                "Priority should be bumped, is $currentPriority",
                currentPriority < initialPriority
            )
        }
        assertEquals(initialPriority, ThreadPriority.get())
    }

    private fun iterationCheck(checkingForThermalThrottling: Boolean) {
        // disable thermal throttle checks, since it can cause loops to be thrown out
        // note that this bypasses allocation count
        val state = BenchmarkState(simplifiedTimingOnlyMode = checkingForThermalThrottling)
        var total = 0
        while (state.keepRunning()) {
            total++
        }

        val report = state.getReport()
        val expectedRepeatCount = state.repeatCountTime +
            if (!checkingForThermalThrottling) BenchmarkState.REPEAT_COUNT_ALLOCATION else 0
        val expectedCount = report.warmupIterations + report.repeatIterations * expectedRepeatCount
        assertEquals(expectedCount, total)

        if (Arguments.iterations != null) {
            assertEquals(Arguments.iterations, report.repeatIterations)
        }

        // verify we're not in warmup mode
        assertTrue(report.warmupIterations > 0)
        assertTrue(report.repeatIterations > 1)
        // verify we're not running in a special mode that affects repeat count (dry run, profiling)
        assertEquals(50, state.repeatCountTime)
    }

    @Test
    fun iterationCheck_simple() {
        iterationCheck(checkingForThermalThrottling = true)
    }

    @Test
    fun iterationCheck_withAllocations() {
        // In any of these conditions, it's known that throttling won't happen, so it's safe
        // to check for allocation count, by setting checkingForThermalThrottling = false
        assumeTrue(
            CpuInfo.locked ||
                IsolationActivity.sustainedPerformanceModeInUse ||
                Errors.isEmulator
        )
        iterationCheck(checkingForThermalThrottling = false)
    }

    @Test
    @Suppress("DEPRECATION")
    fun bundle() {
        val bundle = BenchmarkState().apply {
            while (keepRunning()) {
                // nothing, we're ignoring numbers
            }
        }.getFullStatusReport(
            key = "foo",
            reportMetrics = true,
            tracePath = Outputs.outputDirectory.absolutePath + "/bar"
        )

        val displayStringV1 = (bundle.get("android.studio.display.benchmark") as String)
        val displayStringV2 = (bundle.get("android.studio.v2display.benchmark") as String)
        assertTrue("$displayStringV1 should contain foo", displayStringV1.contains("foo"))
        assertTrue("$displayStringV2 should contain foo", displayStringV2.contains("foo"))
        assertTrue(
            "$displayStringV2 should contain [trace](file://bar)",
            displayStringV2.contains("[trace](file://bar)")
        )

        // check attribute presence and naming
        val prefix = Errors.PREFIX

        // including metric name
        assertNotNull(bundle.get("${prefix}time_nanos_min"))
        assertNotNull(bundle.get("${prefix}time_nanos_median"))
        assertNotNull(bundle.get("${prefix}time_nanos_stddev"))

        assertNotNull(bundle.get("${prefix}allocation_count_min"))
        assertNotNull(bundle.get("${prefix}allocation_count_median"))
        assertNotNull(bundle.get("${prefix}allocation_count_stddev"))
    }

    @Test
    fun notStarted() {
        val initialPriority = ThreadPriority.get()
        try {
            BenchmarkState().getReport().getMetricResult("timeNs").median
            fail("expected exception")
        } catch (e: IllegalStateException) {
            assertEquals(initialPriority, ThreadPriority.get())
            assertTrue(e.message!!.contains("wasn't started"))
        }
    }

    @Test
    fun notFinished() {
        val initialPriority = ThreadPriority.get()
        try {
            BenchmarkState().run {
                keepRunning()
                getReport().getMetricResult("timeNs").median
            }
            fail("expected exception")
        } catch (e: IllegalStateException) {
            assertEquals(initialPriority, ThreadPriority.get())
            assertTrue(e.message!!.contains("hasn't finished"))
            assertTrue(e.message!!.contains("benchmarkRule.measureRepeated {}"))
        }
    }

    @Suppress("DEPRECATION")
    @OptIn(ExperimentalExternalReport::class)
    @Test
    fun reportResult() {
        BenchmarkState.reportData(
            className = "className",
            testName = "testName",
            totalRunTimeNs = 900000000,
            dataNs = listOf(100L, 200L, 300L),
            warmupIterations = 1,
            thermalThrottleSleepSeconds = 0,
            repeatIterations = 1
        )
        val expectedReport = BenchmarkResult(
            className = "className",
            testName = "testName",
            totalRunTimeNs = 900000000,
            metrics = listOf(
                MetricResult(
                    name = "timeNs",
                    data = listOf(100.0, 200.0, 300.0)
                )
            ),
            repeatIterations = 1,
            thermalThrottleSleepSeconds = 0,
            warmupIterations = 1
        )
        assertEquals(expectedReport, ResultWriter.reports.last())
    }

    private fun validateProfilerUsage(simplifiedTimingOnlyMode: Boolean?) {
        try {
            profilerOverride = StackSamplingLegacy

            val benchmarkState = if (simplifiedTimingOnlyMode != null) {
                BenchmarkState(simplifiedTimingOnlyMode = simplifiedTimingOnlyMode)
            } else {
                BenchmarkState()
            }

            // count iters with profiler enabled vs disabled
            var profilerDisabledIterations = 0
            var profilerEnabledIterations = 0
            var profilerAllocationIterations = 0
            while (benchmarkState.keepRunning()) {
                if (StackSamplingLegacy.isRunning) {
                    profilerEnabledIterations++
                } else {
                    profilerDisabledIterations++

                    if (profilerEnabledIterations != 0) {
                        // profiler will only be disabled after running during allocation phase
                        profilerAllocationIterations++
                    }
                }
            }

            if (simplifiedTimingOnlyMode == true) {
                // profiler should be always disabled
                assertNotEquals(0, profilerDisabledIterations)
                assertEquals(0, profilerEnabledIterations)
                assertEquals(0, profilerAllocationIterations)
            } else {
                // first, profiler disabled, then enabled until end
                assertNotEquals(0, profilerDisabledIterations)
                assertNotEquals(0, profilerEnabledIterations)
                assertNotEquals(0, profilerAllocationIterations)
            }
        } finally {
            profilerOverride = null
        }
    }

    @Test fun profiler_default() = validateProfilerUsage(null)
    @Test fun profiler_false() = validateProfilerUsage(false)
    @Test fun profiler_true() = validateProfilerUsage(true)

    @OptIn(ExperimentalBenchmarkStateApi::class)
    @Test
    fun experimentalConstructor() {
        // min values that don't fail
        BenchmarkState(
            warmupCount = null,
            measurementCount = 1
        )

        // test failures
        assertFailsWith<IllegalArgumentException> {
            BenchmarkState(warmupCount = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            BenchmarkState(measurementCount = 0)
        }
    }

    @OptIn(ExperimentalBenchmarkStateApi::class)
    private fun validateIters(
        warmupCount: Int?,
        measurementCount: Int
    ) {
        val state = BenchmarkState(
            warmupCount = warmupCount,
            measurementCount = measurementCount
        )
        var count = 0
        while (state.keepRunning()) {
            count++
        }
        if (warmupCount != null) {
            assertEquals(
                warmupCount + // warmup
                    measurementCount * state.iterationsPerRepeat + // timing
                    BenchmarkState.REPEAT_COUNT_ALLOCATION * state.iterationsPerRepeat, // allocs
                count
            )
        }
        assertEquals(
            measurementCount,
            state.getMeasurementTimeNs().size
        )
    }

    @Test
    @Ignore("b/278737712")
    fun experimentalIters() {
        validateIters(
            warmupCount = 1,
            measurementCount = 1
        )
        validateIters(
            warmupCount = 3,
            measurementCount = 5
        )
        validateIters(
            warmupCount = 10000,
            measurementCount = 1
        )
    }
}
