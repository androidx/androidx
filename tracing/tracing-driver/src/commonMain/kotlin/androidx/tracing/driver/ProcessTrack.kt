/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.tracing.driver

import androidx.collection.mutableScatterMapOf

/** Represents a track for a process in a perfetto trace. */
public open class ProcessTrack(
    /** The tracing context. */
    context: TraceContext,
    /** The process id */
    internal val id: Int,
    /** The name of the process. */
    internal val name: String,
) : SliceTrack(context = context, uuid = monotonicId()) {
    internal val packetLock = Any()
    internal val threads = mutableScatterMapOf<String, ThreadTrack>()
    internal val counters = mutableScatterMapOf<String, CounterTrack>()

    init {
        synchronized(packetLock) {
            emitTraceEvent(immediateDispatch = true) { event ->
                event.setPreamble(
                    TrackDescriptor(
                        name,
                        uuid,
                        parentUuid = INVALID_LONG,
                        type = TRACK_DESCRIPTOR_TYPE_PROCESS,
                        pid = id,
                        tid = INVALID_INT,
                    )
                )
            }
        }
    }

    public override fun beginSection(name: String, flowIds: List<Long>) {
        if (context.isEnabled) {
            synchronized(packetLock) {
                emitTraceEvent { event -> event.setBeginSectionWithFlows(uuid, name, flowIds) }
            }
        }
    }

    public override fun beginSection(name: String) {
        if (context.isEnabled) {
            synchronized(packetLock) {
                emitTraceEvent { event -> event.setBeginSection(uuid, name) }
            }
        }
    }

    public override fun endSection() {
        if (context.isEnabled) {
            synchronized(packetLock) { emitTraceEvent { event -> event.setEndSection(uuid) } }
        }
    }

    public override fun instant(name: String) {
        if (context.isEnabled) {
            synchronized(packetLock) { emitTraceEvent { event -> event.setInstant(uuid, name) } }
        }
    }

    /**
     * @return A [ThreadTrack] for a given [ProcessTrack] using the unique thread [id] and a thread
     *   [name].
     */
    public open fun getOrCreateThreadTrack(id: Int, name: String): ThreadTrack {
        // Thread ids are only unique for lifetime of the thread and can be potentially reused.
        // Therefore we end up combining the `name` of the thread and its `id` as a key.
        val key = "$id/$name"
        return threads[key]
            ?: synchronized(threads) {
                val track =
                    threads.getOrPut(key) { ThreadTrack(id = id, name = name, process = this) }
                check(track.name == name)
                track
            }
    }

    /** @return A [CounterTrack] for a given [ProcessTrack] and the provided counter [name]. */
    public open fun getOrCreateCounterTrack(name: String): CounterTrack {
        return counters[name]
            ?: synchronized(counters) {
                counters.getOrPut(name) { CounterTrack(name = name, parent = this) }
            }
    }
}

// An empty process track when tracing is disabled.

private const val EMPTY_PROCESS_ID = -1
private const val EMPTY_PROCESS_NAME = "Empty Process"

internal class EmptyProcessTrack(context: EmptyTraceContext) :
    ProcessTrack(context = context, id = EMPTY_PROCESS_ID, name = EMPTY_PROCESS_NAME) {

    private val emptyContext: EmptyTraceContext = context

    override fun getOrCreateThreadTrack(id: Int, name: String): ThreadTrack = emptyContext.thread

    override fun getOrCreateCounterTrack(name: String): CounterTrack = emptyContext.counter
}
