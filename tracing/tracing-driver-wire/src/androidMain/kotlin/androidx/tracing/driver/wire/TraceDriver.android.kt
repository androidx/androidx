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

package androidx.tracing.driver.wire

import android.content.Context
import android.os.Build
import android.os.Process
import androidx.tracing.driver.TraceDriver

/**
 * Constructs a [TraceDriver] instance on Android based on the provided [Context] instance.
 *
 * @param context The Android app [Context].
 * @param sink The [TraceSink] instance.
 * @param isEnabled Set this to `true` to emit trace events. `false` disables all tracing to lower
 *   overhead.
 */
@JvmOverloads
public fun TraceDriver(context: Context, sink: TraceSink, isEnabled: Boolean = true): TraceDriver {
    val driver = TraceDriver(sink = sink, isEnabled = isEnabled)
    val pid = Process.myPid()
    val processName =
        if (Build.VERSION.SDK_INT >= 33) {
            Process.myProcessName()
        } else {
            context.packageName
        }

    // Eagerly populate a process track
    val process = driver.context.getOrCreateProcessTrack(id = pid, name = processName)
    process.getOrCreateThreadTrack(id = pid, name = processName) // Main thread
    // Thread Tracks
    val thread = Thread.currentThread()
    val tid = thread.id.toInt()
    if (tid != pid) {
        val thread = Thread.currentThread()
        process.getOrCreateThreadTrack(id = tid, name = thread.name)
    }
    return driver
}
