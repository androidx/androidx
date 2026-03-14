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

package androidx.tracing.wire

import androidx.tracing.AbstractTraceDriver
import androidx.tracing.AbstractTraceSink
import androidx.tracing.DEFAULT_LONG
import androidx.tracing.ExperimentalContextPropagation
import androidx.tracing.PooledTracePacketArray
import androidx.tracing.TRACE_PACKET_BUFFER_SIZE
import androidx.tracing.TRACE_PACKET_POOL_ARRAY_POOL_SIZE
import androidx.tracing.Tracer
import androidx.tracing.wire.protos.MutableCallstack
import androidx.tracing.wire.protos.MutableTracePacket
import androidx.tracing.wire.protos.MutableTrackDescriptor
import androidx.tracing.wire.protos.MutableTrackEvent
import kotlin.concurrent.thread
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.blackholeSink
import okio.buffer
import org.junit.Before

class TestSink : AbstractTraceSink() {
    internal val packets = mutableListOf<MutableTracePacket>()

    override fun enqueue(pooledPacketArray: PooledTracePacketArray) {
        pooledPacketArray.forEach {
            packets.add(
                MutableTracePacket(
                        timestamp = DEFAULT_LONG,
                        trusted_packet_sequence_id = 1, // arbitrary value
                    )
                    .apply {
                        track_event = MutableTrackEvent(track_uuid = DEFAULT_LONG)
                        // slightly abuse this function by passing in freshly allocated objects each
                        // time so this test can keep ref to all packets created, and doesn't need
                        // to bother with proto serialization,
                        WireTraceEventSerializer.updateScratchPacketFromTraceEvent(
                            event = it,
                            reportDroppedTraceEvent = false,
                            scratchTracePacket = this,
                            // this is mostly dropped and not used, but we don't care about extra
                            // allocations during this test
                            scratchTrackDescriptor = MutableTrackDescriptor(),
                            // this is sometimes not used, but we don't care about extra
                            // allocations during this test
                            scratchTrackEvent = MutableTrackEvent(track_uuid = DEFAULT_LONG),
                            // We don't reset in tests & allocations are okay here.
                            scratchAnnotations = mutableListOf(),
                            scratchAnnotationIndex = IntArray(size = 1) { _ -> -1 },
                            scratchCallStack = MutableCallstack(),
                            scratchFrames = mutableListOf(),
                            scratchFrameIndex = IntArray(size = 1) { _ -> -1 },
                        )
                    }
            )
        }
    }

