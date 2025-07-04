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

package androidx.tracing.driver.wire

import androidx.tracing.driver.INVALID_LONG
import androidx.tracing.driver.PooledTracePacketArray
import androidx.tracing.driver.TraceContext
import androidx.tracing.driver.TraceSink
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import perfetto.protos.MutableTracePacket
import perfetto.protos.MutableTrackDescriptor
import perfetto.protos.MutableTrackEvent

class TestSink : TraceSink() {
    internal val packets = mutableListOf<MutableTracePacket>()

    override fun enqueue(pooledPacketArray: PooledTracePacketArray) {
        pooledPacketArray.forEach { it ->
            packets.add(
                MutableTracePacket(
                        timestamp = INVALID_LONG,
                        trusted_packet_sequence_id = 1, // arbitrary value
                    )
                    .apply {
                        track_event = MutableTrackEvent(track_uuid = INVALID_LONG)
                        // slightly abuse this function by passing in freshly allocated objects each
                        // time so this test can keep ref to all packets created, and doesn't need
                        // to bother with proto serialization,
                        WireTraceEventSerializer.updateScratchPacketFromTraceEvent(
                            event = it,
                            scratchTracePacket = this,
                            // this is mostly dropped and not used, but we don't care about extra
                            // allocations during this test
                            scratchTrackDescriptor = MutableTrackDescriptor(),
                            // this is sometimes not used, but we don't care about extra
                            // allocations during this test
                            scratchTrackEvent = MutableTrackEvent(track_uuid = INVALID_LONG),
                        )
                    }
            )
        }
    }

    override fun flush() {
        // Does nothing
    }

    override fun close() {
        // Does nothing
    }
}

class TracingTest {
    private val sink = TestSink()
    private val context: TraceContext = TraceContext(sink = sink, isEnabled = true)

    @Test
    internal fun testProcessTrackEvents() {
        context.use {
            val process = context.getOrCreateProcessTrack(id = 1, name = "process")
            val thread = process.getOrCreateThreadTrack(1, "thread")
            thread.trace("section") {}
        }
        assertTrue(sink.packets.size == 4)
        assertNotNull(sink.packets.find { it.track_descriptor?.process?.process_name == "process" })
        assertNotNull(sink.packets.find { it.track_descriptor?.thread?.thread_name == "thread" })
        sink.packets.assertTraceSection("section")
    }

    @Test
    internal fun testCounterTrackEvents() {
        context.use {
            val process = context.getOrCreateProcessTrack(id = 1, name = "process")
            val counter = process.getOrCreateCounterTrack("counter")
            counter.setCounter(10L)
        }
        assertTrue(sink.packets.size == 3)
    }

    @Test
    internal fun testAsyncEventsInProcess() {
        context.use {
            val process = context.getOrCreateProcessTrack(id = 1, name = "process")
            process.trace("section") {}
            process.trace("section2") {}
        }
        assertTrue(sink.packets.size == 5)
        assertNotNull(sink.packets.find { it.track_descriptor?.process?.process_name == "process" })
        sink.packets.assertTraceSection("section")
        sink.packets.assertTraceSection("section2")
    }

    @Test
    internal fun testAsyncEventsWithFlows() = runTest {
        context.use {
            with(context) {
                val process = getOrCreateProcessTrack(id = 1, name = "process")
                with(process) {
                    traceFlow("service") {
                        coroutineScope {
                            async { traceFlow(name = "method1") { delay(10) } }.await()
                            async { traceFlow(name = "method2") { delay(40) } }.await()
                        }
                    }
                }
            }
        }
        assertTrue { sink.packets.isNotEmpty() }
        val serviceBegin = sink.packets.trackEventPacket(name = "service")
        val method1Begin = sink.packets.trackEventPacket(name = "method1")
        val method2Begin = sink.packets.trackEventPacket(name = "method2")
        assertNotNull(serviceBegin) { "Cannot find packet with name service" }
        val flowId = serviceBegin.track_event?.flow_ids?.first()
        assertNotNull(flowId) { "Packet $serviceBegin does not include a flow_id" }
        assertNotNull(method1Begin) { "Cannot find packet with name method1" }
        assertNotNull(method2Begin) { "Cannot find packet with name method2" }
        assertContains(method1Begin.track_event?.flow_ids ?: emptyList(), flowId)
        assertContains(method2Begin.track_event?.flow_ids ?: emptyList(), flowId)
    }
}
