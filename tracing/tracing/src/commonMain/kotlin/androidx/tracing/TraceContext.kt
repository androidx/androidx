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

package androidx.tracing

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import kotlin.concurrent.Volatile

/**
 * This is something that is only typically created once per process. All the traces emitted are
 * managed and written into a single [AbstractTraceSink] in an optimal way based on the underlying
 * platform.
 */
// False positive: https://youtrack.jetbrains.com/issue/KTIJ-22326
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@RestrictTo(Scope.LIBRARY_GROUP)
public open class TraceContext
internal constructor(
    /** The sink all the trace events are written to. */
    @JvmField public val sink: AbstractTraceSink,
    /** Is tracing enabled ? */
    @JvmField public val isEnabled: Boolean,
    /** Debug mode */
    // When debugging is on, we keep track of outstanding allocations in the pool,
    // and provide useful logging to help with debugging & testing.
    @JvmField internal val isDebug: Boolean,
) : AutoCloseable {

    public constructor(
        sink: AbstractTraceSink,
        isEnabled: Boolean,
    ) : this(sink, isEnabled, isDebug = false)

    @JvmField internal val processTrackLock = Any()

    @JvmField @Volatile internal var isProcessInitialized: Boolean = false
    @RestrictTo(Scope.LIBRARY_GROUP) public open lateinit var process: ProcessTrack

    /** Create an instance of a [Tracer] that can be used to emit trace events. */
    public open fun createTracer(): Tracer {
        return PerfettoTracer(context = this)
    }

    /**
     * @return A [ProcessTrack] using the unique process [id], a [name] and the provided
     *   [TraceContext].
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public open fun createProcessTrack(id: Int, name: String) {
        synchronized(processTrackLock) {
            if (!isProcessInitialized) {
                process = ProcessTrack(context = this, id = id, name = name)
                isProcessInitialized = true
            }
        }
    }

    /** Flushes the trace packets into the underlying [AbstractTraceSink]. */
    public fun flush() {
        if (isEnabled) {
            process.flush()
            process.threads.forEachValue { threadTrack -> threadTrack.flush() }
            process.counters.forEachValue { counterTrack -> counterTrack.flush() }

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
        count += process.pool.poolableCount()
        process.threads.forEachValue { threadTrack -> count += threadTrack.pool.poolableCount() }
        process.counters.forEachValue { counterTrack -> count += counterTrack.pool.poolableCount() }
        return count
    }

    internal fun validateTrackPools(validateTrackPool: (Track) -> Unit) {
        if (isDebug) {
            validateTrackPool(process)
            process.threads.forEachValue { threadTrack -> validateTrackPool(threadTrack) }
            process.counters.forEachValue { counterTrack -> validateTrackPool(counterTrack) }
        }
    }
}

// An empty trace context when tracing is disabled.
internal object EmptyTraceContext : TraceContext(sink = EmptyTraceSink(), isEnabled = false) {
    val track = EmptyProcessTrack(context = this)
    override var process: ProcessTrack = track
    internal val thread = EmptyThreadTrack(track)
    internal val counter = EmptyCounterTrack(track)
}
