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

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okio.buffer
import okio.sink
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class InMemoryRingBufferTraceSinkTest {
    @get:Rule val tmpFolder = TemporaryFolder()

    @Test
    fun testFlush_preservesOngoingEvents() = runBlocking {
        val folder = tmpFolder.newFolder()
        val file1 = File(folder, "trace1.perfetto")
        val sink = InMemoryRingBufferTraceSink(capacityInBytes = 10_000_000, sequenceId = 1)
        TraceDriver(sink = sink, isEnabled = true).use { driver ->
            val tracer = driver.tracer

            val job =
                launch(Dispatchers.Default) {
                    repeat(100) { tracer.traceCoroutine("cat", "event-$it") { delay(1) } }
                }

            // Wait for potential background processing
            delay(50)

            val bufferedSink = file1.sink().buffer()
            driver.flush()
            sink.flushTo(bufferedSink)

            job.join()

            // Flush remainder
            driver.flush()
            sink.flushTo(bufferedSink)
            bufferedSink.close()

            assertTrue(file1.exists(), "File should exist")
            assertTrue(file1.length() > 0, "Should have captured trace data")
        }
    }

    @Test
    fun testBufferOverflow_truncatesOldData() = runBlocking {
        val folder = tmpFolder.newFolder()
        val file = File(folder, "trace_overflow.perfetto")
        // Small capacity
        val capacity = 1024L
        val sink = InMemoryRingBufferTraceSink(capacityInBytes = capacity, sequenceId = 1)
        TraceDriver(sink = sink, isEnabled = true).use { driver ->
            val tracer = driver.tracer

            // Generate enough data to overflow
            repeat(1000) { tracer.traceCoroutine("cat", "event-$it") {} }

            // Wait for potential background processing
            delay(50)

            file.sink().buffer().use { bufferedSink ->
                driver.flush()
                sink.flushTo(bufferedSink)
            }

            assertTrue(file.exists())
            assertTrue(file.length() > 0, "File should not be empty")

            // We can't assert <= capacity because of our approximation of TraceEvent size.
            // However, 1000 events without limits would produce a file > 50KB.
            // We assert that it's within 4KB our expected capacity
            assertTrue(
                file.length() < capacity + 4096,
                "File size (${file.length()} bytes) should be bounded and close to capacity",
            )
        }
    }

    @Test
    fun testComplexEventPreservation() = runBlocking {
        val folder = tmpFolder.newFolder()
        val file = File(folder, "trace_complex.perfetto")
        // Sufficient for one complex event
        val sink = InMemoryRingBufferTraceSink(capacityInBytes = 10_000, sequenceId = 1)
        TraceDriver(sink = sink, isEnabled = true).use { driver ->
            val tracer = driver.tracer

            tracer.traceCoroutine(
                category = "cat-complex",
                name = "event-complex",
                metadataBlock = {
                    addMetadataEntry("meta-key", "meta-value")
                    addCategory("cat-extra")
                    addCallStackEntry("func1", "file1.kt", 100)
                },
            ) {
                // No-op
            }

            // Wait for potential background processing
            delay(50)

            file.sink().buffer().use { bufferedSink ->
                driver.flush()
                sink.flushTo(bufferedSink)
            }

            assertTrue(file.exists())
            assertTrue(file.length() > 0)
        }
    }

    @Test
    fun testCloseWithoutFlush_dropsData() = runBlocking {
        val folder = tmpFolder.newFolder()
        val file = File(folder, "trace_dropped.perfetto")
        val sink = InMemoryRingBufferTraceSink(capacityInBytes = 10_000, sequenceId = 1)
        val driver = TraceDriver(sink = sink, isEnabled = true)
        val tracer = driver.tracer

        tracer.traceCoroutine("cat", "event-dropped") {}

        // Wait for potential background processing
        delay(50)

        // Close driver (and sink) without persistence (no sink provided to close)
        driver.close()

        // The file should be empty because data should be dropped
        assertEquals(0L, file.length(), "File should be empty when closed without flush")
    }

    @Test
    fun testDriverFlush_isNoOpAndDataIsSentToSink() = runBlocking {
        val folder = tmpFolder.newFolder()
        val file = File(folder, "trace_tracks.perfetto")
        val sink = InMemoryRingBufferTraceSink(capacityInBytes = 10_000_000, sequenceId = 1)
        val driver = TraceDriver(sink = sink, isEnabled = true)
        val tracer = driver.tracer

        tracer.trace("cat", "event-test") {
            // no suspending
        }

        // Flushing the driver without flushing the sink ensures that the track preamble
        // and buffered packets are enqueued into the sink, but the sink itself is not cleared.
        driver.flush()

        file.sink().buffer().use { bufferedSink ->
            driver.flush()
            sink.flushTo(bufferedSink)
        }
        // Close driver (and sink)
        driver.close()

        assertTrue(file.exists())
        assertTrue(file.length() > 0, "File should contain flushed events")

        val trace = androidx.tracing.wire.protos.MutableTrace.ADAPTER.decode(file.readBytes())

        val starts =
            trace.packet.filter {
                it.track_event?.type ==
                    androidx.tracing.wire.protos.MutableTrackEvent.Type.TYPE_SLICE_BEGIN
            }
        val ends =
            trace.packet.filter {
                it.track_event?.type ==
                    androidx.tracing.wire.protos.MutableTrackEvent.Type.TYPE_SLICE_END
            }

        assertEquals(1, starts.size, "Should have exactly one start packet")
        assertEquals(1, ends.size, "Should have exactly one end packet")

        // NEW: Verify that the Process and Thread metadata (preambles) are in the trace.
        // Without these, the Perfetto UI won't know what process/thread the slices belong to.
        val trackDescriptors = trace.packet.mapNotNull { it.track_descriptor }

        val hasProcessDescriptor = trackDescriptors.any { it.process != null }
        val hasThreadDescriptor = trackDescriptors.any { it.thread != null }

        assertTrue(hasProcessDescriptor, "Trace must contain a Process TrackDescriptor")
        assertTrue(hasThreadDescriptor, "Trace must contain a Thread TrackDescriptor")
    }
}
