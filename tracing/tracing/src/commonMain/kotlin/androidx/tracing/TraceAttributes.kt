/*
 * Copyright 2026 The Android Open Source Project
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
 * Makes it possible to associate key value pairs to be attached to a trace to provide additional
 * context about any facet of the trace. This can include what data it contains, and properties of
 * the host / machine the trace was collected on, and other interesting information about a trace.
 *
 * Examples include:
 * ```
 * gradle_version = "9.0.10-alpha01"
 * java_major_version = 24
 * ```
 *
 * All keys get prefixed with `trace_attribute.` in Perfetto UI and in `traceprocessor`. If there
 * are multiple trace attributes that use the same key then the last value wins.
 *
 * These key value pairs show up in the `Overview` section in Perfetto UI. Additionally, these key,
 * value pairs can be queried using Perfetto SQL using the following query:
 * ```sql
 * SELECT * FROM metadata WHERE name GLOB 'trace_attribute.*';
 * ```
 */
public abstract class TraceAttributes internal constructor() {
    /** Adds a trace attribute entry where the type of the [value] is a [Long]. */
    public abstract fun addAttribute(name: String, value: Long)

    /** Adds a trace attribute entry where the type of the [value] is a [String]. */
    public abstract fun addAttribute(name: String, value: String)

    /** Dispatches the underlying [TraceEvent] to the [AbstractTraceSink] instance if applicable. */
    @DelicateTracingApi public abstract fun dispatchToTraceSink()
}
