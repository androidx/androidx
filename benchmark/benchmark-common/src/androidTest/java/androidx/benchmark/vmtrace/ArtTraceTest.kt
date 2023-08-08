/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.benchmark.vmtrace

import androidx.benchmark.Outputs
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import perfetto.protos.ThreadDescriptor
import perfetto.protos.TracePacket
import perfetto.protos.TrackDescriptor
import perfetto.protos.TrackEvent

@RunWith(AndroidJUnit4::class)
@SmallTest
class ArtTraceTest {

    @Test
    fun artToPerfettoTraceConversion() {

        // The art-trace-test.trace is generated using the following code:
        // fun testTrace() {
        //      MethodTracing.start("art-trace-test")
        //      myTracedMethod()
        //      MethodTracing.stop()
        //  }
        //  fun myTracedMethod() { }
        //
        // As such we want to assert 3 trace packets: the track descriptor and the 2 track events
        // for slice begin and slice end. Note that the track contains other android framework
        // methods.

        val artTraceFile = fromAssets("art-trace-test.trace")
        val tracePackets = ArtTrace(
            artTrace = artTraceFile,
            pid = 24877,
            uuidProvider = { 1L }
        ).toPerfettoTrace().packet

        tracePackets.single {
            it.track_descriptor != null && it.track_descriptor?.name == "main (Method Trace)"
        }.apply {
            assertEquals(
                expected = TracePacket(
                    timestamp = 430421772813000L,
                    timestamp_clock_id = 3,
                    track_descriptor = TrackDescriptor(
                        uuid = 1L,
                        name = "main (Method Trace)",
                        thread = ThreadDescriptor(pid = 24877, tid = 24877),
                        disallow_merging_with_system_tracks = true,
                    )
                ),
                actual = this
            )
        }

        val targetIid = tracePackets.first {
            it.interned_data != null
        }.interned_data!!.event_names.first {
            it.name == "androidx.benchmark.vmtrace.ArtTraceTest.myTracedMethod: ()V"
        }.iid!!
        val beginPacket = tracePackets.single {
            it.track_event?.name_iid == targetIid
        }
        assertEquals(
            expected = TracePacket(
                timestamp = 430421819817000L,
                track_event = TrackEvent(
                    name_iid = targetIid,
                    type = TrackEvent.Type.TYPE_SLICE_BEGIN,
                    track_uuid = 1L
                ),
                trusted_packet_sequence_id = 1234565432,
                sequence_flags = 0x2
            ),
            actual = beginPacket
        )

        val endPacket = tracePackets.first {
            it.timestamp == 430421819819000
        }
        assertEquals(
            expected = TracePacket(
                timestamp = 430421819819000L,
                track_event = TrackEvent(
                    type = TrackEvent.Type.TYPE_SLICE_END,
                    track_uuid = 1L
                ),
                trusted_packet_sequence_id = 1234565432,
                sequence_flags = 0x2
            ),
            actual = endPacket
        )

        // ensure balanced begin/ends
        assertEquals(
            tracePackets.count { it.track_event?.type == TrackEvent.Type.TYPE_SLICE_BEGIN },
            tracePackets.count { it.track_event?.type == TrackEvent.Type.TYPE_SLICE_END }
        )
    }

    companion object {
        private fun fromAssets(@Suppress("SameParameterValue") filename: String) = File
            .createTempFile(filename, "", Outputs.dirUsableByAppAndShell)
            .apply {
                InstrumentationRegistry
                    .getInstrumentation()
                    .context
                    .assets
                    .open(filename)
                    .copyTo(outputStream())
            }
    }
}
