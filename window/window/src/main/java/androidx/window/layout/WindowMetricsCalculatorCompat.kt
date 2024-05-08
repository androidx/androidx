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
import android.graphics.Rect
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Build.VERSION_CODES
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.annotation.UiContext
import androidx.core.view.WindowInsetsCompat
import androidx.window.core.Bounds
import androidx.window.layout.util.BoundsHelper
import androidx.window.layout.util.ContextCompatHelper.unwrapUiContext
import androidx.window.layout.util.ContextCompatHelperApi30.currentWindowInsets
import androidx.window.layout.util.DensityCompatHelper
import androidx.window.layout.util.DisplayHelper.getRealSizeForDisplay
import androidx.window.layout.util.WindowMetricsCompatHelper

/**
 * Helper class used to compute window metrics across Android versions.
 */
internal class WindowMetricsCalculatorCompat(
    private val densityCompatHelper: DensityCompatHelper = DensityCompatHelper.getInstance()
) : WindowMetricsCalculator {

    /**
     * Computes the current [WindowMetrics] for a given [Context]. The context can be either
     * an [Activity], a Context created with [Context#createWindowContext], or an
     * [InputMethodService].
     * @see WindowMetricsCalculator.computeCurrentWindowMetrics
     */
    override fun computeCurrentWindowMetrics(@UiContext context: Context): WindowMetrics {
        // TODO(b/259148796): Make WindowMetricsCalculatorCompat more testable
        if (Build.VERSION.SDK_INT >= VERSION_CODES.R) {
            return WindowMetricsCompatHelper.getInstance().currentWindowMetrics(context)
        } else {
            when (val unwrappedContext = unwrapUiContext(context)) {
                is Activity -> {
                    return computeCurrentWindowMetrics(unwrappedContext)
                }

                is InputMethodService -> {
                    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

                    // On older SDK levels, the app and IME could show up on different displays.
                    // However, there isn't a way for us to figure this out from the application
                    // layer. But, this should be good enough for now given the small likelihood of
                    // IMEs showing up on non-primary displays on these SDK levels.
                    @Suppress("DEPRECATION")
                    val displaySize = getRealSizeForDisplay(wm.defaultDisplay)

                    // IME occupies the whole display bounds.
                    val imeBounds = Rect(0, 0, displaySize.x, displaySize.y)
                    return WindowMetrics(
                        imeBounds,
                        density = densityCompatHelper.density(context)
                    )
                }

                else -> {
                    throw IllegalArgumentException("$context is not a UiContext")
                }
            }
        }
    }

    /**
     * Computes the current [WindowMetrics] for a given [Activity]
     * @see WindowMetricsCalculator.computeCurrentWindowMetrics
     */
    override fun computeCurrentWindowMetrics(activity: Activity): WindowMetrics {
        val bounds = BoundsHelper.getInstance().currentWindowBounds(activity)

        // TODO (b/233899790): compute insets for other platform versions below R
        val windowInsetsCompat = if (Build.VERSION.SDK_INT >= VERSION_CODES.R) {
            computeWindowInsetsCompat(activity)
        } else {
            WindowInsetsCompat.Builder().build()
        }
        return WindowMetrics(
            Bounds(bounds), windowInsetsCompat,
            densityCompatHelper.density(activity)
        )
    }

    /**
     * Computes the maximum [WindowMetrics] for a given [Activity]
     * @see WindowMetricsCalculator.computeMaximumWindowMetrics
     */
    override fun computeMaximumWindowMetrics(activity: Activity): WindowMetrics {
        return computeMaximumWindowMetrics(activity as Context)
    }

    /**
     * Computes the maximum [WindowMetrics] for a given [UiContext]
     * @See WindowMetricsCalculator.computeMaximumWindowMetrics
     */
    override fun computeMaximumWindowMetrics(@UiContext context: Context): WindowMetrics {
        val bounds = BoundsHelper.getInstance().maximumWindowBounds(context)

        // TODO (b/233899790): compute insets for other platform versions below R
        val windowInsetsCompat = if (Build.VERSION.SDK_INT >= VERSION_CODES.R) {
            computeWindowInsetsCompat(context)
        } else {
            WindowInsetsCompat.Builder().build()
        }
        return WindowMetrics(
            Bounds(bounds), windowInsetsCompat,
            densityCompatHelper.density(context)
        )
    }

    /**
     * [ArrayList] that defines different types of sources causing window insets.
     */
    internal val insetsTypeMasks: ArrayList<Int> = arrayListOf(
        WindowInsetsCompat.Type.statusBars(),
        WindowInsetsCompat.Type.navigationBars(),
        WindowInsetsCompat.Type.captionBar(),
        WindowInsetsCompat.Type.ime(),
        WindowInsetsCompat.Type.systemGestures(),
        WindowInsetsCompat.Type.mandatorySystemGestures(),
        WindowInsetsCompat.Type.tappableElement(),
        WindowInsetsCompat.Type.displayCutout()
    )

    /**
     * Computes the current [WindowInsetsCompat] for a given [Context].
     */
    @RequiresApi(VERSION_CODES.R)
    internal fun computeWindowInsetsCompat(@UiContext context: Context): WindowInsetsCompat {
        val build = Build.VERSION.SDK_INT
        val windowInsetsCompat = if (build >= VERSION_CODES.R) {
            currentWindowInsets(context)
        } else {
            throw Exception("Incompatible SDK version")
        }
        return windowInsetsCompat
    }
}
