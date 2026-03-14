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

@file:JvmName("TraceDriverUtils") // Java Users

package androidx.tracing.wire

import androidx.tracing.AbstractTraceDriver
import androidx.tracing.AbstractTraceSink
import androidx.tracing.TraceContext
import androidx.tracing.Tracer
import kotlin.jvm.optionals.getOrNull

/**
 * Constructs a [TraceDriver] instance on the JVM.
 *
 * @param sink The [TraceSink] instance.
 * @param isEnabled Set this to `true` to emit trace events. `false` disables all tracing to lower
 *   overhead.
 */
@Suppress("DEPRECATION")
public actual class TraceDriver
@JvmOverloads
constructor(sink: AbstractTraceSink, isEnabled: Boolean = true) :
    AbstractTraceDriver(sink = sink, isEnabled = isEnabled) {

    private val context = TraceContext(sink = sink, isEnabled = isEnabled)

    init {
        val processHandle = ProcessHandle.current()
        val pid = processHandle.pid()
        val name = processHandle.info().command().getOrNull() ?: "Process pid($pid)"
        // Eagerly populate a process track
        context.createProcessTrack(id = pid.toInt(), name = name)
        // Eagerly populate the current thread track
        val thread = Thread.currentThread()
        context.process.getOrCreateThreadTrack(id = thread.id.toInt(), name = thread.name)
    }

    override val tracer: Tracer by
        lazy(mode = LazyThreadSafetyMode.PUBLICATION) { this.context.createTracer() }

    override fun flush() {
        this.context.flush()
    }

    override fun close() {
        this.context.close()
    }
}
