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
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(JUnit4::class)
class BenchmarkStateTest {
    private fun ms2ns(ms: Long): Long = TimeUnit.MILLISECONDS.toNanos(ms)

    @get:Rule
    val writePermissionRule =
        GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)!!

    @Test
    fun simple() {
        // would be better to mock the clock, but going with minimal changes for now
        val state = BenchmarkState()
        while (state.keepRunning()) {
            Thread.sleep(3)
            state.pauseTiming()
            Thread.sleep(5)
            state.resumeTiming()
        }
        val median = state.getReport().getStats("timeNs").median
        assertTrue(
            "median $median should be between 2ms and 4ms",
            ms2ns(2) < median && median < ms2ns(4)
        )
    }

    @SdkSuppress(minSdkVersion = 21)
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

    @Test
    fun iterationCheck() {
        val state = BenchmarkState()
        // disable thermal throttle checks, since it can cause loops to be thrown out
        state.performThrottleChecks = false
        var total = 0
        while (state.keepRunning()) {
            total++
        }

        val report = state.getReport()
        val expectedCount =
            report.warmupIterations + report.repeatIterations * BenchmarkState.REPEAT_COUNT_TIME
        assertEquals(expectedCount, total)

        // verify we're not in warmup mode
        assertTrue(report.warmupIterations > 0)
        assertTrue(report.repeatIterations > 1)
        assertEquals(50, BenchmarkState.REPEAT_COUNT_TIME)
    }

    @Test
    fun ideSummary() {
        val summary1 = BenchmarkState.ideSummaryLine("foo", 1000)
        val summary2 = BenchmarkState.ideSummaryLine("fooBarLongerKey", 10000)

        assertEquals(
            summary1.indexOf("foo"),
            summary2.indexOf("foo")
        )
    }

    @Test
    fun bundle() {
        val bundle = BenchmarkState().apply {
            while (keepRunning()) {
                // nothing, we're ignoring numbers
            }
        }.getFullStatusReport(key = "foo", includeStats = true)

        assertTrue(
            (bundle.get("android.studio.display.benchmark") as String).contains("foo")
        )

        // check attribute presence and naming
        val prefix = Errors.PREFIX

        // legacy - before metric name was included
        assertNotNull(bundle.get("${prefix}min"))
        assertNotNull(bundle.get("${prefix}median"))
        assertNotNull(bundle.get("${prefix}standardDeviation"))

        // including metric name
        assertNotNull(bundle.get("${prefix}timeNs_min"))
        assertNotNull(bundle.get("${prefix}timeNs_median"))
        assertNotNull(bundle.get("${prefix}timeNs_stddev"))
    }

    @Test
    fun notStarted() {
        val initialPriority = ThreadPriority.get()
        try {
            BenchmarkState().getReport().getStats("timeNs").median
            fail("expected exception")
        } catch (e: IllegalStateException) {
            assertEquals(initialPriority, ThreadPriority.get())
            assertTrue(e.message!!.contains("wasn't started"))
            assertTrue(e.message!!.contains("benchmarkRule.measureRepeated {}"))
        }
    }

    @Test
    fun notFinished() {
        val initialPriority = ThreadPriority.get()
        try {
            BenchmarkState().run {
                keepRunning()
                getReport().getStats("timeNs").median
            }
            fail("expected exception")
        } catch (e: IllegalStateException) {
            assertEquals(initialPriority, ThreadPriority.get())
            assertTrue(e.message!!.contains("hasn't finished"))
            assertTrue(e.message!!.contains("benchmarkRule.measureRepeated {}"))
        }
    }

    @Suppress("DEPRECATION")
    @UseExperimental(ExperimentalExternalReport::class)
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
        val expectedReport = BenchmarkState.Report(
            className = "className",
            testName = "testName",
            totalRunTimeNs = 900000000,
            data = listOf(listOf(100L, 200L, 300L)),
            stats = listOf(Stats(longArrayOf(100, 200, 300), "timeNs")),
            repeatIterations = 1,
            thermalThrottleSleepSeconds = 0,
            warmupIterations = 1
        )
        assertEquals(expectedReport, ResultWriter.reports.last())
    }
}
