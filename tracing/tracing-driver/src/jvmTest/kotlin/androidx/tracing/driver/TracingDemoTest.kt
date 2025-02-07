/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.tracing.driver

import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class TracingDemoTest {

    internal val random = Random(42)

    internal val forkSize = 20
    internal val multiplier = 1000
    internal val inputSize = forkSize * multiplier

    // Tracks the number of batches completed
    internal var count = 0L
    internal val driver: TraceDriver =
        TraceDriver(sequenceId = 1, sink = JvmTraceSink(File("/tmp")), isEnabled = true)

    @Test
    internal fun testTracingEndToEnd() = runBlocking {
        driver.context.use {
            withContext(context = Dispatchers.Default) {
                // Create a process track
                val track = driver.ProcessTrack(id = 1, name = "TracingTest")
                track.traceFlow("begin") { delay(20L) }
                track.traceFlow("histograms-end-to-end") { track.computeHistograms() }
                track.traceFlow("end") { delay(20L) }
            }
        }
    }

    internal suspend fun ProcessTrack.computeHistograms(): Map<Int, Int> {
        val input = List<Int>(inputSize) { random.nextInt(0, 100_000) }
        val batches = input.chunked(multiplier)
        return coroutineScope {
            val jobs = mutableListOf<Deferred<Map<Int, Int>>>()
            batches.forEachIndexed { index, batch ->
                jobs += async { traceFlow("histograms-batch-$index") { computeHistogram(batch) } }
            }
            val histograms = jobs.awaitAll()
            val output = traceFlow("merge-histograms") { mergeHistograms(input, histograms) }
            output
        }
    }

    internal suspend fun ProcessTrack.computeHistogram(list: List<Int>): Map<Int, Int> {
        val counter = CounterTrack("Batches Completed")
        val frequency = mutableMapOf<Int, Int>()
        for (element in list) {
            val count = frequency[element] ?: 0
            frequency[element] = count + 1
        }
        delay(random.nextLong(10L, 20L)) // Waterfall
        count += 1
        counter.emitLongCounterPacket(count)
        return frequency
    }

    internal suspend fun ProcessTrack.mergeHistograms(
        input: List<Int>,
        histograms: List<Map<Int, Int>>,
    ): Map<Int, Int> {
        val frequency = mutableMapOf<Int, Int>()
        for (element in input) {
            var count = 0
            for (histogram in histograms) {
                count += histogram[element] ?: 0
            }
            frequency[element] = count
        }
        delay(random.nextLong(10L, 20L)) // Waterfall
        return frequency
    }
}
