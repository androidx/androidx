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
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(AndroidJUnit4::class)
public class WarmupManagerTest {

    @Test
    public fun minDuration() {
        // even with tiny, predictable inputs, we require min warmup duration
        val warmup = WarmupManager()
        while (!warmup.onNextIteration(100)) {}
        assertEquals((WarmupManager.MIN_DURATION_NS / 100).toInt(), warmup.iteration)
    }

    @Test
    public fun maxDuration() {
        // even if values are warming up slowly, we require max warmup duration
        val warmup = WarmupManager()
        warmup.warmupOnFakeData(
            warmupNeededNs = TimeUnit.SECONDS.toNanos(20),
            idealDurationNs = TimeUnit.MILLISECONDS.toNanos(5)
        )

        assertApproxEquals(
            WarmupManager.MAX_DURATION_NS,
            warmup.totalDurationNs,
            TimeUnit.MILLISECONDS.toNanos(100)
        )
    }

    @Test
    public fun minIterations() {
        // min iterations overrides max duration
        val warmup = WarmupManager()
        while (!warmup.onNextIteration(TimeUnit.SECONDS.toNanos(2))) {}
        assertEquals(WarmupManager.MIN_ITERATIONS, warmup.iteration)
    }

    @Test
    public fun similarIterationCount() {
        // mock warmup data, and validate we detect convergence
        val warmup = WarmupManager()
        val warmupNeededNs = TimeUnit.SECONDS.toNanos(2)
        val idealDurationNs = TimeUnit.MILLISECONDS.toNanos(5)
        warmup.warmupOnFakeData(warmupNeededNs, idealDurationNs)

        // These asserts aren't very tight - the moving average ratios
        // significantly change how fast we detect convergence
        assertTrue(warmup.totalDurationNs > TimeUnit.SECONDS.toNanos(2))
        assertTrue(warmup.totalDurationNs < TimeUnit.SECONDS.toNanos(6))
    }
}

private fun assertApproxEquals(expected: Long, observed: Long, threshold: Long) {
    assertTrue(
        "Expected: $expected, Observed: $observed, Thresh: $threshold",
        Math.abs(expected - observed) < threshold
    )
}

private fun WarmupManager.warmupOnFakeData(warmupNeededNs: Long, idealDurationNs: Long) {
    val results = generateFakeResults(warmupNeededNs, idealDurationNs)
    for (duration in results) {
        if (onNextIteration(duration)) break
    }
}

private fun generateFakeResults(warmupNeededNs: Long, idealDurationNs: Long): Sequence<Long> {
    val list = ArrayList<Long>()

    var totalDurationNs = 0L
    var currentDurationNs = idealDurationNs.toFloat()
    while (totalDurationNs < warmupNeededNs) {
        val iterDurationNs = currentDurationNs.toLong()
        list.add(0, iterDurationNs)
        totalDurationNs += iterDurationNs
        currentDurationNs *= 1.003f
    }

    // warmup until warmupNeededNs, then just return idealDurationNs
    return generateSequence(1) { it + 1 }
        .map { if (it < list.size) list[it] else idealDurationNs }
}
