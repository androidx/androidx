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

package androidx.window.testing.layout

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.view.Display
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.annotation.UiContext
import androidx.window.layout.WindowMetrics
import androidx.window.layout.WindowMetricsCalculator

/**
 * A stub implementation of [WindowMetricsCalculator] that's intended to be used by Robolectric.
 * [computeCurrentWindowMetrics] and [computeMaximumWindowMetrics] returns reasonable
 * [WindowMetrics] for all supported SDK levels, but is not correct in general terms, as an
 * application or [UiContext] may be running in multi-window mode, or otherwise adjusted to not
 * occupy the entire display.
 */
internal class StubWindowMetricsCalculator : WindowMetricsCalculator {

    override fun computeCurrentWindowMetrics(activity: Activity): WindowMetrics {
        val displayMetrics = activity.resources.displayMetrics
        val bounds = Rect(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)
        return WindowMetrics(bounds)
    }

    override fun computeMaximumWindowMetrics(activity: Activity): WindowMetrics {
        val displayMetrics = activity.resources.displayMetrics
        val bounds = Rect(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)
        return WindowMetrics(bounds)
    }

    // WindowManager#getDefaultDisplay is deprecated but we have this for compatibility with
    // older versions.
    @Suppress("DEPRECATION")
    override fun computeCurrentWindowMetrics(@UiContext context: Context): WindowMetrics {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Api30Impl.getWindowMetrics(wm)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val displaySize = Point()
            // We use getRealSize instead of getSize here because:
            //   1) computeCurrentWindowMetrics and computeMaximumWindowMetrics in this class
            //      always return a measurement equal to the entire display (see class-level
            //      documentation).
            //   2) getRealSize returns the largest region of the display, whereas getSize returns
            //      the current app window. So to stay consistent with class documentation, we use
            //      getRealSize.
            Api17Impl.getRealSize(wm.defaultDisplay, displaySize)
            val bounds = Rect(0, 0, displaySize.x, displaySize.y)
            WindowMetrics(bounds)
        } else {
            val width = wm.defaultDisplay.width
            val height = wm.defaultDisplay.height
            val bounds = Rect(0, 0, width, height)
            WindowMetrics(bounds)
        }
    }

    override fun computeMaximumWindowMetrics(@UiContext context: Context): WindowMetrics {
        return computeCurrentWindowMetrics(context)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private object Api30Impl {
        fun getWindowMetrics(windowManager: WindowManager): WindowMetrics {
            return WindowMetrics(windowManager.currentWindowMetrics.bounds)
        }
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private object Api17Impl {
        // getRealSize is deprecated but we have this for compatibility with older versions.
        @Suppress("DEPRECATION")
        fun getRealSize(display: Display, point: Point) {
            display.getRealSize(point)
        }
    }
}
