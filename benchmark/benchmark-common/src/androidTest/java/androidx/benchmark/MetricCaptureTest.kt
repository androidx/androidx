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
import kotlin.test.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class MetricCaptureTest {
    @Test
    fun allocationCountCapture_simple() {
        AllocationCountCapture().verifyMedian(100..110) { allocate(100) }
    }

    @Test
    fun allocationCountCapture_pauseResume() {
        AllocationCountCapture().verifyMedian(100..110) {
            allocate(100)

            capturePaused()
            // these 1000 allocations shouldn't be counted, capture is paused!
            allocate(1000)
            captureResumed()
        }
    }

    @Test
    fun cpuEventCounterCapture_multi() {
        try {

            // skip test if need root, or event fails to enable
            CpuEventCounter.forceEnable()?.let { errorMessage -> assumeTrue(errorMessage, false) }

            CpuEventCounter().use { counter ->
                val firstEvents = listOf(CpuEventCounter.Event.Instructions)
                val secondEvents =
                    listOf(CpuEventCounter.Event.Instructions, CpuEventCounter.Event.CpuCycles)

                val firstCapture = CpuEventCounterCapture(counter, firstEvents)
                val secondCapture = CpuEventCounterCapture(counter, secondEvents)

                val checkCapture: (CpuEventCounterCapture, List<CpuEventCounter.Event>) -> Unit =
                    { capture, events ->
                        capture.captureStart(0)
                        assertEquals(events.getFlags(), counter.currentEventFlags)
                        capture.captureStop(1, LongArray(events.size), 0)
                    }

                checkCapture(firstCapture, firstEvents)
                checkCapture(secondCapture, secondEvents)
                checkCapture(firstCapture, firstEvents)
            }
        } finally {
            CpuEventCounter.reset()
        }
    }
}

/**
 * Measure many times, and verify the median.
 *
 * This is done to reduce variance, e.g. from random background allocations
 */
private fun MetricCapture.verifyMedian(expected: IntRange, block: MetricCapture.() -> Unit) {
    assertEquals(1, names.size)
    val longArray = longArrayOf(0L)
    val results =
        List(200) {
            captureStart(System.nanoTime())
            block()
            captureStop(System.nanoTime(), longArray, 0)
            longArray[0] * 1.0
        }
    val median = MetricResult(names[0], results).median.toInt()
    if (median !in expected) {
        throw AssertionError(
            "observed median $median, expected $expected, saw: " + results.joinToString()
        )
    }
}
