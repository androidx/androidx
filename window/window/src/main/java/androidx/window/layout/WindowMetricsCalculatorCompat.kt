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
import android.inputmethodservice.InputMethodService
import androidx.annotation.UiContext
import androidx.core.view.WindowInsetsCompat
import androidx.window.layout.util.DensityCompatHelper
import androidx.window.layout.util.WindowMetricsCompatHelper

/** Helper class used to compute window metrics across Android versions. */
internal class WindowMetricsCalculatorCompat(
    private val densityCompatHelper: DensityCompatHelper = DensityCompatHelper.getInstance()
) : WindowMetricsCalculator {

    /**
     * Computes the current [WindowMetrics] for a given [Context]. The context can be either an
     * [Activity], a Context created with [Context#createWindowContext], or an [InputMethodService].
     *
     * @see WindowMetricsCalculator.computeCurrentWindowMetrics
     */
    override fun computeCurrentWindowMetrics(@UiContext context: Context): WindowMetrics {
        return WindowMetricsCompatHelper.getInstance()
            .currentWindowMetrics(context, densityCompatHelper)
    }

    /**
     * Computes the current [WindowMetrics] for a given [Activity]
     *
     * @see WindowMetricsCalculator.computeCurrentWindowMetrics
     */
    override fun computeCurrentWindowMetrics(activity: Activity): WindowMetrics {
        return WindowMetricsCompatHelper.getInstance()
            .currentWindowMetrics(activity, densityCompatHelper)
    }

    /**
     * Computes the maximum [WindowMetrics] for a given [Activity]
     *
     * @see WindowMetricsCalculator.computeMaximumWindowMetrics
     */
    override fun computeMaximumWindowMetrics(activity: Activity): WindowMetrics {
        return WindowMetricsCompatHelper.getInstance()
            .maximumWindowMetrics(activity, densityCompatHelper)
    }

    /**
     * Computes the maximum [WindowMetrics] for a given [UiContext]
     *
     * @See WindowMetricsCalculator.computeMaximumWindowMetrics
     */
    override fun computeMaximumWindowMetrics(@UiContext context: Context): WindowMetrics {
        return WindowMetricsCompatHelper.getInstance()
            .maximumWindowMetrics(context, densityCompatHelper)
    }

    /** [ArrayList] that defines different types of sources causing window insets. */
    internal val insetsTypeMasks: ArrayList<Int> =
        arrayListOf(
            WindowInsetsCompat.Type.statusBars(),
            WindowInsetsCompat.Type.navigationBars(),
            WindowInsetsCompat.Type.captionBar(),
            WindowInsetsCompat.Type.ime(),
            WindowInsetsCompat.Type.systemGestures(),
            WindowInsetsCompat.Type.mandatorySystemGestures(),
            WindowInsetsCompat.Type.tappableElement(),
            WindowInsetsCompat.Type.displayCutout()
        )
}
