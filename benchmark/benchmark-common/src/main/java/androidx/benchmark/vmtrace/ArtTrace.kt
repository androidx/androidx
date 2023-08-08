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
import perfetto.protos.EventName
import perfetto.protos.InternedData
import perfetto.protos.ThreadDescriptor
import perfetto.protos.Trace
import perfetto.protos.TracePacket
import perfetto.protos.TracePacketDefaults
import perfetto.protos.TrackDescriptor
import perfetto.protos.TrackEvent
import perfetto.protos.TrackEventDefaults

typealias UuidProvider = () -> (Long)

internal class ArtTrace(
    private val artTrace: File,
    private val uuidProvider: UuidProvider = {
        UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE
    },
    private val pid: Int = android.os.Process.myPid(),
) {
    fun toPerfettoTrace(): Trace {
        val events = mutableListOf<TracePacket>().also {
            val parser = PerfettoVmTraceParser(
                events = it,
                clockId = clockId,
                uuidProvider = uuidProvider,
                pid = pid,
            )
            VmTraceParser(artTrace, parser).parse()
            parser.flushEndEvents()
        }
        return Trace(events)
    }

    private class PerfettoVmTraceParser(
        private val events: MutableList<TracePacket>,
        private val clockId: Int,
        private val uuidProvider: UuidProvider,
        private val pid: Int,
    ) : VmTraceHandler {

        private data class ThreadTrack(
            val uuid: Long,
            val name: String,
            var created: Boolean,
            var depth: Int = 0,
            var isDefault: Boolean = false
        )

        private val props: MutableMap<String, String> = mutableMapOf()
        private val threads: MutableMap<Int, ThreadTrack> = mutableMapOf()
        private val methods: MutableMap<Long, MethodInfo> = mutableMapOf()
        private var version: Int = -1
        private var startTimeUs: Long = 0L
        private var maxTimeNs: Long = 0L

        /**
         * Offset from methodId -> name_iid in perfetto trace, required to prevent methods from
         * starting at 0.
         */
        val internOffset = 100
        var hasEmittedInternedData: Boolean = false
        fun getInternedData(): InternedData? {
            return if (hasEmittedInternedData) {
                null
            } else {
                hasEmittedInternedData = true
                InternedData(methods.values.map {
                    EventName(it.id + internOffset, it.fullName)
                })
            }
        }

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
                // NOTE: we avoid trusted_packet_sequence_id here, so we don't have to participate
                // in sequence state sharing with the begin/end events
                events.add(
                    TracePacket(
                        timestamp = startTimeUs * 1000,
                        timestamp_clock_id = clockId,
                        track_descriptor = TrackDescriptor(
                            uuid = threadTrack.uuid,
                            name = threadTrack.name + " (Method Trace)",
                            thread = ThreadDescriptor(pid = pid, tid = threadId),
                            // Prevent merging track with existing perfetto thread track, since art
                            // traces are at microsecond granularity. This isn't necessary in most
                            // cases since a benchmark isn't likely to overlap atrace events, this
                            // is just done out of caution
                            disallow_merging_with_system_tracks = true
                        ),
                    )
                )
                threadTrack.created = true
            }

            val timestampNs = (startTimeUs + globalTime) * 1000L
            maxTimeNs = maxTimeNs.coerceAtLeast(timestampNs)
            val isBegin = methodAction == TraceAction.METHOD_ENTER
            if (isBegin || threadTrack.depth > 0) { // avoid unpaired end events
                val internedData = getInternedData()
                if (internedData != null) {
                    // first thread with an event becomes default track for all events in trace
                    // this is simple, but works ideally for common case
                    threadTrack.isDefault = true
                }
                events.add(
                    TracePacket(
                        timestamp = timestampNs,
                        trusted_packet_sequence_id = trustedPacketSequenceId,
                        track_event = TrackEvent(
                            type = if (isBegin) {
                                threadTrack.depth++
                                TrackEvent.Type.TYPE_SLICE_BEGIN
                            } else {
                                threadTrack.depth = (threadTrack.depth - 1).coerceAtLeast(0)
                                TrackEvent.Type.TYPE_SLICE_END
                            },
                            track_uuid = if (threadTrack.isDefault) null else threadTrack.uuid,
                            name_iid = if (isBegin) methodId + internOffset else null
                        ),
                        interned_data = internedData,
                        sequence_flags = if (internedData != null) {
                            SequenceDataInitial
                        } else {
                            SequenceDataSubsequent
                        },
                        trace_packet_defaults = if (internedData != null) {
                            TracePacketDefaults(
                                timestamp_clock_id = clockId,
                                track_event_defaults = TrackEventDefaults(threadTrack.uuid)
                            )
                        } else {
                            null
                        }
                    )
                )
            }
        }

        fun flushEndEvents() {
            threads.values.forEach { threadTrack ->
                repeat(threadTrack.depth) {
                    events.add(
                        TracePacket(
                            timestamp = maxTimeNs,
                            trusted_packet_sequence_id = trustedPacketSequenceId,
                            track_event = TrackEvent(
                                type = TrackEvent.Type.TYPE_SLICE_END,
                                track_uuid = threadTrack.uuid,
                            )
                        )
                    )
                }
            }
        }
    }

    companion object {
        private const val clockId = 3
        private const val trustedPacketSequenceId: Int = 1_234_565_432

        private val SequenceDataInitial =
            TracePacket.SequenceFlags.SEQ_INCREMENTAL_STATE_CLEARED.value.or(
                TracePacket.SequenceFlags.SEQ_NEEDS_INCREMENTAL_STATE.value
            )
        private val SequenceDataSubsequent =
            TracePacket.SequenceFlags.SEQ_NEEDS_INCREMENTAL_STATE.value
    }
}
