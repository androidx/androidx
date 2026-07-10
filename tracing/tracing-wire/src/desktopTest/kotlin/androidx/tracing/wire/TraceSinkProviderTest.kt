/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.tracing.wire

import androidx.tracing.PooledTracePacketArray
import androidx.tracing.Queue
import androidx.tracing.Tracer
import kotlin.test.Test
import kotlin.test.assertEquals

class TraceSinkProviderTest {
    @Test
    internal fun testEvents() {
        val sink = TraceSink(sequenceId = 1, sinkProvider = { throw RuntimeException("Not ready") })
        val driver =
            TraceDriver(sink = sink, isGloballyEnabled = true, isCategoryEnabled = { true })
        val tracer: Tracer = driver.tracer
        driver.use { tracer.trace(category = "category", name = "section") {} }
        // We should still be able to close() the driver despite the provider not being ready.
        // We should have 6 packets.
        // 2 packets for track descriptors (process + thread)
        // 2 packets for begin and end section.
        // 2 packets for flush().
        assertEquals(6, sink.queue.tracePacketCount())
    }

    private fun Queue<PooledTracePacketArray>.tracePacketCount(): Int {
        var count = 0
        for (i in 0 until size) {
            val pooledTracePacketArray = this[i]
            count += pooledTracePacketArray.fillCount
        }
        return count
    }
}
