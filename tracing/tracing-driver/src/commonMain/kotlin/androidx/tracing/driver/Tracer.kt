/*
 * Copyright 2025 The Android Open Source Project
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

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext

/**
 * A [Tracer] is the entry point for all Tracing APIs.
 *
 * To create a Tracer use the `createTracer` API.
 */
// False positive: https://youtrack.jetbrains.com/issue/KTIJ-22326
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
public abstract class Tracer(
    /**
     * The name of the event that is being traced. Typically [Tracer] instances are associated with
     * application entry points so each trace can be disambiguated.
     */
    @JvmField
    @field:Suppress("MutableBareField") // public / mutable to minimize overhead
    public val name: String,

    /** Is set to `true` if Tracing is enabled. */
    @JvmField
    @field:Suppress("MutableBareField") // public / mutable to minimize overhead
    public val isEnabled: Boolean,
) : AutoCloseable {
    /**
     * This gives the ability to control how context propagation works for a
     * [androidx.tracing.driver.Tracer].
     *
     * The default implementation does not support context propagation in non-suspending contexts by
     * returning a [PropagationUnsupportedToken] instance. Alternative implementations can choose to
     * override this method to do something different. Examples include using a `ThreadLocal` like
     * primitive track of [PropagationToken]s across threads.
     */
    @DelicateTracingApi public abstract fun tokenFromThreadContext(): PropagationToken

    /**
     * This gives the ability to control how context propagation works for a
     * [androidx.tracing.driver.Tracer].
     *
     * Alternative implementations can choose to override this method to do something different.
     * Examples include writing your own [kotlin.coroutines.CoroutineContext.Element] that bridges
     * the gap with non-coroutine code by using `ThreadLocal`s under the hood.
     */
    @DelicateTracingApi
    public abstract suspend fun tokenFromCoroutineContext(): CoroutinePropagationToken

    /**
     * Writes a trace message indicating that a given section of code has begun.
     *
     * Should be followed by a corresponding call to [AutoCloseable.close] returned by the call to
     * `beginSection`. If the corresponding [AutoCloseable.close] is missing, the section will be
     * present in the trace, but non-terminating (generally shown as fading out to the left).
     *
     * @param category The category that the trace section belongs to. Apps can potentially filter
     *   sections to the categories that they are interested in looking into.
     * @param name The name of the code section to appear in the trace.
     * @param token An optional [PropagationToken] that can be used for context propagation. The
     *   default implementation uses a list of [Long]s which will connect this trace section to
     *   other sections in the trace, potentially on different Tracks. The start and end of each
     *   trace `flow` (connection) between trace sections must share an ID, so each `Long` must be
     *   unique to each `flow` in the trace. When a `null` value is specified, the [Tracer]
     *   implementation obtains the token by calling [tokenFromThreadContext].
     * @param isRoot An hint that tells the [Tracer] that this trace section is an entry point that
     *   all subsequent trace spans can be attributed to. Some [Tracer] implementations treat trace
     *   sections as a forest, and require that there is at least one top level root span.
     * @return A [EventMetadataCloseable] instance that can be used to add additional metadata and
     *   close the trace section.
     */
    @DelicateTracingApi
    public abstract fun beginSectionWithMetadata(
        category: String,
        name: String,
        token: PropagationToken?,
        isRoot: Boolean,
    ): EventMetadataCloseable

    /**
     * Writes a trace message indicating that a given suspending section of code has begun.
     *
     * Should be followed by a corresponding call to [AutoCloseable.close] returned by the call to
     * `beginCoroutineSectionWithMetadata`. If the corresponding [AutoCloseable.close] is missing,
     * the section will be present in the trace, but non-terminating (generally shown as fading out
     * to the left).
     *
     * @param category The category that the trace section belongs to. Apps can potentially filter
     *   sections to the categories that they are interested in looking into.
     * @param name The name of the code section to appear in the trace.
     * @param token An optional [CoroutinePropagationToken] that can be used for context
     *   propagation. The default implementation uses a list of [Long]s which will connect this
     *   trace section to other sections in the trace, potentially on different Tracks. The start
     *   and end of each trace `flow` (connection) between trace sections must share an ID, so each
     *   `Long` must be unique to each `flow` in the trace. When a `null` value is specified, the
     *   [Tracer] obtains a token by calling [tokenFromCoroutineContext].
     * @param isRoot An hint that tells the [Tracer] that this trace section is an entry point that
     *   all subsequent trace spans can be attributed to. Some [Tracer] implementations treat trace
     *   sections as a forest, and require that there is at least one top level root span.
     * @return A [EventMetadataCloseable] instance that can be used to add additional metadata and
     *   close the trace section.
     */
    @DelicateTracingApi
    public abstract suspend fun beginCoroutineSectionWithMetadata(
        category: String,
        name: String,
        token: CoroutinePropagationToken?,
        isRoot: Boolean,
    ): EventMetadataCloseable

    /**
     * @return The [Counter] instance for the provided [name]. This can be used to emit counter
     *   events.
     */
    @DelicateTracingApi public abstract fun counter(name: String): Counter

    /** Emits a zero duration section to the Trace with the provided [name]. */
    @DelicateTracingApi public abstract fun instant(name: String)

    /**
     * Writes a trace message indicating that a given section of code has begun.
     *
     * Should be followed by a corresponding call to [AutoCloseable.close] returned by the call to
     * `beginSection`. If the corresponding [AutoCloseable.close] is missing, the section will be
     * present in the trace, but non-terminating (generally shown as fading out to the left).
     *
     * @param category The category that the trace section belongs to. Apps can potentially filter
     *   sections to the categories that they are interested in looking into.
     * @param name The name of the code section to appear in the trace.
     * @param token An optional [PropagationToken] that can be used for context propagation. The
     *   default implementation uses a list of [Long]s which will connect this trace section to
     *   other sections in the trace, potentially on different Tracks. The start and end of each
     *   trace `flow` (connection) between trace sections must share an ID, so each `Long` must be
     *   unique to each `flow` in the trace. When a `null` value is specified, the [Tracer]
     *   implementation obtains the token by calling [tokenFromThreadContext].
     * @param isRoot An hint that tells the [Tracer] that this trace section is an entry point that
     *   all subsequent trace spans can be attributed to. Some [Tracer] implementations treat trace
     *   sections as a forest, and require that there is at least one top level root span.
     * @param metadataBlock The lambda that can be used to decorate the [TraceEvent] instance with
     *   additional debug annotations. Return `true` in the block if you intend to dispatch the
     *   [TraceEvent] after all metadata has been added. For e.g. applications might want to filter
     *   [TraceEvent]s scoped to well known categories.
     * @return A [EventMetadataCloseable] instance that can be used to add additional metadata and
     *   close the trace section.
     */
    public inline fun beginSection(
        category: String,
        name: String,
        token: PropagationToken?,
        isRoot: Boolean = false,
        crossinline metadataBlock: EventMetadata.() -> Unit,
    ): AutoCloseable {
        val result =
            beginSectionWithMetadata(
                category = category,
                name = name,
                token = token,
                isRoot = isRoot,
            )
        metadataBlock(result.metadata)
        result.metadata.dispatchToTraceSink()
        return result.closeable
    }

    /**
     * Writes a trace message indicating that a given suspending section of code has begun.
     *
     * Should be followed by a corresponding call to [AutoCloseable.close] returned by the call to
     * `beginCoroutineSectionWithMetadata`. If the corresponding [AutoCloseable.close] is missing,
     * the section will be present in the trace, but non-terminating (generally shown as fading out
     * to the left).
     *
     * @param category The category that the trace section belongs to. Apps can potentially filter
     *   sections to the categories that they are interested in looking into.
     * @param name The name of the code section to appear in the trace.
     * @param token An optional [CoroutinePropagationToken] that can be used for context
     *   propagation. The default implementation uses a list of [Long]s which will connect this
     *   trace section to other sections in the trace, potentially on different Tracks. The start
     *   and end of each trace `flow` (connection) between trace sections must share an ID, so each
     *   `Long` must be unique to each `flow` in the trace. When a `null` value is specified, the
     *   [Tracer] obtains a token by calling [tokenFromCoroutineContext].
     * @param isRoot A hint that tells the [Tracer] that this trace section is an entry point that
     *   all subsequent trace spans can be attributed to. Some [Tracer] implementations treat trace
     *   sections as a forest, and require that there is at least one top level root span.
     * @param metadataBlock The lambda that can be used to decorate the [TraceEvent] instance with
     *   additional debug annotations. Return `true` in the block if you intend to dispatch the
     *   [TraceEvent] after all metadata has been added. For e.g. applications might want to filter
     *   [TraceEvent]s scoped to well known categories.
     * @return A [EventMetadataCloseable] instance that can be used to add additional metadata and
     *   close the trace section.
     */
    public suspend inline fun beginCoroutineSection(
        category: String,
        name: String,
        token: CoroutinePropagationToken?,
        isRoot: Boolean = false,
        crossinline metadataBlock: EventMetadata.() -> Unit,
    ): EventMetadataCloseable {
        val result =
            beginCoroutineSectionWithMetadata(
                category = category,
                name = name,
                token = token,
                isRoot = isRoot,
            )
        metadataBlock(result.metadata)
        result.metadata.dispatchToTraceSink()
        return result
    }

    /**
     * Traces the [block] as a named section of code in the trace with context propagation.
     *
     * @param category The [String] category. Its useful to categorize [TraceEvent]s, so that they
     *   can be filtered if necessary using the [metadataBlock].
     * @param name The name of the trace section.
     * @param token The optional [PropagationToken] instance to use for context propagation. This
     *   defaults to the token returned by [tokenFromThreadContext].
     * @param isRoot An hint that tells the [Tracer] that this trace section is an entry point that
     *   all subsequent trace spans can be attributed to. Some [Tracer] implementations treat trace
     *   sections as a forest, and require that there is at least one top level root span.
     * @param metadataBlock The lambda that can be used to decorate the [TraceEvent] instance with
     *   additional debug annotations. Return `true` in the block if you intend to dispatch the
     *   [TraceEvent] after all metadata has been added. For e.g. applications might want to filter
     *   [TraceEvent]s scoped to well known categories.
     * @param block The block of code being traced.
     * @return The [AutoCloseable] instance that can be used to close the trace section.
     */
    @JvmOverloads
    public inline fun <T> trace(
        category: String,
        name: String,
        token: PropagationToken? = null,
        isRoot: Boolean = false,
        crossinline metadataBlock: EventMetadata.() -> Unit = {},
        crossinline block: () -> T,
    ): T {
        val closeable =
            if (!isEnabled) {
                EmptyCloseable
            } else {
                beginSection(
                    category = category,
                    name = name,
                    token = token,
                    isRoot = isRoot,
                    metadataBlock = metadataBlock,
                )
            }
        // Not using .use here to avoid a layer of indirection in the implementation of
        // AutoCloseable.use on Android.
        try {
            return block()
        } finally {
            closeable.close()
        }
    }

    /**
     * Traces the suspending [block] as a named section of code in the trace with context
     * propagation. The [tokenFromCoroutineContext] method is used to obtain the [PropagationToken]
     * for context propagation.
     *
     * @param category The [String] category. Its useful to categorize [TraceEvent]s, so that they
     *   can be filtered if necessary using the [metadataBlock].
     * @param name The name of the trace section.
     * @param token An optional explicit [CoroutinePropagationToken] instance that is intended to be
     *   used for manual context propagation. This might be useful in instances where the
     *   implementation of context propagation was to distinguish between job executions that are
     *   well scoped vs. fire and forget. When `null`, the [Tracer] instance delegates to the
     *   implementation of [tokenFromCoroutineContext].
     * @param isRoot An hint that tells the [Tracer] that this trace section is an entry point that
     *   all subsequent trace spans can be attributed to. Some [Tracer] implementations treat trace
     *   sections as a forest, and require that there is at least one top level root span.
     * @param metadataBlock The lambda that can be used to decorate the [TraceEvent] instance with
     *   additional debug annotations. Return `true` in the block if you intend to dispatch the
     *   [TraceEvent] after all metadata has been added. For e.g. applications might want to filter
     *   [TraceEvent]s scoped to well known categories.
     * @param block The suspending block of code being traced.
     * @return The [AutoCloseable] instance that can be used to close the trace section.
     */
    @JvmOverloads
    public suspend inline fun <T> traceCoroutine(
        category: String,
        name: String,
        token: CoroutinePropagationToken? = null,
        isRoot: Boolean = false,
        crossinline metadataBlock: EventMetadata.() -> Unit = {},
        crossinline block: suspend () -> T,
    ): T {
        val result =
            if (!isEnabled) {
                EmptyEventMetadataCloseable
            } else {
                beginCoroutineSection(
                    category = category,
                    name = name,
                    token = token,
                    isRoot = isRoot,
                    metadataBlock = metadataBlock,
                )
            }
        // Not using .use here to avoid a layer of indirection in the implementation of
        // AutoCloseable.use on Android.
        try {
            // If the coroutinePropagationToken needs to be installed then install it
            // before dispatching the call to block(). This does bloat the amount of code
            // being inlined in this function, but its worth doing to minimize the additional
            // lambda allocation because of the use of withContext(...).
            return if (result.coroutinePropagationToken.requiresInstall()) {
                withContext(
                    context = currentCoroutineContext() + result.coroutinePropagationToken
                ) {
                    block()
                }
            } else {
                block()
            }
        } finally {
            // Only have the tokenContextElement be relevant for the execution of the suspending
            // `block` and not in this finally block.
            result.closeable.close()
        }
    }
}
