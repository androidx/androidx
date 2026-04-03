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

import androidx.collection.MutableLongIntMap
import androidx.collection.mutableLongIntMapOf
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * We use the [PlatformThreadContextElement] construct to know when a coroutine has suspended, and
 * about to resume on a `Thread`.
 */
public abstract class PlatformThreadContextElement
internal constructor(
    /**
     * The [Tracer] instance that can use this [PropagationToken] for context propagation when the
     * coroutine suspends and resumes.
     */
    internal open val tracer: PerfettoTracer,
    /** The `category` that a trace section belongs to. */
    public open var category: String,
    /** The `name` of the code section as it appears in a trace. */
    public open var name: String,
    /**
     * The underlying Perfetto flow ids that can be used to determine the causality for a given
     * trace section.
     */
    public open val flowIds: List<Long>,
) : AbstractCoroutineContextElement(key = KEY), PropagationToken, AutoCloseable {

    // Calls to update and restore will happen concurrently on multiple threads, if we happen
    // to jump threads between suspends and resumes. Therefore, it's important for us to track
    // begin and end per thread id. For more information refer to the `Reentrancy and thread-safety`
    // section in the documentation for `CopyableThreadContextElement`.
    @JvmField internal val started: MutableLongIntMap = mutableLongIntMapOf()

    init {
        synchronized(this) {
            // Always starts in a `begin` state.
            started[currentThreadId()] = STATE_BEGIN
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    internal inline fun synchronizedCompareAndSet(expected: Int, newValue: Int): Boolean {
        return synchronized(this) {
            // Treat the absence of an entry here as `STATE_END` given we have not emitted a
            // trace packet yet on this Thread. This is true for every child coroutine, but
            // **not** for the coroutine that kicked things off (handled by the init block).
            val id = currentThreadId()
            val current = started.getOrDefault(key = id, defaultValue = STATE_END)
            if (current == expected) {
                started[id] = newValue
                true
            } else {
                false
            }
        }
    }

    @DelicateTracingApi
    override fun contextElementOrNull(): CoroutineContext.Element? {
        return this
    }

    public override fun close() {
        if (synchronizedCompareAndSet(expected = STATE_BEGIN, newValue = STATE_END)) {
            tracer.process.currentThreadTrack().endSection()
        }
    }

    @PublishedApi
    internal companion object {
        // Used to represent that the current slice has begun.
        @PublishedApi internal const val STATE_BEGIN: Int = 1

        // Used to represent that the current slice has ended.
        @PublishedApi internal const val STATE_END: Int = 0

        @PublishedApi
        @JvmField
        internal val KEY: CoroutineContext.Key<PlatformThreadContextElement> =
            object : CoroutineContext.Key<PlatformThreadContextElement> {}
    }
}

/** Builds an instance of the Platform specific [PlatformThreadContextElement]. */
internal expect fun buildPropagationElement(
    tracer: PerfettoTracer,
    category: String,
    name: String,
    flowIds: List<Long>,
): PlatformThreadContextElement

/** Builds an instance of the Platform specific [PlatformThreadContextElement]. */
internal expect fun buildCoroutinePropagationElement(
    tracer: PerfettoTracer,
    category: String,
    name: String,
    flowIds: List<Long>,
): PlatformThreadContextElement
