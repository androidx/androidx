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
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
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
            uuidProvider = { 1L }
        ).toPerfettoTrace().packet
        val toFind = mutableListOf(
            TracePacket(
                timestamp = 430421772813000L,
                timestamp_clock_id = 3,
                incremental_state_cleared = true,
                track_descriptor = TrackDescriptor(
                    name = "main",
                    uuid = 1L,
                )
            ),
            TracePacket(
                timestamp = 430421819817000L,
                timestamp_clock_id = 3,
                track_event = TrackEvent(
                    categories = listOf("art_trace"),
                    name = "androidx.benchmark.vmtrace.ArtTraceTest.myTracedMethod: ()V",
                    type = TrackEvent.Type.TYPE_SLICE_BEGIN,
                    track_uuid = 1L
                ),
                trusted_packet_sequence_id = 1234543210
            ),
            TracePacket(
                timestamp = 430421819819000,
                timestamp_clock_id = 3,
                track_event = TrackEvent(
                    categories = listOf("art_trace"),
                    name = "androidx.benchmark.vmtrace.ArtTraceTest.myTracedMethod: ()V",
                    type = TrackEvent.Type.TYPE_SLICE_END,
                    track_uuid = 1L
                ),
                trusted_packet_sequence_id = 1234543210
            )
        )

        // Asserts that all the trace packets are found in order
        tracePackets.iterator().apply {
            while (hasNext() and toFind.isNotEmpty()) {
                val nextToFind = toFind.first()
                if (next() == nextToFind) toFind.removeFirst()
            }
        }
        assertTrue(toFind.isEmpty())
    }

    companion object {
        private fun fromAssets(filename: String) = File
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
