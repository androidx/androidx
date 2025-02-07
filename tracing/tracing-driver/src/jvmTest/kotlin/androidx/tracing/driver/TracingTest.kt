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

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest

class TestSink : TraceSink() {
    internal val packets = mutableListOf<PooledTracePacket>()

    override fun emit(pooledPacketArray: PooledTracePacketArray) {
        for (packet in pooledPacketArray.pooledTracePacketArray) {
            if (packet != null) {
                packets += packet
            }
        }
        pooledPacketArray.recycle()
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
    private val context: TraceContext = TraceContext(sequenceId = 1, sink = sink, isEnabled = true)

    @BeforeTest
    fun setUp() {
        for (packet in sink.packets) {
            packet.recycle()
        }
        sink.packets.clear()
    }

    @Test
    internal fun testProcessTrackEvents() {
        context.use {
            val process = context.ProcessTrack(id = 1, name = "process")
            val thread = process.ThreadTrack(1, "thread")
            thread.trace("section") {}
        }
        assertTrue(sink.packets.size == 4)
        assertNotNull(
            sink.packets.find {
                it.tracePacket.track_descriptor?.process?.process_name == "process"
            }
        )
        assertNotNull(
            sink.packets.find { it.tracePacket.track_descriptor?.thread?.thread_name == "thread" }
        )
        sink.packets.assertTraceSection("section")
    }

    @Test
    internal fun testCounterTrackEvents() {
        context.use {
            val process = context.ProcessTrack(id = 1, name = "process")
            val counter = process.CounterTrack("counter")
            counter.emitLongCounterPacket(10L)
        }
        assertTrue(sink.packets.size == 3)
    }

    @Test
    internal fun testAsyncEventsInProcess() {
        context.use {
            val process = context.ProcessTrack(id = 1, name = "process")
            process.trace("section") {}
            process.trace("section2") {}
        }
        assertTrue(sink.packets.size == 5)
        assertNotNull(
            sink.packets.find {
                it.tracePacket.track_descriptor?.process?.process_name == "process"
            }
        )
        sink.packets.assertTraceSection("section")
        sink.packets.assertTraceSection("section2")
    }

    @Test
    internal fun testAsyncEventsWithFlows() = runTest {
        context.use {
            with(context) {
                val process = ProcessTrack(id = 1, name = "process")
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
        val flowId = serviceBegin.tracePacket.track_event?.flow_ids?.first()
        assertNotNull(flowId) { "Packet $serviceBegin does not include a flow_id" }
        assertNotNull(method1Begin) { "Cannot find packet with name method1" }
        assertNotNull(method2Begin) { "Cannot find packet with name method2" }
        assertContains(method1Begin.tracePacket.track_event?.flow_ids ?: emptyList(), flowId)
        assertContains(method2Begin.tracePacket.track_event?.flow_ids ?: emptyList(), flowId)
    }
}
