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

import androidx.benchmark.MicrobenchmarkPhase.LoopMode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SmallTest
import java.util.concurrent.TimeUnit
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class MicrobenchmarkPhaseTest {
    @LargeTest
    @Test
    fun execute_thermalThrottleSleepLongerThanHardDeadline_doesNotThrow() = runBlocking {
        try {
            // Keep the deadlines well above the warmup thread-priority setup and synchronous
            // Runtime.getRuntime().gc() duration (~64ms on Pixel 6, and variable across devices),
            // while still running quickly.
            Microbenchmark.softDeadlineDurationMsOverride = 200
            Microbenchmark.hardDeadlineDurationMsOverride = 300
            // Sleep for 400ms, which is longer than both the 200ms soft deadline and the 300ms hard
            // deadline, so the benchmark would fail after waking up if the sleep did not reset the
            // deadlines.
            Arguments.thermalThrottleSleepDurationMsOverride = 400
            assertTrue(
                Microbenchmark.softDeadlineDurationMs < Microbenchmark.hardDeadlineDurationMs
            )
            assertTrue(
                Arguments.thermalThrottleSleepDurationMs > Microbenchmark.hardDeadlineDurationMs
            )
            ThrottleDetector.insertSingleFakeThrottleDetection = true

            createThermalThrottleDeadlineTestMicrobenchmark().executePhases()

            // ThrottleDetector.insertSingleFakeThrottleDetection is reset when the fake throttle is
            // executed.
            assertFalse(ThrottleDetector.insertSingleFakeThrottleDetection)
        } finally {
            Microbenchmark.softDeadlineDurationMsOverride = null
            Microbenchmark.hardDeadlineDurationMsOverride = null
            Arguments.thermalThrottleSleepDurationMsOverride = null
            ThrottleDetector.insertSingleFakeThrottleDetection = false
        }
    }

    @LargeTest
    @Test
    fun execute_thermalThrottleSleep_throwsWhenPostSleepRepeatOverrunsHardDeadline() = runBlocking {
        try {
            // Keep the deadlines well above the warmup thread-priority setup and synchronous
            // Runtime.getRuntime().gc() duration (~64ms on Pixel 6, and variable across devices),
            // while still running quickly.
            Microbenchmark.softDeadlineDurationMsOverride = 200
            Microbenchmark.hardDeadlineDurationMsOverride = 300
            // Sleep for 400ms, which is longer than both the 200ms soft deadline and the 300ms hard
            // deadline.
            Arguments.thermalThrottleSleepDurationMsOverride = 400
            assertTrue(
                Microbenchmark.softDeadlineDurationMs < Microbenchmark.hardDeadlineDurationMs
            )
            assertTrue(
                Arguments.thermalThrottleSleepDurationMs > Microbenchmark.hardDeadlineDurationMs
            )
            ThrottleDetector.insertSingleFakeThrottleDetection = true

            val exception =
                assertFailsWith<IllegalStateException> {
                    createThermalThrottleDeadlineTestMicrobenchmark { _, _ ->
                        if (ThrottleDetector.insertSingleFakeThrottleDetection) {
                            return@createThermalThrottleDeadlineTestMicrobenchmark
                        }
                        // Spin for 350ms, which is longer than both the 200ms soft deadline
                        // and the 300ms hard deadline, so this repeat triggers the hard
                        // time limit error.
                        val spinEndNs = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(350)
                        while (System.nanoTime() < spinEndNs) {}
                    }
                        .executePhases()
                }
            assertTrue(exception.message!!.contains("Benchmark loop overran hard time limit"))
        } finally {
            Microbenchmark.softDeadlineDurationMsOverride = null
            Microbenchmark.hardDeadlineDurationMsOverride = null
            Arguments.thermalThrottleSleepDurationMsOverride = null
            ThrottleDetector.insertSingleFakeThrottleDetection = false
        }
    }

    @Test
    fun loopMode_getIterations() {
        val durationLoopMode = LoopMode.Duration(1_000_000)

        // validate division
        assertEquals(1, durationLoopMode.getIterations(1_000_000))
        assertEquals(1_000, durationLoopMode.getIterations(1_000))
        assertEquals(1_000_000, durationLoopMode.getIterations(1))

        // low boundary condition
        assertEquals(LoopMode.MIN_TEST_ITERATIONS, durationLoopMode.getIterations(1_000_000_000))

        // high boundary condition
        // Note that 0 is allowed due to clock imprecision
        // E.g. on Mako API 17, have observed 30us granularity in System.nanoTime()
        assertEquals(LoopMode.MAX_TEST_ITERATIONS, durationLoopMode.getIterations(0))

        // fail - warmup hasn't occurred
        assertFailsWith<IllegalStateException> { durationLoopMode.getIterations(-1) }
    }

    private fun createThermalThrottleDeadlineTestMicrobenchmark(
        loopedMeasurementBlock: LoopedMeasurementBlock = { _, _ -> }
    ) =
        Microbenchmark(
            definition = TestDefinition("com.example.TestClass", "TestClass", "testMethod"),
            phaseConfig =
                MicrobenchmarkPhase.Config(
                    dryRunMode = false,
                    startupMode = false,
                    simplifiedTimingOnlyMode = false,
                    // Skip method tracing so trace file I/O does not exceed the 300ms test
                    // deadline.
                    profiler = null,
                    profilerPerfCompareMode = false,
                    warmupCount = 1,
                    measurementCount = 1,
                    metrics = arrayOf(TimeCapture()),
                ),
            yieldThreadPeriodically = true,
            scopeFactory = { MicrobenchmarkScope(it) },
            loopedMeasurementBlock = loopedMeasurementBlock,
        )
}
