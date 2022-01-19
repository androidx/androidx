/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.tracing.perfetto

import androidx.tracing.perfetto.jni.PerfettoNative
import java.util.concurrent.atomic.AtomicBoolean

object Tracing {
    private val isEnabled = AtomicBoolean()

    @Volatile
    private var _isTraceInProgress: Boolean = false

    val isTraceInProgress: Boolean
        get() = _isTraceInProgress

    // TODO: replace with a Broadcast
    fun enable() {
        if (!isEnabled.getAndSet(true)) {
            PerfettoNative.loadLib()
            PerfettoNative.nativeRegisterWithPerfetto()
        }
    }

    // TODO: remove and replace with an observer wired into Perfetto
    fun setTraceInProgress(newState: Boolean) {
        val oldState = _isTraceInProgress
        _isTraceInProgress = newState
        if (newState != oldState && !newState) PerfettoNative.nativeFlushEvents()
    }

    fun traceEventStart(key: Int, traceInfo: String) =
        PerfettoNative.nativeTraceEventBegin(key, traceInfo)

    fun traceEventEnd() = PerfettoNative.nativeTraceEventEnd()
}
