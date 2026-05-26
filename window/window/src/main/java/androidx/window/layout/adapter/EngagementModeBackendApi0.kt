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

package androidx.window.layout.adapter

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.GuardedBy
import androidx.annotation.UiContext
import androidx.core.util.Consumer
import androidx.window.layout.WindowEngagementInfo.EngagementMode
import androidx.window.layout.util.DeduplicateConsumer
import androidx.window.layout.util.EngagementModeHelper
import androidx.window.layout.util.RunOnExecutorConsumer
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class EngagementModeBackendApi0
internal constructor(
    private val engagementModeHelper: EngagementModeHelper,
    internal val inputDeviceTracker: InputDeviceTracker,
) : EngagementModeBackend {

    private val lock = ReentrantLock()
    /** Map of all registered callbacks for engagement layout mode. */
    @GuardedBy("lock")
    private val engagementChangeCallbacks =
        mutableMapOf<Consumer<EngagementMode>, DeduplicateConsumer<EngagementMode>>()

    /** Map of callback to context. */
    @GuardedBy("lock")
    private val callbackToContext = mutableMapOf<Consumer<EngagementMode>, Context>()

    /** Map of context to [ConfigChangeCallback]. */
    @GuardedBy("lock")
    private val contextRegistrations = mutableMapOf<Context, ConfigChangeCallback>()

    private val inputDeviceListener = Consumer<Boolean> { updateAllCallbacks() }

    override fun engagementMode(@UiContext context: Context): EngagementMode {
        return calculateEngagementMode(context)
    }

    override fun addEngagementLayoutChangeCallback(
        @UiContext context: Context,
        executor: Executor,
        callback: Consumer<EngagementMode>,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            callback.accept(EngagementMode.TOUCH)
            return
        }
        var callbackWrapper: DeduplicateConsumer<EngagementMode>
        var isFirstCallback = false
        lock.withLock {
            if (engagementChangeCallbacks.containsKey(callback)) {
                // Return early if the same callback is already registered.
                return
            }
            isFirstCallback = engagementChangeCallbacks.isEmpty()
            callbackWrapper = DeduplicateConsumer(RunOnExecutorConsumer(executor, callback))
            engagementChangeCallbacks[callback] = callbackWrapper
            callbackToContext[callback] = context

            // Ensure we only register ComponentCallbacks once per context.
            contextRegistrations.getOrPut(context) {
                ConfigChangeCallback().also { context.registerComponentCallbacks(it) }
            }
        }
        if (isFirstCallback) {
            inputDeviceTracker.registerListener(executor, inputDeviceListener)
        }
        callbackWrapper.accept(calculateEngagementMode(context))
    }

    override fun removeEngagementLayoutChangeCallback(callback: Consumer<EngagementMode>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }
        lock.withLock {
            val context = callbackToContext.remove(callback)
            engagementChangeCallbacks.remove(callback)
            if (context != null) {
                // No more callbacks registered for the context, unregister ComponentCallbacks.
                if (!callbackToContext.values.contains(context)) {
                    contextRegistrations.remove(context)?.let { registration ->
                        context.unregisterComponentCallbacks(registration)
                    }
                }
            }

            if (engagementChangeCallbacks.isEmpty()) {
                inputDeviceTracker.unregisterListener(inputDeviceListener)
            }
        }
    }

    private inner class ConfigChangeCallback : ComponentCallbacks2 {

        // Capture when app is moved between different displays to re-evaluate display size.
        override fun onConfigurationChanged(newConfig: Configuration) {
            updateAllCallbacks()
        }

        override fun onLowMemory() {}

        override fun onTrimMemory(level: Int) {}
    }

    private fun updateAllCallbacks() {
        val callbacksToNotify =
            mutableListOf<Pair<DeduplicateConsumer<EngagementMode>, EngagementMode>>()
        lock.withLock {
            engagementChangeCallbacks.forEach { (callback, wrapper) ->
                val context = callbackToContext[callback]!!
                callbacksToNotify.add(wrapper to calculateEngagementMode(context))
            }
        }
        callbacksToNotify.forEach { (wrapper, mode) -> wrapper.accept(mode) }
    }

    private fun calculateEngagementMode(context: Context): EngagementMode {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return EngagementMode.TOUCH
        }
        val isConnected = inputDeviceTracker.isMouseAndKeyboardConnected()
        val isLargeEnough = engagementModeHelper.hasLargeEnoughDisplay(context)
        return if (isConnected && isLargeEnough) {
            EngagementMode.PRECISE_POINTER
        } else {
            EngagementMode.TOUCH
        }
    }
}
