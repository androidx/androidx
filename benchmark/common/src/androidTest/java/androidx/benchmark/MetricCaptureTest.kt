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
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
public class AllocationCountCaptureTest {
    @Test
    public fun simple() {
        AllocationCountCapture().verifyMedian(100..110) {
            allocate(100)
        }
    }

    @Test
    public fun pauseResume() {
        AllocationCountCapture().verifyMedian(100..110) {
            allocate(100)

            capturePaused()
            // these 1000 allocations shouldn't be counted, capture is paused!
            allocate(1000)
            captureResumed()
        }
    }
}

/**
 * Measure many times, and verify the median.
 *
 * This is done to reduce variance, e.g. from random background allocations
 */
private fun MetricCapture.verifyMedian(expected: IntRange, block: MetricCapture.() -> Unit) {
    val results = List(200) {
        captureStart()
        block()
        captureStop()
    }.toLongArray()
    val median = Stats(results, name).median
    if (median !in expected) {
        throw AssertionError(
            "observed median $median, expected $expected, saw: " + results.joinToString()
        )
    }
}
