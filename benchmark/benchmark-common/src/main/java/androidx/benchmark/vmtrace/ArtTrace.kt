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

import java.io.File
import java.util.UUID
import perfetto.protos.Trace
import perfetto.protos.TracePacket
import perfetto.protos.TrackDescriptor
import perfetto.protos.TrackEvent

typealias UuidProvider = () -> (Long)

internal class ArtTrace(
    private val artTrace: File,
    private val uuidProvider: UuidProvider = {
        UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE
    }
) {

    private val clockId = 3
    private val trustedPacketSequenceId: Int = 1_234_543_210

    fun toPerfettoTrace(): Trace {
        val events = mutableListOf<TracePacket>().also {
            val parser = PerfettoVmTraceParser(
                events = it,
                trustedPacketSequenceId = trustedPacketSequenceId,
                clockId = clockId,
                uuidProvider = uuidProvider
            )
            VmTraceParser(artTrace, parser).parse()
        }
        return Trace(events)
    }

    private class PerfettoVmTraceParser(
        private val events: MutableList<TracePacket>,
        private val trustedPacketSequenceId: Int,
        private val clockId: Int,
        private val uuidProvider: UuidProvider
    ) : VmTraceHandler {

        private data class ThreadTrack(
            val uuid: Long,
            val name: String,
            var created: Boolean
        )

        private val props: MutableMap<String, String> = mutableMapOf()
        private val threads: MutableMap<Int, ThreadTrack> = mutableMapOf()
        private val methods: MutableMap<Long, MethodInfo> = mutableMapOf()
        private var version: Int = -1
        private var startTimeUs: Long = 0L

        override fun setVersion(version: Int) {
            this.version = version
        }

        override fun setProperty(key: String, value: String) {
            this.props[key] = value
        }

        override fun addMethod(id: Long, info: MethodInfo) {
            this.methods[id] = info
        }

        override fun setStartTimeUs(startTimeUs: Long) {
            this.startTimeUs = startTimeUs
        }

        override fun addThread(id: Int, name: String) {
            if (id in threads) return
            this.threads[id] = ThreadTrack(
                uuid = uuidProvider(),
                name = name,
                created = false
            )
        }

        override fun addMethodAction(
            threadId: Int,
            methodId: Long,
            methodAction: TraceAction,
            threadTime: Int,
            globalTime: Int
        ) {
            val threadTrack = threads[threadId]!!

            if (!threadTrack.created) {
                events.add(
                    TracePacket(
                        timestamp = startTimeUs * 1000,
                        timestamp_clock_id = clockId,
                        incremental_state_cleared = true,
                        track_descriptor = TrackDescriptor(
                            uuid = threadTrack.uuid,
                            name = threadTrack.name
                        )
                    )
                )
                threadTrack.created = true
            }

            events.add(
                TracePacket(
                    timestamp = (startTimeUs + globalTime) * 1000,
                    timestamp_clock_id = clockId,
                    trusted_packet_sequence_id = trustedPacketSequenceId,
                    track_event = TrackEvent(
                        type = when (methodAction) {
                            TraceAction.METHOD_ENTER -> TrackEvent.Type.TYPE_SLICE_BEGIN
                            TraceAction.METHOD_EXIT -> TrackEvent.Type.TYPE_SLICE_END
                            TraceAction.METHOD_EXIT_UNROLL -> TrackEvent.Type.TYPE_SLICE_END
                        },
                        track_uuid = threadTrack.uuid,
                        categories = listOf("art_trace"),
                        name = methods[methodId]?.fullName ?: "unknown-method"
                    )
                )
            )
        }
    }
}
