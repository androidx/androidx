/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.window.layout

import android.app.Activity
import android.content.Context
import androidx.annotation.UiContext
import androidx.core.util.Consumer
import androidx.window.WindowSdkExtensions
import androidx.window.layout.adapter.WindowBackend
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/**
 * An implementation of [WindowInfoTracker] that provides the [WindowLayoutInfo] and [WindowMetrics]
 * for the given [Activity] or [UiContext].
 *
 * @param windowMetricsCalculator a helper to calculate the [WindowMetrics] for the [Activity].
 * @param windowBackend a helper to provide the [WindowLayoutInfo].
 */
internal class WindowInfoTrackerImpl(
    private val windowMetricsCalculator: WindowMetricsCalculator,
    private val windowBackend: WindowBackend,
    private val windowSdkExtensions: WindowSdkExtensions,
) : WindowInfoTracker {

    private val lock = ReentrantLock()
    private val listenerMapping =
        mutableMapOf<Consumer<WindowEngagementInfo>, Consumer<WindowLayoutInfo>>()

    /**
     * A [Flow] of window layout changes in the current visual [UiContext]. A context has to be
     * either an [Activity] or created with [Context.createWindowContext].
     */
    override fun windowLayoutInfo(@UiContext context: Context): Flow<WindowLayoutInfo> {
        return callbackFlow {
                val listener = Consumer { info: WindowLayoutInfo -> trySend(info) }
                windowBackend.registerLayoutChangeCallback(context, Runnable::run, listener)
                awaitClose { windowBackend.unregisterLayoutChangeCallback(listener) }
            }
            .flowOn(Dispatchers.Main)
    }

    /** A [Flow] of window layout changes in the current visual [Activity]. */
    override fun windowLayoutInfo(activity: Activity): Flow<WindowLayoutInfo> {
        return windowLayoutInfo(activity as Context)
    }

    override fun windowEngagementInfo(@UiContext context: Context): Flow<WindowEngagementInfo> {
        return callbackFlow {
                val listener = Consumer { info: WindowEngagementInfo -> trySend(info) }
                registerWindowEngagementInfoListener(context, Runnable::run, listener)
                awaitClose { unregisterWindowEngagementInfoListener(listener) }
            }
            .flowOn(Dispatchers.Main)
    }

    override val supportedPostures: List<SupportedPosture>
        get() {
            windowSdkExtensions.requireExtensionVersion(6)
            return windowBackend.supportedPostures
        }

    override fun getCurrentWindowLayoutInfo(@UiContext context: Context): WindowLayoutInfo {
        windowSdkExtensions.requireExtensionVersion(9)
        return windowBackend.getCurrentWindowLayoutInfo(context)
    }

    override fun registerWindowLayoutInfoListener(
        @UiContext context: Context,
        executor: Executor,
        listener: Consumer<WindowLayoutInfo>,
    ) {
        windowBackend.registerLayoutChangeCallback(context, executor, listener)
    }

    override fun unregisterWindowLayoutInfoListener(listener: Consumer<WindowLayoutInfo>) {
        windowBackend.unregisterLayoutChangeCallback(listener)
    }

    @Suppress("DEPRECATION")
    override fun registerWindowEngagementInfoListener(
        @UiContext context: Context,
        executor: Executor,
        listener: Consumer<WindowEngagementInfo>,
    ) {
        lock.withLock {
            if (listenerMapping.containsKey(listener)) {
                return
            }
            val backendListener = EngagementInfoTranslator(executor, listener)
            listenerMapping[listener] = backendListener
            windowBackend.registerLayoutChangeCallback(context, Runnable::run, backendListener)
        }
    }

    override fun unregisterWindowEngagementInfoListener(listener: Consumer<WindowEngagementInfo>) {
        lock.withLock {
            val backendListener = listenerMapping.remove(listener) ?: return
            windowBackend.unregisterLayoutChangeCallback(backendListener)
        }
    }
}

@Suppress("DEPRECATION")
private class EngagementInfoTranslator(
    private val executor: Executor,
    private val listener: Consumer<WindowEngagementInfo>,
) : Consumer<WindowLayoutInfo> {
    private var lastInfo: WindowEngagementInfo? = null
    private val lock = ReentrantLock()

    override fun accept(value: WindowLayoutInfo) {
        val oemModes = value.engagementModes
        val newModes = mutableSetOf<WindowEngagementInfo.EngagementMode>()
        if (oemModes.contains(WindowLayoutInfo.EngagementMode.VISUALS_ON)) {
            newModes.add(WindowEngagementInfo.EngagementMode.VISUALS_ON)
        }
        if (oemModes.contains(WindowLayoutInfo.EngagementMode.AUDIO_ON)) {
            newModes.add(WindowEngagementInfo.EngagementMode.AUDIO_ON)
        }

        val engagementInfo = WindowEngagementInfo(newModes)
        val shouldSend =
            lock.withLock {
                if (engagementInfo != lastInfo) {
                    lastInfo = engagementInfo
                    true
                } else {
                    false
                }
            }

        if (shouldSend) {
            executor.execute { listener.accept(engagementInfo) }
        }
    }
}
