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

import androidx.benchmark.MethodTracing
import androidx.benchmark.Outputs
import androidx.benchmark.createTempFileFromAsset
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import perfetto.protos.ThreadDescriptor
import perfetto.protos.Trace
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
        val tracePackets =
            ArtTrace(artTrace = artTraceFile, pid = 24877, uuidProvider = { 1L })
                .toPerfettoTrace()
                .packet

        tracePackets
            .single {
                it.track_descriptor != null && it.track_descriptor?.name == "main (Method Trace)"
            }
            .apply {
                assertEquals(
                    expected =
                        TracePacket(
                            timestamp = 430421772813000L,
                            timestamp_clock_id = 3,
                            track_descriptor =
                                TrackDescriptor(
                                    uuid = 1L,
                                    name = "main (Method Trace)",
                                    thread = ThreadDescriptor(pid = 24877, tid = 24877),
                                    disallow_merging_with_system_tracks = true,
                                ),
                        ),
                    actual = this,
                )
            }

        // check for clock sync packet
        tracePackets
            .single {
                it.clock_snapshot != null
            }
            .apply {
                assertEquals(2, clock_snapshot!!.clocks.size)
                assertEquals(1, clock_snapshot.clocks.filter { it.clock_id == 3 }.size)
                assertEquals(1, clock_snapshot.clocks.filter { it.clock_id == 6 }.size)
            }

        val targetIid =
            tracePackets
                .first { it.interned_data != null }
                .interned_data!!
                .event_names
                .first { it.name == "androidx.benchmark.vmtrace.ArtTraceTest.myTracedMethod: ()V" }
                .iid!!
        val beginPacket = tracePackets.single { it.track_event?.name_iid == targetIid }
        assertEquals(
            expected =
                TracePacket(
                    timestamp = 430421819817000L,
                    track_event =
                        TrackEvent(
                            name_iid = targetIid,
                            type = TrackEvent.Type.TYPE_SLICE_BEGIN,
                            track_uuid = 1L,
                        ),
                    trusted_packet_sequence_id = 1234565432,
                    sequence_flags = 0x2,
                ),
            actual = beginPacket,
        )

        val endPacket = tracePackets.first { it.timestamp == 430421819819000 }
        assertEquals(
            expected =
                TracePacket(
                    timestamp = 430421819819000L,
                    track_event =
                        TrackEvent(type = TrackEvent.Type.TYPE_SLICE_END, track_uuid = 1L),
                    trusted_packet_sequence_id = 1234565432,
                    sequence_flags = 0x2,
                ),
            actual = endPacket,
        )

        // ensure balanced begin/ends
        assertEquals(
            tracePackets.count { it.track_event?.type == TrackEvent.Type.TYPE_SLICE_BEGIN },
            tracePackets.count { it.track_event?.type == TrackEvent.Type.TYPE_SLICE_END },
        )
    }

    @Test
    fun embedInPerfettoTrace_zipContainer() {
        val artTraceFile = fromAssets("art-trace-test.trace")
        val systemTraceFile = createTempFileFromAsset("api31_startup_cold", ".perfetto-trace")
        val perfettoZipFile =
            File.createTempFile("test-zip-trace", ".perfetto-trace", Outputs.dirUsableByAppAndShell)
        ZipOutputStream(FileOutputStream(perfettoZipFile)).use { zipOut ->
            zipOut.putNextEntry(ZipEntry("Trace_output.pb"))
            zipOut.write(systemTraceFile.readBytes())
            zipOut.closeEntry()
        }

        MethodTracing.embedInPerfettoTrace(
            profilerTrace = artTraceFile,
            perfettoTrace = perfettoZipFile,
        )

        // Read entries from the resulting ZIP
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(FileInputStream(perfettoZipFile)).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                entries[entry.name] = zipIn.readBytes()
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }

        assertTrue(entries.containsKey("Trace_output.pb"))
        assertTrue(entries.containsKey("MethodTrace.pb"))
        val systemTraceBytes = systemTraceFile.readBytes()
        assertEquals(systemTraceBytes.toList(), entries["Trace_output.pb"]!!.toList())

        // Validate decoded MethodTrace.pb packets
        val trace = Trace.ADAPTER.decode(entries["MethodTrace.pb"]!!)
        assertTrue(
            trace.packet.any { packet ->
                packet.interned_data?.event_names?.any {
                    it.name == "androidx.benchmark.vmtrace.ArtTraceTest.myTracedMethod: ()V"
                } == true
            }
        )
    }

    companion object {
        private fun fromAssets(@Suppress("SameParameterValue") filename: String) =
            File.createTempFile(filename, "", Outputs.dirUsableByAppAndShell).apply {
                InstrumentationRegistry.getInstrumentation()
                    .context
                    .assets
                    .open(filename)
                    .copyTo(outputStream())
            }
    }
}
