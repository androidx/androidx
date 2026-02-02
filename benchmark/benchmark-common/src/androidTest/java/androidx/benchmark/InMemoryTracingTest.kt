/*
 * Copyright 2021 The Android Open Source Project
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

import android.os.Process
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import perfetto.protos.ThreadDescriptor
import perfetto.protos.TracePacket
import perfetto.protos.TrackDescriptor
import perfetto.protos.TrackEvent

@RunWith(AndroidJUnit4::class)
@SmallTest
class InMemoryTracingTest {
    @Before
    @After
    fun setup() {
        InMemoryTracing.clearEvents() // reset
    }

    @Test
    fun emptyTrace() {
        val trace = InMemoryTracing.commitToTrace("testLabel") // capture trace

        assertEquals(1, trace.packet.size)
        val packet = trace.packet.first()
        assertEquals(
            packet,
            TracePacket(
                timestamp = packet.timestamp,
                timestamp_clock_id = 3,
                incremental_state_cleared = true,
                track_descriptor =
                    TrackDescriptor(
                        uuid = packet.track_descriptor?.uuid,
                        name = "testLabel",
                        thread = ThreadDescriptor(pid = Process.myPid(), tid = Process.myTid()),
                        disallow_merging_with_system_tracks = true
                    )
            )
        )
    }

    @Test
    fun minimalTrace() {
        val beforeTime = System.nanoTime()
        inMemoryTrace("test trace section") {}
        val afterTime = System.nanoTime()

        val trace = InMemoryTracing.commitToTrace("testLabel")

        assertEquals(3, trace.packet.size)

        // verify track
        val descriptor = trace.packet.first().track_descriptor
        assertNotNull(descriptor)
        assertEquals("testLabel", descriptor.name)

        // verify events
        trace.packet[1].apply {
            assertTrue(timestamp in beforeTime..afterTime)
            assertEquals(
                TracePacket(
                    timestamp = timestamp,
                    timestamp_clock_id = 3,
                    trusted_packet_sequence_id = trusted_packet_sequence_id,
                    track_event =
                        TrackEvent(
                            type = TrackEvent.Type.TYPE_SLICE_BEGIN,
                            track_uuid = descriptor.uuid,
                            categories = listOf("benchmark"),
                            name = "test trace section"
                        )
                ),
                this
            )
        }
        trace.packet[2].apply {
            assertTrue(timestamp in beforeTime..afterTime)
            assertEquals(
                TracePacket(
                    timestamp = timestamp,
                    timestamp_clock_id = 3,
                    trusted_packet_sequence_id = trusted_packet_sequence_id,
                    track_event =
                        TrackEvent(
                            type = TrackEvent.Type.TYPE_SLICE_END,
                            track_uuid = descriptor.uuid,
                        )
                ),
                this
            )
        }
    }

    @Test
    fun traceWithCounters() {
        val beforeTime = 100L
        val afterTime = 200L

        // test counter embedded in beginSection
        InMemoryTracing.beginSection(
            "test trace section",
            beforeTime,
            counterNames = listOf("counterLabel"),
            counterValues = listOf(0.1)
        )
        InMemoryTracing.endSection(afterTime)

        // test counter on its own
        InMemoryTracing.counter("counterLabel", 1.0, afterTime)

        val trace = InMemoryTracing.commitToTrace("testLabel")

        assertEquals(5, trace.packet.size)

        // verify first track, for slices
        val sliceDescriptor = trace.packet.first().track_descriptor
        assertNotNull(sliceDescriptor)
        assertEquals("testLabel", sliceDescriptor.name)
        // verify second track, for counters
        val counterDescriptor = trace.packet[1].track_descriptor
        assertNotNull(counterDescriptor)
        assertEquals("counterLabel", counterDescriptor.name)

        // verify events
        trace.packet[2].apply {
            assertEquals(timestamp, beforeTime)
            assertEquals(
                TracePacket(
                    timestamp = timestamp,
                    timestamp_clock_id = 3,
                    trusted_packet_sequence_id = trusted_packet_sequence_id,
                    track_event =
                        TrackEvent(
                            type = TrackEvent.Type.TYPE_SLICE_BEGIN,
                            track_uuid = sliceDescriptor.uuid,
                            categories = listOf("benchmark"),
                            name = "test trace section",
                            extra_double_counter_track_uuids = listOf(counterDescriptor.uuid!!),
                            extra_double_counter_values = listOf(0.1)
                        )
                ),
                this
            )
        }
        trace.packet[3].apply {
            assertEquals(timestamp, afterTime)
            assertEquals(
                TracePacket(
                    timestamp = timestamp,
                    timestamp_clock_id = 3,
                    trusted_packet_sequence_id = trusted_packet_sequence_id,
                    track_event =
                        TrackEvent(
                            type = TrackEvent.Type.TYPE_SLICE_END,
                            track_uuid = sliceDescriptor.uuid,
                        )
                ),
                this
            )
        }
        trace.packet[4].apply {
            assertEquals(timestamp, afterTime)
            assertEquals(
                TracePacket(
                    timestamp = timestamp,
                    timestamp_clock_id = 3,
                    trusted_packet_sequence_id = trusted_packet_sequence_id,
                    track_event =
                        TrackEvent(
                            type = TrackEvent.Type.TYPE_COUNTER,
                            track_uuid = counterDescriptor.uuid,
                            double_counter_value = 1.0,
                        )
                ),
                this
            )
        }
    }
}

@Suppress("SameParameterValue")
internal fun createTempFileFromAsset(prefix: String, suffix: String): File {
    val file = File.createTempFile(prefix, suffix, Outputs.dirUsableByAppAndShell)
    InstrumentationRegistry.getInstrumentation()
        .context
        .assets
        .open(prefix + suffix)
        .copyTo(file.outputStream())
    return file
}
