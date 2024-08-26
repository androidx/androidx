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
import androidx.annotation.RestrictTo
import androidx.benchmark.InMemoryTracing.commitToTrace
import perfetto.protos.CounterDescriptor
import perfetto.protos.ThreadDescriptor
import perfetto.protos.Trace
import perfetto.protos.TracePacket
import perfetto.protos.TrackDescriptor
import perfetto.protos.TrackEvent

/**
 * Tracing api that writes events directly into memory.
 *
 * This has a few advantages over typical atrace:
 * - can record while atrace isn't captured either due to platform limitations (old platforms may
 *   only allow one process to be traced at a time), or when debugging benchmark performance, it can
 *   capture when atrace isn't active (e.g. tracing the start/stop of perfetto)
 * - can create events asynchronously, deferring record cost (e.g. micro creating events after all
 *   measurements, using existing timestamps)
 * - can customize presentation of events in trace
 *
 * After trace processing, the extra events (before _and_ after the measureBlock section of a
 * benchmark) can be added to the trace by calling [commitToTrace], and appending that to the trace
 * on-disk.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object InMemoryTracing {
    /**
     * All events emitted by the benchmark annotation should have the same value. the value needs to
     * not conflict with any sequence id emitted in the trace. You can rely on the fact that traces
     * will never contain an ID > kMaxProducerID * kMaxWriterID (65536 * 1024) = 67108864. A high
     * number will be good enough without having to read the trace (unless something else outside of
     * your control is also emitting fake slices)
     */
    private const val TRUSTED_PACKET_SEQUENCE_ID: Int = 1_234_543_210

    /**
     * This is a unique ID of the track. The state is global and 64 bit. Tracks are obtained by
     * hashing pids/tids. Just picked an arbitrary 64 bit value. You have more probability of
     * winning the lottery than hitting a collision.
     */
    private const val UUID = 123_456_543_210L

    /** Clock id for clock used by tracing events - this corresponds to CLOCK_MONOTONIC */
    private const val CLOCK_ID = 3

    /** Tag to enable post-filtering of events in the trace. */
    private val TRACK_EVENT_CATEGORIES = listOf("benchmark")

    /**
     * For perf/simplicity, this isn't protected by a lock - it should only ever be accessed by the
     * test thread, and dumped/reset between tests.
     */
    val events = mutableListOf<TracePacket>()

    /** Map of counter name to UUID, populated by [counterNameToTrackUuid] */
    private val counterTracks = mutableMapOf<String, Long>()

    private fun counterNameToTrackUuid(name: String): Long {
        return counterTracks.getOrPut(name) { UUID + 1 + counterTracks.size }
    }

    fun clearEvents() {
        events.clear()
        counterTracks.clear()
    }

    /** Capture trace state, and return as a Trace(), which can be appended to a trace file. */
    fun commitToTrace(label: String): Trace {
        val capturedEvents = events.toList()
        val capturedCounterDescriptors =
            counterTracks.map { (name, uuid) ->
                TracePacket(
                    timestamp_clock_id = CLOCK_ID,
                    incremental_state_cleared = true,
                    track_descriptor =
                        TrackDescriptor(
                            uuid = uuid,
                            parent_uuid = UUID,
                            name = name,
                            counter = CounterDescriptor()
                        )
                )
            }

        clearEvents()
        return Trace(
            listOf(
                TracePacket(
                    timestamp_clock_id = CLOCK_ID,
                    incremental_state_cleared = true,
                    track_descriptor =
                        TrackDescriptor(
                            uuid = UUID,
                            name = label,
                            thread = ThreadDescriptor(pid = Process.myPid(), tid = Process.myTid()),
                            // currently separate for clarity, to allow InMemoryTrace events to have
                            // a visible
                            // track name, but not override the thread name
                            disallow_merging_with_system_tracks = true
                        )
                )
            ) + capturedCounterDescriptors + capturedEvents
        )
    }

    fun beginSection(
        label: String,
        nanoTime: Long = System.nanoTime(),
        counterNames: List<String> = emptyList(),
        counterValues: List<Double> = emptyList()
    ) {
        require(counterNames.size == counterValues.size)
        events.add(
            TracePacket(
                timestamp = nanoTime,
                timestamp_clock_id = CLOCK_ID,
                trusted_packet_sequence_id = TRUSTED_PACKET_SEQUENCE_ID,
                track_event =
                    TrackEvent(
                        type = TrackEvent.Type.TYPE_SLICE_BEGIN,
                        track_uuid = UUID,
                        categories = TRACK_EVENT_CATEGORIES,
                        name = label,
                        extra_double_counter_values = counterValues,
                        extra_double_counter_track_uuids =
                            counterNames.map { counterNameToTrackUuid(it) },
                    )
            )
        )
    }

    fun endSection(nanoTime: Long = System.nanoTime()) {
        events.add(
            TracePacket(
                timestamp = nanoTime,
                timestamp_clock_id = CLOCK_ID,
                trusted_packet_sequence_id = TRUSTED_PACKET_SEQUENCE_ID,
                track_event =
                    TrackEvent(
                        type = TrackEvent.Type.TYPE_SLICE_END,
                        track_uuid = UUID,
                    )
            )
        )
    }

    fun counter(name: String, value: Double, nanoTime: Long = System.nanoTime()) {
        events.add(
            TracePacket(
                timestamp = nanoTime,
                timestamp_clock_id = CLOCK_ID,
                trusted_packet_sequence_id = TRUSTED_PACKET_SEQUENCE_ID,
                track_event =
                    TrackEvent(
                        type = TrackEvent.Type.TYPE_COUNTER,
                        double_counter_value = value,
                        track_uuid = counterNameToTrackUuid(name),
                        // track_uuid = UUID
                    )
            )
        )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun <T> inMemoryTrace(label: String, block: () -> T): T {
    InMemoryTracing.beginSection(label)
    return try {
        block()
    } finally {
        InMemoryTracing.endSection()
    }
}
