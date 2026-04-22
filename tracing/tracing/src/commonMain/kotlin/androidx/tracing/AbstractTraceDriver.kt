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

/** The entry point for the tracing API. */
public abstract class AbstractTraceDriver : AutoCloseable {
    /**
     * The [AbstractTraceSink] that asynchronously serializes trace events to a file or buffer,
     * depending on implementation.
     */
    public val sink: AbstractTraceSink

    /** Builds an instance of [AbstractTraceDriver] using the provided [AbstractTraceSink]. */
    protected constructor(sink: AbstractTraceSink) {
        this.sink = sink
    }

    /** Return an instance of a [Tracer] that can be used to emit trace events. */
    public abstract val tracer: Tracer

    /** Flushes the trace packets into the underlying [AbstractTraceSink]. */
    public abstract fun flush()

    /**
     * Flushes all outstanding packets to the [AbstractTraceSink] and then closes the
     * [AbstractTraceSink].
     */
    public abstract override fun close()
}