    override fun onDroppedTraceEvent() {
        // Does nothing
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
    lateinit var driver: AbstractTraceDriver
    lateinit var tracer: Tracer

    @Before
    internal fun setUp() {
        sink.packets.clear()
        driver = TraceDriver(sink = sink, isEnabled = true)
        tracer = driver.tracer
    }

    @Test
    internal fun testTrackEvents() {
        driver.use { tracer.trace(category = "category", name = "section") {} }
        // 2 packets for track descriptors (process + thread)
        // 2 packets for begin and end section.
        assertEquals(4, sink.packets.size)
        assertNotNull(sink.packets.find { it.track_descriptor?.process?.process_name != null })
        assertNotNull(sink.packets.find { it.track_descriptor?.thread?.thread_name != null })
        sink.firstStartStopWithName("section") { start, _ ->
            // There should be only one category
            assertEquals(1, start.track_event!!.categories.size)
        }
    }

    @Test
    internal fun testTrackEventsMultipleCategories() {
        driver.use {
            tracer.trace(
                category = "category",
                name = "section",
                metadataBlock = { addCategory("category 1") },
            ) {}
        }
        sink.firstStartStopWithName("section") { start, _ ->
            // There should be only one category
            assertTrue { start.track_event!!.categories.size == 2 }
            assertContentEquals(
                expected = listOf("category", "category 1"),
                actual = start.track_event!!.categories,
            )
        }
    }

    @Test
    internal fun testTrackEventsWithCorrelationIds() {
        val correlationId = 10L
        val correlationIdString = "correlationId"
        driver.use {
            tracer.trace(
                category = "category",
                name = "section",
                metadataBlock = { addCorrelationId(correlationId) },
            ) {}
            tracer.trace(
                category = "category",
                name = "section2",
                metadataBlock = { addCorrelationId(correlationIdString) },
            ) {}
        }
        // 2 packets for track descriptors (process + thread)
        // 2 * 2 packets for begin and end section.
        assertEquals(6, sink.packets.size)
        assertNotNull(sink.packets.find { it.track_descriptor?.process?.process_name != null })
        assertNotNull(sink.packets.find { it.track_descriptor?.thread?.thread_name != null })
        sink.firstStartStopWithName("section") { start, _ ->
            assertEquals(correlationId, start.track_event!!.correlation_id)
            // There should be only one category
            assertEquals(1, start.track_event!!.categories.size)
        }
        sink.firstStartStopWithName("section2") { start, _ ->
            assertEquals(correlationIdString, start.track_event!!.correlation_id_str)
            // There should be only one category
            assertEquals(1, start.track_event!!.categories.size)
        }
    }

    @Test
    internal fun testTrackEventsWithMultipleSlices() {
        driver.use {
            tracer.trace(category = "category", name = "section") {}
            tracer.trace(category = "category", name = "section2") {}
        }
        // 2 packets for track descriptors (process + thread)
        // 4 packets for begin and end section.
        assertEquals(6, sink.packets.size)
        assertNotNull(sink.packets.find { it.track_descriptor?.process?.process_name != null })
        listOf("section", "section2").forEach { name ->
            sink.firstStartStopWithName(name) { start, _ ->
                assertTrue { start.track_event!!.categories.contains("category") }
                assertEquals(1, start.track_event!!.categories.size)
            }
        }
    }

    @Test
    internal fun testTrackEventsWithPropagation() = runTest {
        driver.use {
            tracer.traceCoroutine(category = "category", name = "service") {
                coroutineScope {
                    async {
                            tracer.traceCoroutine(category = "category", name = "method1") {
                                delay(timeMillis = 10)
                            }
                        }
                        .await()
                    async {
                            tracer.traceCoroutine(category = "category", name = "method2") {
                                delay(timeMillis = 40)
                            }
                        }
                        .await()
                }
            }
        }
        assertTrue(message = "Missing Packets in Trace Sink") { sink.packets.isNotEmpty() }
        val (start, _) = sink.firstStartStopWithName("service")
        val flowId = start.track_event?.flow_ids?.first()
        assertNotNull(flowId) { "Packet $start does not include a flow_id" }
        val (method1, _) = sink.firstStartStopWithName("method1")
        val (method2, _) = sink.firstStartStopWithName("method2")
        val method1FlowIds = method1.track_event?.flow_ids ?: emptyList()
        val method2FlowIds = method2.track_event?.flow_ids ?: emptyList()
        // Method 1, 2 should be assigned unique flow ids.
        assertFalse { method1FlowIds.contains(flowId) }
        assertFalse { method2FlowIds.contains(flowId) }
    }

    @Test
    // The amount of time spent sleeping does not affect the outcome of the test.
    @Suppress("BanThreadSleep")
    @OptIn(ExperimentalContextPropagation::class)
    internal fun testTrackEventsWithManualContextPropagation() = runTest {
        driver.use {
            val token = tracer.tokenForManualPropagation()
            val threads = mutableListOf<Thread>()
            threads += thread {
                tracer.trace(category = "category", name = "first", token = token) {
                    Thread.sleep(100L)
                    tracer.trace(category = "category", name = "second", token = token) {
                        Thread.sleep(200L)

                        runBlocking {
                            tracer.traceCoroutine(
                                category = "category",
                                name = "third",
                                token = token,
                            ) {
                                delay(200L)
                            }
                        }
                    }
                }
            }
            threads.forEach { it.join() }
        }
        assertTrue(message = "Missing Packets in Trace Sink") { sink.packets.isNotEmpty() }
        val (start, _) = sink.firstStartStopWithName("first")
        val flowId = start.track_event?.flow_ids?.first()
        assertNotNull(flowId) { "Packet $start does not include a flow_id" }
        val (secondSlice, _) = sink.firstStartStopWithName("second")
        val (thirdSlice, _) = sink.firstStartStopWithName("third")
        val secondFlowIds = secondSlice.track_event?.flow_ids ?: emptyList()
        val thirdFlowIds = thirdSlice.track_event?.flow_ids ?: emptyList()
        // Method second and third should be assigned the same flow id as that of the first slice.
        assertTrue { secondFlowIds.contains(flowId) }
        assertTrue { thirdFlowIds.contains(flowId) }
    }

    @Test
    internal fun testTrackEventsWithExplicitPropagation() = runTest {
        driver.use {
            tracer.traceCoroutine(category = "category", name = "first") {
                val token = tracer.tokenFromCoroutineContext()
                tracer.traceCoroutine(category = "category", name = "second", token = token) {
                    delay(10)
                }
            }
        }
        assertTrue(message = "Missing Packets in Trace Sink") { sink.packets.isNotEmpty() }
        val (start, _) = sink.firstStartStopWithName("first")
        val flowId = start.track_event?.flow_ids?.first()
        assertNotNull(flowId) { "Packet $start does not include a flow_id" }
        val (secondSlice, _) = sink.firstStartStopWithName("second")
        val secondFlowIds = secondSlice.track_event?.flow_ids ?: emptyList()
        // Method second should be assigned the same flow id as that of the first slice.
        assertTrue { secondFlowIds.contains(flowId) }
    }

    @Test
    internal fun testCounterTrackEvents() {
        driver.use { tracer.counter(category = "counter", "counter").setValue(10L) }
        assertEquals(4, sink.packets.size)
        val packet =
            sink.packets.firstOrNull { packet ->
                packet.track_event?.type == MutableTrackEvent.Type.TYPE_COUNTER
            }
        assertNotNull(packet) { "Cannot find a track event of TYPE_COUNTER" }
    }

    @Test
    internal fun testInstantTrackEvents() {
        driver.use {
            tracer.instant(category = "category", name = "name") {
                addMetadataEntry("key", "value")
            }
        }
        assertEquals(3, sink.packets.size)
        val packet =
            sink.packets.firstOrNull { packet ->
                packet.track_event?.type == MutableTrackEvent.Type.TYPE_INSTANT
            }
        assertNotNull(packet) { "Cannot find a track event of TYPE_INSTANT" }
    }

    @Test
    internal fun testSuspendAndResume() = runTest {
        driver.use {
            tracer.traceCoroutine(category = "category", name = "service") {
                coroutineScope {
                    async {
                            tracer.traceCoroutine(category = "category", name = "method1") {
                                delay(10)
                            }
                        }
                        .await()
                }
            }
        }
        assertTrue(message = "Expecting packets in the sink") { sink.packets.isNotEmpty() }
        // We should have a balanced number of begin and end events.
        val starts =
            sink.packets.filter { packet ->
                packet.track_event?.type == MutableTrackEvent.Type.TYPE_SLICE_BEGIN
            }
        val ends =
            sink.packets.filter { packet ->
                packet.track_event?.type == MutableTrackEvent.Type.TYPE_SLICE_END
            }
        assertTrue(message = "Expecting the same number of SLICE_BEGIN and SLICE_END packets") {
            starts.size == ends.size
        }
    }

    @Test
    @Ignore("We no longer drop trace packets like we used to.")
    @Suppress("DEPRECATION")
    internal fun testDroppedPackets() {
        val dispatcher = StandardTestDispatcher()
        // Use a real sink to test for dropped packets.
        val sink =
            TraceSinkDelegate(
                sink =
                    TraceSink(
                        sequenceId = 1,
                        bufferedSink = blackholeSink().buffer(),
                        // Use a test dispatcher to control exactly when trace events are being
                        // drained from the queue.
                        coroutineContext = dispatcher,
                    )
            )
        val driver = TraceDriver(sink = sink, isEnabled = true)
        // Create the Tracer
        val tracer = driver.tracer
        // Warm up tracks
        tracer.trace(category = "category", name = "name") {}
        // Discount the preamble packets
        sink.packetCount = 0

        // Don't use context.use { ... } here given it will wait indefinitely because the
        // queue won't be empty unless we advance the test dispatcher.
        repeat(TRACE_PACKET_POOL_ARRAY_POOL_SIZE) {
            repeat(16) {
                tracer.trace(category = "category", name = "section") {} // 2 events per loop.
            }
        }
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(message = "Expecting dropped packets") { sink.reportDroppedTracePacket }
        // We have 2 tracks (1 process track + 1 thread track)
        // The process track holds on to an additional allocation of the PooledTracePacketArray
        // greedily, and hence we need to discount one instance of `TRACE_PACKET_BUFFER_SIZE`
        val expectedPacketCount =
            (TRACE_PACKET_POOL_ARRAY_POOL_SIZE * TRACE_PACKET_BUFFER_SIZE) -
                TRACE_PACKET_BUFFER_SIZE
        assertEquals(
            expected = expectedPacketCount,
            actual = sink.packetCountOnDroppedTracePacket,
            message = "Unexpected number of packets",
        )
    }

    @Test
    internal fun testTrackEventsWithCallStackFrames() {
        driver.use {
            tracer.trace(
                category = "category",
                name = "section",
                metadataBlock = {
                    addCallStackEntry(name = "name", sourceFile = "sourceFile", lineNumber = 1)
                },
            ) {
                // Do nothing
            }
        }

        // 2 packets for track descriptors (process + thread)
        // 2 packets for begin and end section.
        assertEquals(4, sink.packets.size)
        assertNotNull(sink.packets.find { it.track_descriptor?.process?.process_name != null })
        assertNotNull(sink.packets.find { it.track_descriptor?.thread?.thread_name != null })
        sink.firstStartStopWithName("section") { start, _ ->
            // There should be only one category
            assertEquals(1, start.track_event!!.categories.size)
            assertEquals(1, start.track_event!!.callstack!!.frames.size)
            val frame = start.track_event!!.callstack!!.frames[0]
            assertEquals("name", frame.function_name)
            assertEquals("sourceFile", frame.source_file)
            assertEquals(1, frame.line_number)
        }
    }

    internal class TraceSinkDelegate(private val sink: AbstractTraceSink) : AbstractTraceSink() {
        internal var reportDroppedTracePacket = false
        internal var packetCount: Int = 0
        internal var packetCountOnDroppedTracePacket = 0

        override fun enqueue(pooledPacketArray: PooledTracePacketArray) {
            sink.enqueue(pooledPacketArray)
            packetCount += pooledPacketArray.packets.size
        }

        override fun onDroppedTraceEvent() {
            reportDroppedTracePacket = true
            packetCountOnDroppedTracePacket = packetCount
        }

        override fun flush() {
            sink.flush()
        }

        override fun close() {
            sink.close()
        }
    }
}
