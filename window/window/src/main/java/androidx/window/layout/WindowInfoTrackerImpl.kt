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
import androidx.window.core.ExperimentalWindowApi
import androidx.window.layout.adapter.WindowBackend
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * An implementation of [WindowInfoTracker] that provides the [WindowLayoutInfo] and
 * [WindowMetrics] for the given [Activity] or [UiContext].
 *
 * @param windowMetricsCalculator a helper to calculate the [WindowMetrics] for the [Activity].
 * @param windowBackend a helper to provide the [WindowLayoutInfo].
 */
internal class WindowInfoTrackerImpl(
    private val windowMetricsCalculator: WindowMetricsCalculator,
    private val windowBackend: WindowBackend
) : WindowInfoTracker {

    /**
     * A [Flow] of window layout changes in the current visual [UiContext]. A context has to be
     * either an [Activity] or created with [Context#createWindowContext].
     */
    @ExperimentalWindowApi
    override fun windowLayoutInfo(@UiContext context: Context): Flow<WindowLayoutInfo> {
        return callbackFlow {
            val listener = Consumer { info: WindowLayoutInfo -> trySend(info) }
            windowBackend.registerLayoutChangeCallback(context, Runnable::run, listener)
            awaitClose {
                windowBackend.unregisterLayoutChangeCallback(listener)
            }
        }
    }

    /**
     * A [Flow] of window layout changes in the current visual [Activity].
     */
    override fun windowLayoutInfo(activity: Activity): Flow<WindowLayoutInfo> {
        return callbackFlow {
            val listener = Consumer { info: WindowLayoutInfo -> trySend(info) }
            windowBackend.registerLayoutChangeCallback(activity, Runnable::run, listener)
            awaitClose {
                windowBackend.unregisterLayoutChangeCallback(listener)
            }
        }
    }
}
