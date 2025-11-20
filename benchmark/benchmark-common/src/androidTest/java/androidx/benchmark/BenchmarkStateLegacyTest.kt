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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import java.util.concurrent.TimeUnit
import kotlin.test.assertFailsWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class BenchmarkStateLegacyTest {
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
        val state = BenchmarkStateLegacy()
        while (state.keepRunning()) {
            runAndSpin(durationUs = 300) {
                // note, important here to not do too much work - this test may run on an
                // extremely slow device, or wacky emulator.
                allocate(40)
            }

            state.pauseTiming()
            runAndSpin(durationUs = 700) { allocate(80) }
            state.resumeTiming()
        }
        // The point of these asserts are to verify that pause/resume work, and that metrics that
        // come out are reasonable, not perfect - this isn't always run in stable perf environments
        val medianTime = state.peekTestResult().metrics["timeNs"]!!.median.toLong()
        assertTrue(
            "median time (ns) $medianTime should be roughly 300us",
            medianTime in us2ns(280)..us2ns(900),
        )
        val medianAlloc = state.peekTestResult().metrics["allocationCount"]!!.median.toInt()
        assertTrue("median allocs $medianAlloc should be approximately 40", medianAlloc in 40..50)
    }

    @Test
    fun keepRunningMissingResume() {
        val state = BenchmarkStateLegacy()

        assertEquals(true, state.keepRunning())
        state.pauseTiming()
        assertFailsWith<IllegalStateException> { state.keepRunning() }
    }

    @Test
    fun pauseCalledTwice() {
        val state = BenchmarkStateLegacy()

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
            ThreadPriority.getJit(),
        )

        // verify priority is only bumped during loop (NOTE: lower number means higher priority)
        val state = BenchmarkStateLegacy()
        while (state.keepRunning()) {
            val currentJitPriority = ThreadPriority.getJit()
            assertTrue(
                "JIT priority should be bumped," +
                    " is $currentJitPriority vs ${ThreadPriority.JIT_INITIAL_PRIORITY}",
                currentJitPriority < ThreadPriority.JIT_INITIAL_PRIORITY,
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
            ThreadPriority.get(),
        )

        // verify priority is only bumped during loop (NOTE: lower number means higher priority)
        val state = BenchmarkStateLegacy()
        while (state.keepRunning()) {
            val currentPriority = ThreadPriority.get()
            assertTrue(
                "Priority should be bumped, is $currentPriority",
                currentPriority < initialPriority,
            )
        }
        assertEquals(initialPriority, ThreadPriority.get())
    }

    private fun iterationCheck(simplifiedTimingOnlyMode: Boolean) {
        // disable thermal throttle checks, since it can cause loops to be thrown out
        // note that this bypasses allocation count
        val state = BenchmarkStateLegacy(simplifiedTimingOnlyMode = simplifiedTimingOnlyMode)
        var total = 0
        while (state.keepRunning()) {
            total++
        }

        val testResult = state.peekTestResult()

        // '50' assumes we're not running in a special mode
        // that affects repeat count (dry run)
        val expectedRepeatCount =
            50 + if (simplifiedTimingOnlyMode) 0 else BenchmarkStateLegacy.REPEAT_COUNT_ALLOCATION
        val expectedCount =
            testResult.warmupIterations!! +
                testResult.repeatIterations!! * expectedRepeatCount +
                if (Arguments.profiler == MethodTracing && !simplifiedTimingOnlyMode) 1 else 0
        assertEquals(expectedCount, total)

        if (Arguments.iterations != null) {
            assertEquals(Arguments.iterations, testResult.repeatIterations)
        }

        // verify we're not in warmup mode
        assertTrue(testResult.warmupIterations!! > 0)
        assertTrue(testResult.repeatIterations!! > 1)
    }

    @Test
    fun iterationCheck_simple() {
        iterationCheck(simplifiedTimingOnlyMode = true)
    }

    @Test
    fun iterationCheck_withAllocations() {
        // In any of these conditions, it's known that throttling won't happen, so it's safe
        // to check for allocation count, by setting checkingForThermalThrottling = false
        assumeTrue(
            CpuInfo.locked ||
                IsolationActivity.sustainedPerformanceModeInUse ||
                DeviceInfo.isEmulator
        )
        iterationCheck(simplifiedTimingOnlyMode = false)
    }

    @Test
    @Suppress("DEPRECATION")
    fun bundle() {
        val bundle =
            BenchmarkStateLegacy()
                .apply {
                    while (keepRunning()) {
                        // nothing, we're ignoring numbers
                    }
                }
                .getFullStatusReport(
                    key = "foo",
                    reportMetrics = true,
                    tracePath = Outputs.outputDirectory.absolutePath + "/bar",
                )

        val displayStringV1 = (bundle.get("android.studio.display.benchmark") as String)
        val displayStringV2 = (bundle.get("android.studio.v2display.benchmark") as String)
        assertTrue("$displayStringV1 should contain foo", displayStringV1.contains("foo"))
        assertTrue("$displayStringV2 should contain foo", displayStringV2.contains("foo"))
        assertTrue(
            "$displayStringV2 should contain [Trace](file://bar)",
            displayStringV2.contains("[Trace](file://bar)"),
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
            BenchmarkStateLegacy().peekTestResult().metrics["timeNs"]!!.median
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
            BenchmarkStateLegacy().run {
                keepRunning()
                peekTestResult().metrics["timeNs"]!!.median
            }
            fail("expected exception")
        } catch (e: IllegalStateException) {
            assertEquals(initialPriority, ThreadPriority.get())
            assertTrue(e.message!!.contains("hasn't finished"))
            assertTrue(e.message!!.contains("benchmarkRule.measureRepeated {}"))
        }
    }

    private fun validateProfilerUsage(simplifiedTimingOnlyMode: Boolean?) {
        val config = MicrobenchmarkConfig(profiler = ProfilerConfig.StackSamplingLegacy())

        val benchmarkStateLegacy =
            if (simplifiedTimingOnlyMode != null) {
                BenchmarkStateLegacy(
                    config = config,
                    simplifiedTimingOnlyMode = simplifiedTimingOnlyMode,
                )
            } else {
                BenchmarkStateLegacy(config)
            }

        // count iters with profiler enabled vs disabled
        var profilerDisabledIterations = 0
        var profilerEnabledIterations = 0
        var profilerAllocationIterations = 0
        while (benchmarkStateLegacy.keepRunning()) {
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
            // first, profiler disabled (timing) ...
            assertNotEquals(0, profilerDisabledIterations)
            // then enabled (profiling) ...
            assertNotEquals(0, profilerEnabledIterations)
            // then disabled again (allocs)
            assertNotEquals(0, profilerAllocationIterations)
        }
    }

    @Test fun profiler_default() = validateProfilerUsage(null)

    @Test fun profiler_false() = validateProfilerUsage(false)

    @Test fun profiler_true() = validateProfilerUsage(true)

    @OptIn(ExperimentalBenchmarkStateApi::class)
    @Test
    fun experimentalConstructor() {
        // min values that don't fail
        BenchmarkStateLegacy(warmupCount = null, measurementCount = 1)

        // test failures
        assertFailsWith<IllegalArgumentException> { BenchmarkStateLegacy(warmupCount = 0) }
        assertFailsWith<IllegalArgumentException> { BenchmarkStateLegacy(measurementCount = 0) }
    }
}
