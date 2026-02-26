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

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * We use the [PlatformThreadContextElement] construct to know when a coroutine has suspended, and
 * about to resume on a `Thread`.
 */
// False positive: https://youtrack.jetbrains.com/issue/KTIJ-22326
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
public abstract class PlatformThreadContextElement<S, T : Tracer>
internal constructor(
    /**
     * The [Tracer] instance that can use this [PropagationToken] for context propagation when the
     * coroutine suspends and resumes.
     */
    public open val tracer: T,
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
    // Always starts in a `begin` state.
    @JvmField internal var started: Int = STATE_BEGIN

    /**
     * This method is called **before a coroutine is resumed** on a thread that belongs to a
     * dispatcher.
     */
    internal abstract fun updateThreadContext(context: CoroutineContext): S

    /** This method is called **after** a coroutine is suspended on the current thread. */
    internal abstract fun restoreThreadContext(context: CoroutineContext, oldState: S)

    @Suppress("NOTHING_TO_INLINE")
    internal inline fun synchronizedCompareAndSet(expected: Int, newValue: Int): Boolean {
        return synchronized(this) {
            if (started == expected) {
                started = newValue
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

    @PublishedApi
    internal companion object {
        // Used to represent that the current slice has begun.
        @PublishedApi internal const val STATE_BEGIN: Int = 1

        // Used to represent that the current slice has ended.
        @PublishedApi internal const val STATE_END: Int = 0

        @PublishedApi
        @JvmField
        internal val KEY: CoroutineContext.Key<PlatformThreadContextElement<*, *>> =
            object : CoroutineContext.Key<PlatformThreadContextElement<*, *>> {}
    }
}

/** Builds an instance of the Platform specific [PlatformThreadContextElement]. */
@PublishedApi
internal expect fun <T : Tracer> buildThreadContextElement(
    tracer: T,
    category: String,
    name: String,
    flowIds: List<Long>,
    updateThreadContextBlock: (context: CoroutineContext) -> Unit,
    restoreThreadContextBlock: (context: CoroutineContext) -> Unit,
    close: (platformThreadContextElement: PlatformThreadContextElement<*, T>) -> Unit,
): PlatformThreadContextElement<Unit, T>
