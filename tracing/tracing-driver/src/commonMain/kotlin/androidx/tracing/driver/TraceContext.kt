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

import androidx.collection.mutableIntObjectMapOf

/**
 * This is something that is only typically created once per process. All the traces emitted are
 * managed and written into a single [TraceSink] in an optimal way based on the underlying platform.
 */
public open class TraceContext
internal constructor(
    /** The sink all the trace events are written to. */
    public val sink: TraceSink,
    /** Is tracing enabled ? */
    public val isEnabled: Boolean,
    /** Debug mode */
    // When debugging is on, we keep track of outstanding allocations in the pool,
    // and provide useful logging to help with debugging & testing.
    internal val isDebug: Boolean,
) : AutoCloseable {

    public constructor(sink: TraceSink, isEnabled: Boolean) : this(sink, isEnabled, isDebug = false)

    internal val processTrackLock = Any()
    internal val processes = mutableIntObjectMapOf<ProcessTrack>()

    /**
     * @return A [ProcessTrack] using the unique process [id], a [name] and the provided
     *   [TraceContext].
     */
    public open fun getOrCreateProcessTrack(id: Int, name: String): ProcessTrack {
        val track = processes[id]
        return track
            ?: synchronized(processTrackLock) {
                val track =
                    processes.getOrPut(id) { ProcessTrack(context = this, id = id, name = name) }
                check(track.name == name)
                track
            }
    }

    /** Flushes the trace packets into the underlying [TraceSink]. */
    public fun flush() {
        if (isEnabled) {
            processes.forEachValue { processTrack ->
                processTrack.flush()
                processTrack.threads.forEachValue { threadTrack -> threadTrack.flush() }
                processTrack.counters.forEachValue { counterTrack -> counterTrack.flush() }
            }
            // Call flush() on the sink after all the tracks have been flushed.
            sink.flush()
        }
    }

    override fun close() {
        flush()
        sink.close()
    }

    // Debug only
    internal fun poolableCount(): Long {
        if (!isDebug) {
            return 0L
        }
        var count = 0L
        processes.forEachValue { processTrack ->
            count += processTrack.pool.poolableCount()
            processTrack.threads.forEachValue { threadTrack ->
                count += threadTrack.pool.poolableCount()
            }

            processTrack.counters.forEachValue { counterTrack ->
                count += counterTrack.pool.poolableCount()
            }
        }
        return count
    }

    internal fun validateTrackPools(validateTrackPool: (Track) -> Unit) {
        if (isDebug) {
            processes.forEachValue { processTrack ->
                validateTrackPool(processTrack)
                processTrack.threads.forEachValue { threadTrack -> validateTrackPool(threadTrack) }

                processTrack.counters.forEachValue { counterTrack ->
                    validateTrackPool(counterTrack)
                }
            }
        }
    }
}

// An empty trace context when tracing is disabled.

internal object EmptyTraceContext : TraceContext(sink = EmptyTraceSink(), isEnabled = false) {
    internal val process = EmptyProcessTrack(this)
    internal val thread = EmptyThreadTrack(process)
    internal val counter = EmptyCounterTrack(process)

    override fun getOrCreateProcessTrack(id: Int, name: String): ProcessTrack = process
}
