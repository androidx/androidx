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

import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class WarmupManagerTest {

    @Test
    fun minDuration() {
        // even with tiny, predictable inputs, we require min warmup duration
        val warmup = WarmupManager()
        while (!warmup.onNextIteration(100)) {}
        assertEquals((WarmupManager.MIN_DURATION_NS / 100).toInt(), warmup.iteration)
    }

    @Test
    fun maxDuration() {
        // even if values are warming up slowly, we require max warmup duration
        val warmup = WarmupManager()
        warmup.warmupOnFakeData(
                warmupNeededNs = TimeUnit.SECONDS.toNanos(20),
                idealDurationNs = TimeUnit.MILLISECONDS.toNanos(5))

        assertApproxEquals(
                WarmupManager.MAX_DURATION_NS,
                warmup.totalDuration,
                TimeUnit.MILLISECONDS.toNanos(100))
    }

    @Test
    fun minIterations() {
        // min iterations overrides max duration
        val warmup = WarmupManager()
        while (!warmup.onNextIteration(TimeUnit.SECONDS.toNanos(2))) {}
        assertEquals(WarmupManager.MIN_ITERATIONS, warmup.iteration)
    }

    @Test
    fun similarIterationCount() {
        // mock warmup data, and validate we detect convergence
        val warmup = WarmupManager()
        val warmupNeededNs = TimeUnit.SECONDS.toNanos(2)
        val idealDurationNs = TimeUnit.MILLISECONDS.toNanos(5)
        warmup.warmupOnFakeData(warmupNeededNs, idealDurationNs)

        // These asserts aren't very tight - the moving average ratios
        // significantly change how fast we detect convergence
        assertTrue(warmup.totalDuration > TimeUnit.SECONDS.toNanos(2))
        assertTrue(warmup.totalDuration < TimeUnit.SECONDS.toNanos(6))
    }
}

private fun assertApproxEquals(expected: Long, observed: Long, threshold: Long) {
    assertTrue("Expected: $expected, Observed: $observed, Thresh: $threshold",
            Math.abs(expected - observed) < threshold)
}

private fun WarmupManager.warmupOnFakeData(warmupNeededNs: Long, idealDurationNs: Long) {
    val results = generateFakeResults(warmupNeededNs, idealDurationNs)
    for (duration in results) {
        if (onNextIteration(duration)) break
    }
}

private fun generateFakeResults(warmupNeededNs: Long, idealDurationNs: Long): Sequence<Long> {
    val list = ArrayList<Long>()

    var totalDuration = 0L
    var currentDuration = idealDurationNs.toFloat()
    while (totalDuration < warmupNeededNs) {
        val iterDuration = currentDuration.toLong()
        list.add(0, iterDuration)
        totalDuration += iterDuration
        currentDuration *= 1.003f
    }

    // warmup until warmupNeededNs, then just return idealDurationNs
    return generateSequence(1) { it + 1 }
            .map { if (it < list.size) list[it] else idealDurationNs }
}
