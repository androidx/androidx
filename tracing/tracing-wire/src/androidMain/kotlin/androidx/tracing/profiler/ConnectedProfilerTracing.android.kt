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

package androidx.tracing.profiler

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED

/** Manages enabling and disabling of in-process tracing for profiling and benchmarking tools. */
internal object ConnectedProfilerTracing {
    /** This is the global flag that controls if in-process tracing is enabled. */
    private var isTracingEnabled: Boolean = false

    internal fun initialize(context: Context) {
        isTracingEnabled = isReceiverEnabled(context)
    }

    @Suppress("NOTHING_TO_INLINE")
    internal inline fun readAfterInitialize(): Boolean {
        return isTracingEnabled
    }

    // No locks necessary for enable / disable. These methods are always called from the main
    // thread.

    internal fun enableTracing(context: Context) {
        isTracingEnabled = true
        setTracingState(context = context, state = COMPONENT_ENABLED_STATE_ENABLED)
    }

    internal fun disableTracing(context: Context) {
        isTracingEnabled = false
        setTracingState(context = context, state = COMPONENT_ENABLED_STATE_DISABLED)
    }

    // Internal utilities
    private fun setTracingState(context: Context, state: Int) {
        val applicationContext = context.applicationContext
        val component = enableTracingComponent(applicationContext)
        applicationContext.packageManager.setComponentEnabledSetting(
            component,
            state,
            PackageManager.DONT_KILL_APP,
        )
    }

    private fun isReceiverEnabled(context: Context): Boolean {
        val applicationContext = context.applicationContext
        val component = enableTracingComponent(context)
        val setting = applicationContext.packageManager.getComponentEnabledSetting(component)
        return setting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun enableTracingComponent(context: Context): ComponentName {
        return ComponentName(context, ConnectedProfilerTracingEnabledReceiver::class.java)
    }
}
