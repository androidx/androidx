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

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Process
import androidx.annotation.RestrictTo
import androidx.tracing.AbstractTraceDriver
import androidx.tracing.AbstractTraceSink
import androidx.tracing.EmptyTraceContext
import androidx.tracing.EmptyTraceSink
import androidx.tracing.PerfettoTracer
import androidx.tracing.Trace
import androidx.tracing.TraceAttributes
import androidx.tracing.TraceContext
import androidx.tracing.Tracer

/**
 * Constructs a [TraceDriver] instance on Android.
 *
 * @param contextProvider The Android application [Context] provider.
 * @param sink The [TraceSink] instance.
 * @param isGloballyEnabled Set this to `true` to conditionally emit trace events based on
 *   [isCategoryEnabled]. `false` disables all tracing to lower overhead.
 * @param isCategoryEnabled returns `true` if the provided trace [category] should be enabled. If
 *   `false` then trace events corresponding to the [category] are dropped to reduce tracing
 *   overhead. This is particularly useful when you want to lower the overhead of trace events from
 *   uninteresting or noisy categories.
 * @param attributes Collection of key value pairs to be attached to a trace to provide additional
 *   context about any facet of the trace. This can include what data it contains, and properties of
 *   the host / machine the trace was collected on, and other interesting information about a trace.
 *   At the end of the [attributes] block, these key value pairs are dispatched to the designated
 *   [AbstractTraceSink] to be serialized.
 *
 * Examples include:
 * ```
 * gradle_version = "9.0.10-alpha01"
 * java_major_version = 24
 * ```
 */
public actual class TraceDriver
internal constructor(
    contextProvider: () -> Context,
    sink: AbstractTraceSink,
    isGloballyEnabled: Boolean = true,
    @JvmField internal val isCategoryEnabled: (category: String) -> Boolean = { Trace.isEnabled() },
    attributes: (TraceAttributes.() -> Unit)? = null,
) : AbstractTraceDriver(sink = sink) {

    /**
     * Constructs a [TraceDriver] instance on Android based on the provided [Context] instance.
     *
     * @param context The Android application [Context].
     * @param sink The [TraceSink] instance.
     * @param isCategoryEnabled returns `true` if the provided trace [category] should be enabled.
     *   If `false` then trace events corresponding to the [category] are dropped to reduce tracing
     *   overhead. This is particularly useful when you want to lower the overhead of trace events
     *   from uninteresting or noisy categories. The default implementation of this check allows all
     *   trace categories as long as a Perfetto tracing session is active ([Trace.isEnabled]).
     *
     *   Note:This method should be **extremely** low overhead given it's called every time a
     *   [Tracer] can emit trace events.
     *
     * @param attributes Collection of key value pairs to be attached to a trace to provide
     *   additional context about any facet of the trace. This can include what data it contains,
     *   and properties of the host / machine the trace was collected on, and other interesting
     *   information about a trace. At the end of the [attributes] block, these key value pairs are
     *   dispatched to the designated [AbstractTraceSink] to be serialized.
     *
     * Examples include:
     * ```
     * gradle_version = "9.0.10-alpha01"
     * java_major_version = 24
     * ```
     */
    @JvmOverloads
    public constructor(
        context: Context,
        sink: AbstractTraceSink,
        isCategoryEnabled: (category: String) -> Boolean = { Trace.isEnabled() },
        attributes: (TraceAttributes.() -> Unit)? = null,
    ) : this(
        contextProvider = { context },
        sink = sink,
        isGloballyEnabled = true,
        isCategoryEnabled = isCategoryEnabled,
        attributes = attributes,
    )

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val context: TraceContext =
        if (isGloballyEnabled) {
            TraceContext(
                sink = sink,
                isGloballyEnabled = true,
                isCategoryEnabled = isCategoryEnabled,
            )
        } else {
            EmptyTraceContext
        }

    init {
        // Only bootstrap tracks if globally enabled.
        if (isGloballyEnabled) {
            val pid = Process.myPid()
            // This is only used to eagerly create the ThreadTrack for the main thread.
            // On Android, pid == tid for main thread.
            val longPid = pid.toLong()
            val processName = getProcessName(context = contextProvider().applicationContext)
            // Eagerly populate a process track
            this.context.createProcessTrack(id = pid, name = processName)
            // Eager populate the main thread track
            // For the main thread on Android pid = tid
            // Main thread
            val mainTrack =
                this.context.process.getOrCreateThreadTrack(id = longPid, name = processName)
            // Thread Tracks
            // There are multiple ways of obtaining tids.
            // You can use android.Os.gettid(). This makes a JNI call under the hood (libcore)
            // [SLOW].
            // This method returns an `Int`.
            // The fastest way of getting a `tid` is by relying on `Thread.currentThread().id`.
            val thread = Thread.currentThread()
            val tid = thread.id
            // Populate additional thread tracks if necessary.
            if (tid != longPid) {
                this.context.process.getOrCreateThreadTrack(id = tid, name = thread.name)
            }
            // Trace attributes
            if (attributes != null) {
                val attributes = mainTrack.traceAttributes()
                attributes.attributes()
                attributes.dispatchToTraceSink()
            }
        }
    }

    override val tracer: Tracer by
        lazy(mode = LazyThreadSafetyMode.PUBLICATION) {
            PerfettoTracer(context = this.context, categoryEnabled = isCategoryEnabled)
        }

    override fun flush() {
        this.context.flush()
    }

    override fun close() {
        this.context.close()
    }

    public actual companion object {
        @JvmStatic
        public actual fun stubTraceDriver(): TraceDriver {
            return TraceDriver(
                contextProvider = { throw IllegalStateException("Should never happen") },
                sink = EmptyTraceSink,
                isGloballyEnabled = false,
                isCategoryEnabled = { false },
                attributes = null,
            )
        }
    }
}

internal fun getProcessName(context: Context): String {
    if (Build.VERSION.SDK_INT >= 28) return Application.getProcessName()
    @Suppress("PrivateApi")
    try {
        // Obtain the name of the current process from the ActivityThread.
        val activityThread =
            Class.forName(
                /* name = */ "android.app.ActivityThread",
                /* initialize = */ false,
                /* loader = */ AbstractTraceDriver::class.java.classLoader,
            )
        val currentProcessName = activityThread.getDeclaredMethod(/* name= */ "currentProcessName")
        currentProcessName.isAccessible = true
        val processName = currentProcessName.invoke(null) as? String
        if (processName != null) return processName
    } catch (_: Throwable) {
        // Do nothing
    }
    // Slow path
    val pid = Process.myPid()
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val processName = am.runningAppProcesses?.first { process -> process.pid == pid }?.processName
    return processName ?: "${context.packageName}($pid)"
}
