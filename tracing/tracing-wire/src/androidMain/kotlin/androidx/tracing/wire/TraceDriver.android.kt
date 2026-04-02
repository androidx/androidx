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
import androidx.tracing.TraceAttributes
import androidx.tracing.TraceContext
import androidx.tracing.Tracer

/**
 * Constructs a [TraceDriver] instance on Android based on the provided [Context] instance.
 *
 * @param context The Android application [Context].
 * @param sink The [TraceSink] instance.
 * @param isEnabled Set this to `true` to emit trace events. `false` disables all tracing to lower
 *   overhead.
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
@JvmOverloads
constructor(
    context: Context,
    sink: AbstractTraceSink,
    isEnabled: Boolean = true,
    attributes: (TraceAttributes.() -> Unit)? = null,
) : AbstractTraceDriver(sink = sink, isEnabled = isEnabled) {

    private val applicationContext = context.applicationContext

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val context: TraceContext =
        if (isEnabled) {
            TraceContext(sink = sink, isEnabled = isEnabled)
        } else {
            EmptyTraceContext
        }

    init {
        val pid = Process.myPid()
        val processName = getProcessName(context = applicationContext)
        // Eagerly populate a process track
        this.context.createProcessTrack(id = pid, name = processName)
        // Eager populate the main thread track
        // For the main thread on Android pid = tid
        // Main thread
        val mainTrack = this.context.process.getOrCreateThreadTrack(id = pid, name = processName)
        // Thread Tracks
        // There are multiple ways of obtaining tids.
        // You can use android.Os.gettid(). This makes a JNI call under the hood (libcore) [SLOW].
        // This method returns an `Int`.
        // The fastest way of getting a `tid` is by relying on `Thread.currentThread().id`. Even
        // though this method returns a `Long` type, given the underlying tid is an `Int` as defined
        // in libcore - this downcast is safe.
        val thread = Thread.currentThread()
        val tid = thread.id.toInt()
        // Populate additional thread tracks if necessary.
        if (tid != pid) {
            val thread = Thread.currentThread()
            this.context.process.getOrCreateThreadTrack(id = tid, name = thread.name)
        }
        // Trace attributes
        if (attributes != null) {
            val attributes = mainTrack.traceAttributes()
            attributes.attributes()
            attributes.dispatchToTraceSink()
        }
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
