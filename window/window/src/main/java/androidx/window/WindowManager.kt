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
import android.content.ContextWrapper
import androidx.core.util.Consumer
import java.util.concurrent.Executor

/**
 * Main interaction point with the WindowManager library. An instance of this class allows
 * polling the current state of the device and display, and registering callbacks for changes in
 * the corresponding states.
 *
 * All methods of this class will return information that is associated with this visual
 * context.
 *
 */
public class WindowManager @JvmOverloads constructor(
    /**
     * A visual [Context], such as an [Activity] or a [ContextWrapper]
     */
    context: Context,
    /**
     * Backing server class that will provide information for this instance.
     *
     * Pass a custom [WindowBackend] implementation for testing.
     */
    private val windowBackend: WindowBackend = ExtensionWindowBackend.getInstance(
        context
    )
) {
    /**
     * A class to calculate the bounds of a [android.view.Window] across different versions of
     * Android.
     */
    internal var windowMetricsCalculator: WindowMetricsCalculator = WindowMetricsCalculatorCompat

    /**
     * Activity that was registered with this instance of [WindowManager] at creation.
     * This is used to find the token identifier of the window when requesting layout information
     * from the [androidx.window.sidecar.SidecarInterface] or is passed directly to the
     * [androidx.window.extensions.ExtensionInterface].
     */
    private val activity: Activity = getActivityFromContext(context)
        ?: throw IllegalArgumentException(
            "Used non-visual Context to obtain an instance of WindowManager. Please use an " +
                "Activity or a ContextWrapper around one instead."
        )

    /**
     * Registers a callback for layout changes of the window of the current visual [Context].
     * Must be called only after the it is attached to the window.
     *
     * @see Activity.onAttachedToWindow
     */
    public fun registerLayoutChangeCallback(
        executor: Executor,
        callback: Consumer<WindowLayoutInfo>
    ) {
        windowBackend.registerLayoutChangeCallback(activity, executor, callback)
    }

    /**
     * Unregisters a callback for window layout changes of the window.
     */
    public fun unregisterLayoutChangeCallback(callback: Consumer<WindowLayoutInfo>) {
        windowBackend.unregisterLayoutChangeCallback(callback)
    }

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
     * @see getMaximumWindowMetrics
     * @see android.view.WindowManager.getCurrentWindowMetrics
     */
    public fun getCurrentWindowMetrics(): WindowMetrics {
        return windowMetricsCalculator.computeCurrentWindowMetrics(activity)
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
     * @see getCurrentWindowMetrics
     * @see android.view.WindowManager.getMaximumWindowMetrics
     */
    public fun getMaximumWindowMetrics(): WindowMetrics {
        return windowMetricsCalculator.computeMaximumWindowMetrics(activity)
    }

    internal companion object {
        /**
         * Unwraps the hierarchy of [ContextWrapper]-s until [Activity] is reached.
         *
         * @return Base [Activity] context or `null` if not available.
         */
        fun getActivityFromContext(initialContext: Context): Activity? {
            var context: Context? = initialContext
            while (context is ContextWrapper) {
                if (context is Activity) {
                    return context
                }
                context = context.baseContext
            }
            return null
        }
    }
}
