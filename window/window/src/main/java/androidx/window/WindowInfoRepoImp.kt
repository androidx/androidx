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

package androidx.window

import android.app.Activity
import android.content.Context
import androidx.core.util.Consumer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

/**
 * An implementation of [WindowInfoRepo] that provides the [WindowLayoutInfo] and
 * [WindowMetrics] for the given [Activity].
 *
 * @param activity that the provided window is based on.
 * @param windowBoundsHelper a helper to calculate the [WindowMetrics] for the [Activity].
 * @param windowBackend a helper to provide the [WindowLayoutInfo].
 */
@ExperimentalCoroutinesApi
internal class WindowInfoRepoImp(
    private val activity: Activity,
    private val windowBoundsHelper: WindowBoundsHelper,
    private val windowBackend: WindowBackend
) : WindowInfoRepo {

    /**
     * Returns the [WindowMetrics] according to the current system state.
     *
     *
     * The metrics describe the size of the area the window would occupy with
     * [MATCH_PARENT][android.view.WindowManager.LayoutParams.MATCH_PARENT] width and height
     * and any combination of flags that would allow the window to extend behind display cutouts.
     *
     *
     * The value of this is based on the **current** windowing state of the system. For
     * example, for activities in multi-window mode, the metrics returned are based on the
     * current bounds that the user has selected for the [Activity][android.app.Activity]'s
     * window.
     *
     * @see maximumWindowMetrics
     * @see android.view.WindowManager.getCurrentWindowMetrics
     */
    override val currentWindowMetrics: WindowMetrics
        get() {
            return WindowMetrics(windowBoundsHelper.computeCurrentWindowBounds(activity))
        }

    /**
     * Returns the largest [WindowMetrics] an app may expect in the current system state.
     *
     *
     * The metrics describe the size of the largest potential area the window might occupy with
     * [MATCH_PARENT][android.view.WindowManager.LayoutParams.MATCH_PARENT] width and height
     * and any combination of flags that would allow the window to extend behind display cutouts.
     *
     *
     * The value of this is based on the largest **potential** windowing state of the system.
     * For example, for activities in multi-window mode the metrics returned are based on what the
     * bounds would be if the user expanded the window to cover the entire screen.
     *
     *
     * Note that this might still be smaller than the size of the physical display if certain
     * areas of the display are not available to windows created for the associated [Context].
     * For example, devices with foldable displays that wrap around the enclosure may split the
     * physical display into different regions, one for the front and one for the back, each acting
     * as different logical displays. In this case [.getMaximumWindowMetrics] would return
     * the region describing the side of the device the associated [context&#39;s][Context]
     * window is placed.
     *
     * @see currentWindowMetrics
     * @see android.view.WindowManager.getMaximumWindowMetrics
     */
    override val maximumWindowMetrics: WindowMetrics
        get() {
            return WindowMetrics(windowBoundsHelper.computeMaximumWindowBounds(activity))
        }

    /**
     * A [Flow] of window layout changes in the current visual [Context].
     *
     * @see Activity.onAttachedToWindow
     */
    override val windowLayoutInfo: Flow<WindowLayoutInfo>
        get() = callbackFlow {
            val callback = Consumer<WindowLayoutInfo> { info -> offer(info) }
            windowBackend.registerLayoutChangeCallback(activity, Runnable::run, callback)
            awaitClose { windowBackend.unregisterLayoutChangeCallback(callback) }
        }.buffer(capacity = UNLIMITED)
}
