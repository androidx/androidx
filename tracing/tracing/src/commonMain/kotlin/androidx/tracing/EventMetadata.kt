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

package androidx.tracing

/**
 * Makes it possible to associate debug metadata & categories to a [TraceEvent]. Typically obtained
 * by a call to [Tracer.beginSectionWithMetadata].
 */
public abstract class EventMetadata internal constructor() {

    /** Adds a metadata entry where the type of the [value] is an [Boolean]. */
    public abstract fun addMetadataEntry(name: String, value: Boolean)

    /** Adds a metadata entry where the type of the [value] is an [Long]. */
    public abstract fun addMetadataEntry(name: String, value: Long)

    /** Adds a metadata entry where the type of the [value] is an [Double]. */
    public abstract fun addMetadataEntry(name: String, value: Double)

    /** Adds a metadata entry where the type of the [value] is an [String]. */
    public abstract fun addMetadataEntry(name: String, value: String)

    /** Adds call stack frame information to the [TraceEvent]. */
    public abstract fun addCallStackEntry(name: String, sourceFile: String?, lineNumber: Int)

    /** Add a [Long] correlation id to the trace packet. */
    public abstract fun addCorrelationId(id: Long)

    /**
     * Adds a [String] correlation id to the trace packet.
     *
     * Consider using the `addCorrelationId(Long)` variant for performance reasons when possible.
     */
    public abstract fun addCorrelationId(id: String)

    /**
     * Adds additional categories to the [TraceEvent].
     *
     * This is useful when an application is interested in a subset of [TraceEvent]s that belong to
     * well known categories. These are typically small identifiers useful for namespacing
     * [TraceEvent]s.
     */
    public abstract fun addCategory(name: String)

    /** Dispatches the underlying [TraceEvent] to the [AbstractTraceSink] instance if applicable. */
    @DelicateTracingApi public abstract fun dispatchToTraceSink()
}
