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

import androidx.annotation.RestrictTo
import perfetto.protos.Trace
import perfetto.protos.TracePacket
import perfetto.protos.TrackDescriptor
import perfetto.protos.TrackEvent

/**
 * Userspace-buffer-based tracing api, that provides implementation for [userspaceTrace].
 *
 * This records while atrace isn't capturing by storing trace events manually in a list of
 * in-userspace-memory perfetto protos.
 *
 * After trace processing, the extra events (before _and_ after the measureBlock section of a
 * benchmark) can be added to the trace by calling [commitToTrace], and appending that to the
 * trace on-disk.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object UserspaceTracing {
    /**
     * All events emitted by the benchmark annotation should have the same value.
     * the value needs to not conflict with any sequence id emitted in the trace.
     * You can rely on the fact that traces will never contain an ID >
     * kMaxProducerID * kMaxWriterID (65536 * 1024) = 67108864. A high number will
     * be good enough without having to read the trace (unless something else
     * outside of your control is also emitting fake slices)
     */
    private const val TRUSTED_PACKET_SEQUENCE_ID: Int = 1_234_543_210

    /**
     * This is a unique ID of the track. The state is global and 64 bit. Tracks are
     * obtained by hashing pids/tids. Just picked an arbitrary 64 bit value. You have
     * more probability of winning the lottery than hitting a collision.
     */
    private const val UUID = 123_456_543_210L

    /**
     * Clock id for clock used by tracing events - this corresponds to CLOCK_MONOTONIC
     */
    private const val CLOCK_ID = 3

    /**
     * Name of track in for userspace tracing events
     */
    private const val TRACK_DESCRIPTOR_NAME = "Macrobenchmark"

    /**
     * Tag to enable post-filtering of events in the trace.
     */
    private val TRACK_EVENT_CATEGORIES = listOf("benchmark")

    private fun createInitialTracePacket() = TracePacket(
        timestamp = System.nanoTime(),
        timestamp_clock_id = CLOCK_ID,
        incremental_state_cleared = true,
        track_descriptor = TrackDescriptor(
            uuid = UUID,
            name = TRACK_DESCRIPTOR_NAME
        )
    )

    /**
     * For perf/simplicity, this isn't protected by a lock - it should only every be
     * accessed by the test thread, and dumped/reset between tests.
     */
    val events = mutableListOf(createInitialTracePacket())

    /**
     * Capture trace state, and return as a Trace(), which can be appended to a trace file.
     */
    fun commitToTrace(): Trace {
        val capturedEvents = events.toList()
        events.clear()
        events.add(createInitialTracePacket())
        return Trace(capturedEvents)
    }

    fun startSection(label: String) {
        events.add(
            TracePacket(
                timestamp = System.nanoTime(),
                timestamp_clock_id = CLOCK_ID,
                trusted_packet_sequence_id = TRUSTED_PACKET_SEQUENCE_ID,
                track_event = TrackEvent(
                    type = TrackEvent.Type.TYPE_SLICE_BEGIN,
                    track_uuid = UUID,
                    categories = TRACK_EVENT_CATEGORIES,
                    name = label
                )
            )
        )
    }

    fun endSection() {
        events.add(
            TracePacket(
                timestamp = System.nanoTime(),
                timestamp_clock_id = CLOCK_ID,
                trusted_packet_sequence_id = TRUSTED_PACKET_SEQUENCE_ID,
                track_event = TrackEvent(
                    type = TrackEvent.Type.TYPE_SLICE_END,
                    track_uuid = UUID,
                )
            )
        )
    }
}

/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun <T> userspaceTrace(label: String, block: () -> T): T {
    UserspaceTracing.startSection(label)
    return try {
        block()
    } finally {
        UserspaceTracing.endSection()
    }
}
