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
import androidx.annotation.VisibleForTesting
import androidx.collection.mutableScatterSetOf

/** Manages enabling and disabling of in-process tracing for profiling and benchmarking tools. */
internal object ConnectedProfilerTracing {

    /** This is the global flag that controls if in-process tracing is enabled. */
    // We are updating isTracingEnabled from multiple threads. So this needs to be marked volatile.
    @Volatile private var isTracingEnabled: Boolean = false

    /** Set to `true` iff explicit categories have been enabled. */
    @Volatile private var hasExplicitCategories: Boolean = false

    /** The global set of enabled categories. */
    @Volatile private var enabledCategories = mutableScatterSetOf<String>()

    private val lock = Any()

    internal fun initialize(context: Context) {
        synchronized(lock) {
            isTracingEnabled = isReceiverEnabled(context)
            val config = context.traceConfiguration()
            if (isTracingEnabled && config.exists() && config.canRead()) {
                val categories = context.traceConfiguration().loadConfig()
                enabledCategories = categories
            }
            hasExplicitCategories = enabledCategories.isNotEmpty()
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    internal inline fun readAfterInitialize(): Boolean {
        return isTracingEnabled
    }

    /**
     * Returns `true` if the specified [category] is enabled. This method is lock-free and extremely
     * low overhead. When all categories are enabled, it does not read the [enabledCategories]
     * volatile.
     */
    @Suppress("NOTHING_TO_INLINE")
    internal inline fun isCategoryEnabled(category: String): Boolean {
        return if (!hasExplicitCategories) {
            isTracingEnabled
        } else {
            isTracingEnabled && enabledCategories.contains(category)
        }
    }

    internal fun enableTracing(context: Context, categories: Array<String>? = null) {
        synchronized(lock) {
            if (categories.isNullOrEmpty()) {
                hasExplicitCategories = false
                context.traceConfiguration().delete()
            } else {
                hasExplicitCategories = true
                val enabledCategories = mutableScatterSetOf<String>()
                categories.forEach { enabledCategories += it }
                this.enabledCategories = enabledCategories
                context.traceConfiguration().saveConfig(categories)
            }
            isTracingEnabled = true
            setTracingState(context = context, state = COMPONENT_ENABLED_STATE_ENABLED)
        }
    }

    internal fun disableTracing(context: Context) {
        synchronized(lock) {
            isTracingEnabled = false
            hasExplicitCategories = false
            enabledCategories = mutableScatterSetOf()
            context.traceConfiguration().delete()
            setTracingState(context = context, state = COMPONENT_ENABLED_STATE_DISABLED)
        }
    }

    // Internal utilities
    @VisibleForTesting
    internal fun setTracingState(context: Context, state: Int) {
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
