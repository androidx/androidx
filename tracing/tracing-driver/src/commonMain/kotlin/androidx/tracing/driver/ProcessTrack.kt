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

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.collection.mutableIntObjectMapOf
import androidx.collection.mutableScatterMapOf

/** Represents a track for a process in a perfetto trace. */
@RestrictTo(Scope.LIBRARY_GROUP)
public open class ProcessTrack(
    /** The tracing context. */
    context: TraceContext,
    /** The process id */
    public val id: Int,
    /** The name of the process. */
    public val name: String,
) : SliceTrack(context = context, uuid = monotonicId()) {

    internal val threads = mutableIntObjectMapOf<ThreadTrack>()
    internal val counters = mutableScatterMapOf<String, CounterTrack>()

    init {
        synchronized(traceEventScope) {
            val event = obtainTraceEvent()
            if (event != null) {
                event.setPreamble(
                    TrackDescriptor(
                        name,
                        uuid,
                        parentUuid = DEFAULT_LONG,
                        type = TRACK_DESCRIPTOR_TYPE_PROCESS,
                        pid = id,
                        tid = DEFAULT_INT,
                    )
                )
                dispatchTraceEvent(event, immediateDispatch = true)
            }
        }
    }

    /**
     * @return A [ThreadTrack] for a given [ProcessTrack] using the unique thread [id] and a thread
     *   [name].
     */
    public open fun getOrCreateThreadTrack(id: Int, name: String): ThreadTrack {
        return synchronized(threads) {
            threads.getOrPut(key = id) { ThreadTrack(id = id, name = name, process = this) }
        }
    }

    /** @return A [CounterTrack] for a given [ProcessTrack] and the provided counter [name]. */
    public open fun getOrCreateCounterTrack(name: String): CounterTrack {
        return synchronized(counters) {
            counters.getOrPut(key = name) { CounterTrack(name = name, parent = this) }
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
