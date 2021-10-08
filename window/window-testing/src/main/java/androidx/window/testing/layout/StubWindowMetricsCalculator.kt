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
import android.graphics.Rect
import androidx.window.layout.WindowMetrics
import androidx.window.layout.WindowMetricsCalculator

/**
 * A stub implementation of [WindowMetricsCalculator] that returns the
 * [android.util.DisplayMetrics] for the current and maximum [WindowMetrics]. This is not correct
 * in general terms, as an application may be running in multi-window or otherwise adjusted to not
 * occupy the entire display.
 */
internal object StubWindowMetricsCalculator : WindowMetricsCalculator {

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
}